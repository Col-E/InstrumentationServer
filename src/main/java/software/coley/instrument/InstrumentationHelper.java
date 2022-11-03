package software.coley.instrument;

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
import java.util.*;
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
			populateExisting();
			instrumentation.addTransformer(this, true);
		}
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (className != null && !isSelf(protectionDomain))
			getOrCreateDataWrapper(loader)
					.update(className, classBeingRedefined, classfileBuffer);
		return classfileBuffer;
	}

	/**
	 * @param protectionDomain
	 * 		Some protection domain.
	 *
	 * @return {@code true} when the domain matches the agent's domain.
	 * Given that the agent is loaded from a jar, no other class, outside of the agent's own, should use this domain.
	 */
	private static boolean isSelf(ProtectionDomain protectionDomain) {
		return Agent.class.getProtectionDomain() == protectionDomain;
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
		for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
			if (isSelf(cls.getProtectionDomain()))
				continue;
			String name = cls.getName().replace('.', '/');
			InputStream clsStream = ClassLoader.getSystemResourceAsStream(name + ".class");
			if (clsStream != null) {
				ClassLoader loader = cls.getClassLoader();
				try {
					byte[] code = Streams.readStream(clsStream);
					getOrCreateDataWrapper(loader)
							.update(name, cls, code);
				} catch (IOException e) {
					Logger.debug("Failed to read existing class: " + name);
				}
			}
		}
	}

	/**
	 * @return All loaders.
	 */
	public Collection<ServerClassLoaderInfo> getLoaders() {
		return loaders.values().stream()
				.map(i -> i.loaderInfo)
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
	 * @throws UnmodifiableClassException
	 * 		When the class is not modifiable.
	 * @throws ClassNotFoundException
	 * 		When the class is not found.
	 */
	public void redefineClass(int loaderId, String className, byte[] code) throws UnmodifiableClassException, ClassNotFoundException {
		LoaderData data = loaders.get(loaderId);
		if (data == null)
			return;
		Class<?> ref = data.refs.get(className);
		if (ref != null) {
			ClassDefinition def = new ClassDefinition(ref, code);
			instrumentation.redefineClasses(def);
			data.bytecode.put(className, code);
		}
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
	}
}