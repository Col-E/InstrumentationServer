package software.coley.instrument;

import software.coley.instrument.data.BasicClassLoaderInfo;
import software.coley.instrument.data.ClassData;
import software.coley.instrument.data.ServerClassLoaderInfo;
import software.coley.instrument.message.broadcast.BroadcastClassMessage;
import software.coley.instrument.message.broadcast.BroadcastClassloaderMessage;
import software.coley.instrument.util.Logger;
import software.coley.instrument.util.Streams;
import sun.instrument.InstrumentationHookBridge;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link Instrumentation} and {@link ClassFileTransformer}.
 *
 * @author Matt Coley
 * @author xxDark
 */
public final class InstrumentationHelper implements ClassFileTransformer {
	private static final ProtectionDomain OUR_DOMAIN = Agent.class.getProtectionDomain();
	private static volatile InstrumentationHelper activeHelper;
	// Config
	public static boolean notrampolines;
	// ClassLoader collections
	private final Map<Integer, LoaderData> loaders = new HashMap<>();
	// Instrumentation
	private final Lock lock = new ReentrantLock();
	private final Lock transformerOrderLock = new ReentrantLock();
	private final AtomicInteger transformerSignalBlock = new AtomicInteger();
	private final Instrumentation instrumentation;
	private final Server server;
	private volatile boolean transformerOrderHookInstalled;

	public InstrumentationHelper(Server server, Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
		this.server = server;
		activeHelper = this;
		// Can be null for test purposes
		if (instrumentation != null) {
			populateExisting();
			addSelfTransformer();
			installTransformerOrderingHook();
		}
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
	                        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		if (className != null && !isSelf(protectionDomain) && !isBlacklisted(loader))
			getOrCreateDataWrapper(loader)
					.update(className, classBeingRedefined, classfileBuffer);
		return classfileBuffer;
	}

