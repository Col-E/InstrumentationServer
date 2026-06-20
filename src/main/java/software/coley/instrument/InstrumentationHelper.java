package software.coley.instrument;

import software.coley.instrument.data.BasicClassLoaderInfo;
import software.coley.instrument.data.ClassData;
import software.coley.instrument.data.ServerClassLoaderInfo;
import software.coley.instrument.message.broadcast.BroadcastClassMessage;
import software.coley.instrument.message.broadcast.BroadcastClassloaderMessage;
import software.coley.instrument.util.Logger;
import software.coley.instrument.util.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;
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
	public static int existingClassBatchSize = 128;

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
			// We register our hook in the transformation manager that ensures our transformer is always executed last.
			installTransformerManagerHook();

			// Our transformer manager hook does assume our transformer is registered, so we register it even if the hook fails.
			// If it fails we just don't get the benefit of always being last.
			instrumentation.addTransformer(this, true);

			// Broadcast existing classes to clients.
			populateExisting();
		}
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
	                        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		try {
			if (className != null && !isSelf(protectionDomain) && !isBlacklisted(loader))
				getOrCreateDataWrapper(loader)
						.update(className, classBeingRedefined, classfileBuffer);
			return null;
		} catch (Throwable t) {
			Logger.error("Failed to process class " + className + " for loader " + loader + ": " + t);
			return null;
		}
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
		ClassLoader bootClassLoader = new ClassLoader(null) {
		};
		Class<?>[] transformBatch = new Class[existingClassBatchSize];
		Class[] allLoadedClasses = instrumentation.getAllLoadedClasses();
		int batchIndex = 0;
		for (int i = 0; i < allLoadedClasses.length; i++) {
			Class<?> cls = allLoadedClasses[i];
			ClassLoader loader = cls.getClassLoader();
			if (isSelf(cls.getProtectionDomain()))
				continue;
			if (isBlacklisted(loader) || !instrumentation.isModifiableClass(cls))
				continue;
			transformBatch[batchIndex++] = cls;
			if (batchIndex == transformBatch.length) {
				batchIndex = 0;
				transformBatch(transformBatch, bootClassLoader);
			}
		}
		if (batchIndex != 0) {
			transformBatch(Arrays.copyOf(transformBatch, batchIndex), bootClassLoader);
		}
	}

	/**
	 * Retransform a batch of classes, falling back to individual retransformation if the batch fails.
	 *
	 * @param batch
	 * 		Array of classes to retransform.
	 * @param bootClassLoader
	 * 		Bootstrap classloader to use when reading class bytecode for failed retransformation.
	 */
	private void transformBatch(Class<?>[] batch, ClassLoader bootClassLoader) {
		try {
			instrumentation.retransformClasses(batch);
		} catch (Throwable e) {
			for (Class<?> cls : batch) {
				try {
					instrumentation.retransformClasses(cls);
				} catch (Throwable t) {
					Logger.error("Failed to retransform existing class: " + cls + " - " + t);
					ClassLoader loader = cls.getClassLoader();
					String name = cls.getName().replace('.', '/');
					try (InputStream clsStream = (loader == null ? bootClassLoader : loader).getResourceAsStream(name + ".class")) {
						if (clsStream == null) {
							Logger.warn("Failed to find existing class: " + name);
							continue;
						}
						byte[] code = Streams.readStream(clsStream);
						getOrCreateDataWrapper(loader)
								.update(name, cls, code);
					} catch (IOException ioex) {
						Logger.warn("Failed to read existing class: " + name);
					}
				}
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
	 * Installs a transformer that hooks into {@code sun.instrument.TransformerManager}
	 * so our transformer is always executed last.
	 *
	 * @return {@code true} when the hook was installed, otherwise {@code false}.
	 */
	private boolean installTransformerManagerHook() {
		// Skip if transformation is unavailable, we cannot hook into the internal transformer manager.
		if (!instrumentation.isRetransformClassesSupported()) {
			Logger.warn("Retransformation is unavailable, TransformerManager hook is disabled");
			return false;
		}

		ClassFileTransformer[] transformerWrapper = new ClassFileTransformer[1];
		try {
			Class<?> transformerManagerClass = Class.forName("sun.instrument.TransformerManager", false, null);
			InstrumentationImplHookTransformer transformerImpl = new InstrumentationImplHookTransformer(transformerManagerClass);
			transformerWrapper[0] = transformerImpl;
			instrumentation.addTransformer(transformerImpl, true);
			instrumentation.retransformClasses(transformerManagerClass);
			if (transformerImpl.wasInjected()) {
				Logger.info("Installed TransformerManager ordering hook");
				return true;
			}
			Logger.warn("TransformerManager ordering hook was not applied");
			return false;
		} catch (Throwable t) {
			// Not the end of the world, some use cases won't bundle ASM on the classpath. We're still mostly usable.
			// We won't be able to append ourselves to TransformerManager snapshots and must fall back to registration.
			Logger.warn("Failed to install TransformerManager hook, class observations may not be up-to-date with" +
					" other transformers present: " + t);
			return false;
		} finally {
			// Remove the hook transformer, we don't need it anymore.
			// The hook is already injected into the target class or failed to do so.
			ClassFileTransformer transformer = transformerWrapper[0];
			if (transformer != null)
				instrumentation.removeTransformer(transformer);
		}
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