package software.coley.instrument;

import software.coley.instrument.data.BasicClassLoaderInfo;
import software.coley.instrument.data.ClassData;
import software.coley.instrument.data.ServerClassLoaderInfo;
import software.coley.instrument.message.broadcast.BroadcastClassMessage;
import software.coley.instrument.message.broadcast.BroadcastClassloaderMessage;
import software.coley.instrument.util.JavaVersion;
import software.coley.instrument.util.Logger;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link Instrumentation} and {@link ClassFileTransformer}.
 *
 * @author Matt Coley
 * @author xxDark
 */
public final class InstrumentationHelper implements ClassFileTransformer {
	private static final ProtectionDomain OUR_DOMAIN = Agent.class.getProtectionDomain();
	// Config
	public static boolean notrampolines;
	// ClassLoader collections
	private final Map<Integer, LoaderData> loaders = new HashMap<>();
	// Instrumentation
	private final Lock lock = new ReentrantLock();
	private final Instrumentation instrumentation;
	private final Server server;

	public InstrumentationHelper(Server server, Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
		this.server = server;
		// Can be null for test purposes
		if (instrumentation != null) {
            instrumentation.addTransformer(this, true);
			populateExisting();
		}
	}

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (className != null && !isSelf(protectionDomain) && !isBlacklisted(loader)) {
            getOrCreateDataWrapper(loader)
                    .update(className, classBeingRedefined, classfileBuffer);
            Logger.debug("Collecting class: " + className + ",loader hash: " + (loader != null ? Integer.toHexString(loader.hashCode()) : null));
        }
        return null;
    }

	/**
	 * @param protectionDomain
	 * 		Some protection domain.
	 *
	 * @return {@code true} when the domain matches the agent's domain.
	 * Given that the agent is loaded from a jar, no other class, outside of the agent's own, should use this domain.
	 */
	private static boolean isSelf(ProtectionDomain protectionDomain) {
		return OUR_DOMAIN == protectionDomain;
	}

	/**
	 * @param loader
	 * 		Classloader instance.
	 *
	 * @return Data wrapper for loader.
	 */
	private LoaderData getOrCreateDataWrapper(ClassLoader loader) {
		lock.lock();
		try {
			if (loader == null) {
				return loaders.computeIfAbsent(ApiConstants.BOOTSTRAP_CLASSLOADER_ID,
						i -> new LoaderData(ServerClassLoaderInfo.BOOTSTRAP));
			} else if (loader == ServerClassLoaderInfo.SCL) {
				return loaders.computeIfAbsent(ApiConstants.SYSTEM_CLASSLOADER_ID,
						i -> new LoaderData(ServerClassLoaderInfo.SYSTEM));
			} else {
				int id = loader.hashCode();
				return loaders.computeIfAbsent(id, i -> new LoaderData(ServerClassLoaderInfo.fromLoader(loader)));
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Call {@link LoaderData#update(String, Class, byte[])} with existing classes from
	 * {@link Instrumentation#getAllLoadedClasses()}.
	 */
    private void populateExisting() {
        HashSet<Class<?>> allClasses = new HashSet<>();
        // This part of the code is attributed to the summary of the test of a springboot2.x application,
        // jdk range from 8 to 24 (except 9,10,12,14)
        for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
            ClassLoader classLoader = cls.getClassLoader();
            String clsName = cls.getName();
            if (isSelf(cls.getProtectionDomain())) {
                Logger.debug("Ignore the agent's own class:: " + clsName);
                continue;
            }
            if (isBlacklisted(classLoader)) {
                Logger.debug("Ignore class with loader in blacklist: " + clsName);
                continue;
            }
            if (clsName.contains("$$Lambda")) {
                // because jdk do not support retransform lambda class: https://github.com/alibaba/arthas/issues/1512.
                Logger.debug("Ignore lambda class: " + clsName);
                continue;
            }
            if (clsName.startsWith("[")) {
                Logger.debug("Ignore array class: " + clsName);
                continue;
            }
            if(JavaVersion.getMajorVersion() <= 8 && maybeCauseFatalError(clsName)){
                Logger.debug("Ignore class which may cause fatal error: " + clsName);
                continue;
            }
            if (JavaVersion.getMajorVersion() >= 13 && maybeInvalidClass(clsName)) {
                Logger.debug("Ignore potentially invalid class: " + clsName);
                continue;
            }
            if (JavaVersion.getMajorVersion() >= 11 && !instrumentation.isModifiableClass(cls)) {
                Logger.debug("Ignore unmodifiable class: " + clsName);
                continue;
            }
            allClasses.add(cls);
        }
        // DEBUG : retransformClassesDebug(allClasses);
        retransformClasses(allClasses);
    }

    private boolean maybeCauseFatalError(String clsName) {
        return clsName.matches("^java\\.lang\\.invoke\\.LambdaForm\\$(B)?MH/\\d+$");
    }

    private boolean maybeInvalidClass(String clsName) {
        switch (clsName) {
            // TODO: It is speculated that the common characteristic of these classes is that
            //  they contain references to classes that do not exist in the current runtime JVM,
            //  which are used as attributes or return values, or referenced by static methods.
            //  There may not yet be a reliable and performant way to separate these classes
            case "org.springframework.transaction.interceptor.TransactionAspectSupport$ReactiveTransactionSupport":
            case "org.springframework.boot.autoconfigure.cache.InfinispanCacheConfiguration":
            case "org.springframework.http.codec.multipart.DefaultPartHttpMessageReader":
            case "org.springframework.boot.logging.log4j2.Log4J2LoggingSystem":
            case "org.springframework.web.multipart.commons.CommonsMultipartResolver":
            case "org.springframework.boot.autoconfigure.cache.RedisCacheConfiguration":
            case "groovy.grape.GrapeIvy":
            case "org.mariadb.jdbc.client.socket.impl.UnixDomainSocket":
            case "org.springframework.web.multipart.commons.CommonsFileUploadSupport":
            case "org.springframework.transaction.reactive.TransactionalOperator":
                return true;
            default:
                return false;
        }
    }

    /**
     * @param classes All class which will be retransformed without error
     */
    public void retransformClasses(Set<Class<?>> classes) {
        Logger.debug("Retransforming classes: " + classes.size());
        try {
            instrumentation.retransformClasses(classes.toArray(new Class[0]));
        } catch (Throwable e) {
            Logger.error("Retransform Classes class error, msg: " + e.getMessage());
        }
    }

    /**
     * TODO: Executing the retransformClasses method one by one has slightly lower performance.
     * TODO: This method is used to find which class make something wrong!
     * TODO: It will be removed when the API is stable.
     *
     * @param classes All class which will be retransformed without error
     */
    public void retransformClassesDebug(Set<Class<?>> classes) {
        Logger.debug("Retransforming classes: " + classes.size());
        for (Class<?> clazz : classes) {
            try {
                Logger.debug("Retransforming class: " + clazz.getName() + ", loader hash: " +
                        (clazz.getClassLoader()!= null ? Integer.toHexString(clazz.getClassLoader().hashCode()) : null));
                instrumentation.retransformClasses(clazz);
            } catch (Throwable e) {
                Logger.error("Retransform class error, msg: " + e.getMessage() +
                        ", class: " + clazz.getName() + ", loader hash: " +
                        (clazz.getClassLoader() != null ? Integer.toHexString(clazz.getClassLoader().hashCode()) : null));
            }
        }
    }

	/**
	 * @return All loaders.
	 */
	public Collection<ServerClassLoaderInfo> getLoaders() {
		return new HashSet<>(loaders.values()).stream()
				.map(i -> i.loaderInfo)
				.sorted(Comparator.comparingInt(BasicClassLoaderInfo::getId))
				.collect(Collectors.toList());
	}

	/**
	 * @param loaderId
	 * 		Classloader id.
	 *
	 * @return All class names the classloader is responsible for.
	 */
	public Set<String> getLoaderClasses(int loaderId) {
		LoaderData data = loaders.get(loaderId);
		if (data == null)
			return Collections.emptySet();
		return data.bytecode.keySet();
	}

	/**
	 * @param loaderId
	 * 		Classloader id.
	 * @param className
	 * 		Name of class.
	 *
	 * @return Bytecode of class.
	 */
	public byte[] getClassBytecode(int loaderId, String className) {
		LoaderData data = loaders.get(loaderId);
		if (data == null)
			return null;
		return data.bytecode.get(className);
	}

	/**
	 * @param loaderId
	 * 		Classloader id.
	 * @param name
	 * 		Name of class.
	 *
	 * @return Class data, containing classloader info and bytecode.
	 */
	public ClassData getClassData(int loaderId, String name) {
		byte[] code = getClassBytecode(loaderId, name);
		return new ClassData(name, loaderId, code);
	}

	/**
	 * Redefine the given class.
	 *
	 * @param loaderId
	 * 		Classloader id.
	 * @param className
	 * 		Name of class.
	 * @param code
	 * 		Bytecode to use for redefinition.
	 *
	 * @return Failure reason, or {@code null} for success.
	 *
	 * @throws UnmodifiableClassException
	 * 		When the class is not modifiable.
	 * @throws ClassNotFoundException
	 * 		When the class is not found.
	 */
	public String redefineClass(int loaderId, String className, byte[] code) throws UnmodifiableClassException, ClassNotFoundException {
		LoaderData data = loaders.get(loaderId);
		if (data == null)
			return "Unknown classloader " + loaderId;
		Class<?> ref = data.refs.get(className);
		if (ref == null)
			ref = data.tryLoad(className);
		if (ref == null)
			return "Unknown class '" + className + "' in loader " + loaderId;
		ClassDefinition def = new ClassDefinition(ref, code);
		instrumentation.redefineClasses(def);
		data.bytecode.put(className, code);
		return null;
	}

	/**
	 * Acquire lock.
	 */
	public void lock() {
		lock.lock();
	}

	/**
	 * Release lock.
	 */
	public void unlock() {
		lock.unlock();
	}

	/**
	 * @return Instrumentation instance.
	 */
	public Instrumentation instrumentation() {
		return instrumentation;
	}

	/**
	 * @param loader
	 * 		Loader to check. May be {@code null}.
	 *
	 * @return {@code true} when we want to skip looking at the contents of this classloader.
	 */
	private static boolean isBlacklisted(ClassLoader loader) {
		if (loader == null) return false;

		String name = loader.getClass().getName();
		if (notrampolines && (name.equals("jdk.internal.reflect.DelegatingClassLoader")
				|| name.equals("sun.reflect.DelegatingClassLoader")
				|| name.equals("sun.reflect.misc.MethodUtil")))
			return true;

		return false;
	}

	private class LoaderData {
		private final ServerClassLoaderInfo loaderInfo;
		private final Map<String, byte[]> bytecode = new HashMap<>();
		private final Map<String, Class<?>> refs = new HashMap<>();

		LoaderData(ServerClassLoaderInfo loaderInfo) {
			this.loaderInfo = loaderInfo;
			server.broadcast(new BroadcastClassloaderMessage(loaderInfo));
		}

		void update(String className, Class<?> ref, byte[] code) {
			bytecode.put(className, code);
			// Null when called as class-init
			if (ref != null)
				refs.put(className, ref);
			// Broadcast class update
			server.broadcast(new BroadcastClassMessage(new ClassData(className, loaderInfo.getId(), code)));
		}

		Class<?> tryLoad(String name) {
			try {
				Class<?> cls = Class.forName(name.replace('/', '.'), false, loaderInfo.getClassLoader());
				refs.put(name, cls);
				return cls;
			} catch (Exception ex) {
				return null;
			}
		}
	}
}