	/**
	 * Called by the {@link InstrumentationHookBridge} injected into {@code sun.instrument.InstrumentationImpl}.
	 *
	 * @param transformer
	 * 		Transformer that was added.
	 * @param canRetransform
	 * 		Whether the transformer is retransform-capable.
	 */
	@SuppressWarnings("unused") // It isn't, called reflectively from the bootstrap bridge.
	public static void onTransformerAdded(ClassFileTransformer transformer, boolean canRetransform) {
		InstrumentationHelper helper = activeHelper;
		if (helper != null)
			helper.handleTransformerAdded(transformer, canRetransform);
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
		for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
			if (isSelf(cls.getProtectionDomain()))
				continue;
			String name = cls.getName().replace('.', '/');
			InputStream clsStream = ClassLoader.getSystemResourceAsStream(name + ".class");
			if (clsStream != null) {
				ClassLoader loader = cls.getClassLoader();
				if (isBlacklisted(loader))
					continue;
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
	 * Adds ourselves as an instrumentation transformer while also incrementing the transformer signal block counter.
	 */
	private void addSelfTransformer() {
		// We increment the signal block counter to avoid reordering ourselves when we add ourselves as a transformer.
		withTransformerSignalBlock(() -> instrumentation.addTransformer(this, true));
	}

	/**
	 * Installs a transformer that hooks into {@code sun.instrument.InstrumentationImpl}
	 * to track when new transformers are added and maintain our transformer as the terminal observer.
	 */
	private void installTransformerOrderingHook() {
		if (!instrumentation.isRetransformClassesSupported()) {
			Logger.warn("Retransformation is unavailable, transformer ordering hook is disabled");
			return;
		}

		if (!appendSelfToBootstrapSearch()) {
			Logger.warn("Failed to expose bootstrap hook bridge, transformer ordering hook is disabled");
			return;
		}

		ClassFileTransformer[] transformerWrapper = new ClassFileTransformer[1];
		try {
			InstrumentationImplHookTransformer transformerImpl = new InstrumentationImplHookTransformer(instrumentation.getClass());
			transformerWrapper[0] = transformerImpl;
			withTransformerSignalBlock(() -> instrumentation.addTransformer(transformerImpl, true));
			instrumentation.retransformClasses(instrumentation.getClass());
			transformerOrderHookInstalled = transformerImpl.wasInjected();
			if (transformerOrderHookInstalled)
				Logger.debug("Installed InstrumentationImpl transformer ordering hook");
			else
				Logger.warn("InstrumentationImpl transformer ordering hook was not applied");
		} catch (Throwable t) {
			// Not the end of the world, some use cases won't bundle ASM on the classpath. We're still mostly usable.
			// We won't be able to track transformer additions and maintain our transformer as the terminal observer.
			Logger.warn("Failed to install transformer ordering hook," +
					" class observations may not be up-to-date with other transformers present: " + t);
		} finally {
			// If ASM isn't available we cannot add the hook transformer, so we skip cleanup in that case to avoid NoClassDefFoundErrors.
			ClassFileTransformer transformer = transformerWrapper[0];
			if (transformer != null)
				withTransformerSignalBlock(() -> instrumentation.removeTransformer(transformer));
		}
	}

	/**
	 * @return {@code true} if we successfully appended a jar containing the bootstrap bridge to the bootstrap search, {@code false} otherwise.
	 */
	private boolean appendSelfToBootstrapSearch() {
		try {
			// We should be able to locate our hook class.
			InputStream bridgeStream = ClassLoader.getSystemResourceAsStream("sun/instrument/InstrumentationHookBridge.class");
			if (bridgeStream == null) {
				Logger.warn("Could not locate bootstrap hook bridge class bytes");
				return false;
			}

			// Create a temporary jar to append to the bootstrap search.
			Path bootstrapJar = Files.createTempFile("instrumentation-server-bootstrap-", ".jar");
			bootstrapJar.toFile().deleteOnExit();
			try (InputStream in = bridgeStream;
			     JarOutputStream jar = new JarOutputStream(Files.newOutputStream(bootstrapJar))) {
				jar.putNextEntry(new JarEntry("sun/instrument/InstrumentationHookBridge.class"));
				byte[] bridgeBytes = Streams.readStream(in);
				jar.write(bridgeBytes);
				jar.closeEntry();
			}

			// Append the jar to the bootstrap search.
			try (JarFile jarFile = new JarFile(bootstrapJar.toFile())) {
				instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
				return true;
			}
		} catch (IOException ex) {
			Logger.warn("Failed to append agent to bootstrap search: " + ex);
			return false;
		}
	}

	/**
	 * Handles a new transformer being added to the instrumentation instance.
	 * <p>
	 * We want to maintain ourselves as the terminal observer so that we see
	 * the final transformed state of classes after all other transformers
	 * have had their shot at transforming.
	 *
	 * @param transformer
	 * 		Transformer that was added.
	 * @param canRetransform
	 * 		Whether the transformer is retransform-capable.
	 *
	 * @see #onTransformerAdded(ClassFileTransformer, boolean)
	 */
	private void handleTransformerAdded(ClassFileTransformer transformer, boolean canRetransform) {
		// Skip if we don't have the hook installed, or if the transformer is null.
		if (!transformerOrderHookInstalled || transformer == null)
			return;
		if (transformer == this || transformerSignalBlock.get() > 0)
			return;

		// Keep our transformer as the terminal observer so cached bytecode reflects the final transformed state.
		transformerOrderLock.lock();
		try {
			if (transformerSignalBlock.get() > 0)
				return;
			withTransformerSignalBlock(() -> {
				boolean removed = instrumentation.removeTransformer(this);
				instrumentation.addTransformer(this, true);
				Logger.debug("Moved instrumentation listener to transformer tail after addTransformer(" +
						transformer.getClass().getName() + ", " + canRetransform + "), removed=" + removed);
			});
		} catch (RuntimeException ex) {
			Logger.warn("Failed to restore listener ordering: " + ex);
		} finally {
			transformerOrderLock.unlock();
		}
	}

	/**
	 * @param action
	 * 		Action to run while the transformer signal block is incremented.
	 */
	private void withTransformerSignalBlock(Runnable action) {
		transformerSignalBlock.incrementAndGet();
		try {
			action.run();
		} finally {
			transformerSignalBlock.decrementAndGet();
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