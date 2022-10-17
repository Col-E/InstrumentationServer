package software.coley.instrument;

import software.coley.instrument.data.ClassData;
import software.coley.instrument.data.ServerClassLoaderInfo;
import software.coley.instrument.util.Logger;
import software.coley.instrument.util.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrapper around {@link Instrumentation} and {@link ClassFileTransformer}.
 *
 * @author Matt Coley
 * @author xxDark
 */
public final class InstrumentationHelper implements ClassFileTransformer {
	private static final ClassLoader SCL = ClassLoader.getSystemClassLoader();
	private static final Method CLASS_LOADER_NAME;
	// ClassLoader collections
	private final Map<Integer, ServerClassLoaderInfo> loaders = new HashMap<>();
	private final Map<Integer, Set<String>> loaderToClasses = new HashMap<>();
	private final Map<String, ServerClassLoaderInfo> classesToLoader = new HashMap<>();
	// Classes collections
	private final Map<String, byte[]> classesToCode = new HashMap<>();
	private final Map<String, Class<?>> classesToRef = new HashMap<>();
	private final SortedSet<String> sortedClassNames = new TreeSet<>();
	private final Set<String> newClassNames = new HashSet<>();
	// Instrumentation
	private final Lock lock = new ReentrantLock();
	private final Instrumentation instrumentation;

	public InstrumentationHelper(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
		// Can be null for test purposes
		if (instrumentation != null)
			instrumentation.addTransformer(this, true);
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
							ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		recordClass(loader, className, classBeingRedefined, classfileBuffer);
		return classfileBuffer;
	}

	/**
	 * Records class data.
	 *
	 * @param loader
	 * 		Class's loader.
	 * @param className
	 * 		Internal name of class.
	 * @param ref
	 * 		Reference, {@code null} for class init when recorded via
	 *        {@link #transform(ClassLoader, String, Class, ProtectionDomain, byte[])}.
	 * @param code
	 * 		Bytecode of class.
	 */
	private void recordClass(ClassLoader loader, String className, Class<?> ref, byte[] code) {
		if (className == null)
			return;
		// ClassLoader items
		ServerClassLoaderInfo loaderInfo = getInfoForClassLoader(loader);
		int loaderId = loaderInfo.getId();
		classesToLoader.put(className, loaderInfo);
		loaderToClasses.computeIfAbsent(loaderId, l -> new HashSet<>()).add(className);
		loaders.putIfAbsent(loaderId, loaderInfo);
		// Class items
		classesToCode.put(className, code);
		sortedClassNames.add(className);
		newClassNames.add(className);
		// Null when called as class-init
		if (ref != null)
			classesToRef.put(className, ref);
	}

	/**
	 * Call {@link #recordClass(ClassLoader, String, Class, byte[])} with existing classes from
	 * {@link Instrumentation#getAllLoadedClasses()}.
	 */
	private void populateExisting() {
		for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
			String name = cls.getName().replace('.', '/');
			InputStream clsStream = ClassLoader.getSystemResourceAsStream(name + ".class");
			if (clsStream != null) {
				ClassLoader loader = cls.getClassLoader();
				try {
					byte[] code = Streams.readStream(clsStream);
					recordClass(loader, name, cls, code);
				} catch (IOException e) {
					Logger.debug("Failed to read existing class: " + name);
				}
			}
		}
	}

	/**
	 * @return New class names since the last time this method was called.
	 */
	public SortedSet<String> getNewClassNames() {
		TreeSet<String> set = new TreeSet<>(newClassNames);
		newClassNames.clear();
		return set;
	}

	/**
	 * @return All class names.
	 */
	public Set<String> getAllClasses() {
		return sortedClassNames;
	}

	/**
	 * @return All loaders.
	 */
	public Collection<ServerClassLoaderInfo> getLoaders() {
		return loaders.values();
	}

	/**
	 * @param className
	 * 		Name of class.
	 *
	 * @return Containing classloader.
	 */
	public ServerClassLoaderInfo getLoaderOfClass(String className) {
		return classesToLoader.get(className);
	}

	/**
	 * @param loaderKey
	 * 		Loader hash.
	 *
	 * @return Matching loader.
	 */
	public ServerClassLoaderInfo getClassLoader(int loaderKey) {
		lock.lock();
		try {
			if (loaderKey == 0)
				return null;
			for (ServerClassLoaderInfo loader : getLoaders())
				if (loader.hashCode() == loaderKey)
					return loader;
			return null;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @param loaderId
	 * 		Classloader id.
	 *
	 * @return All class names the classloader is responsible for.
	 */
	public Set<String> getLoaderClasses(int loaderId) {
		return loaderToClasses.getOrDefault(loaderId, Collections.emptySet());
	}

	/**
	 * @param className
	 * 		Name of class.
	 *
	 * @return Bytecode of class.
	 */
	public byte[] getClassBytecode(String className) {
		return classesToCode.get(className);
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
		byte[] code = getClassBytecode(name);
		return new ClassData(name, loaderId, code);
	}

	/**
	 * Redefine the given class.
	 *
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
	public void redefineClass(String className, byte[] code) throws UnmodifiableClassException, ClassNotFoundException {
		Class<?> ref = getClassRef(className);
		ClassDefinition def = new ClassDefinition(ref, code);
		instrumentation.redefineClasses(def);
		classesToCode.put(className, code);
	}

	/**
	 * @param className
	 * 		Internal class name.
	 *
	 * @return Reference of class.
	 *
	 * @throws ClassNotFoundException
	 * 		When class is not found.
	 */
	private Class<?> getClassRef(String className) throws ClassNotFoundException {
		Class<?> ref = classesToRef.get(className);
		if (ref == null) {
			// Populate ref if available.
			ref = Class.forName(className.replace('/', '.'), false, classesToLoader.get(className).getClassLoader());
			classesToRef.put(className, ref);
		}
		return ref;
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

	private static ServerClassLoaderInfo getInfoForClassLoader(ClassLoader loader) {
		String name;
		int id;
		if (loader == null) {
			name = "Bootstrap ClassLoader";
			id = 0;
		} else if (loader == SCL) {
			name = "System ClassLoader";
			id = 1;
		} else {
			name = lookupClassLoaderName(loader);
			id = loader.hashCode();
		}
		return new ServerClassLoaderInfo(loader, id, name);
	}

	private static String lookupClassLoaderName(ClassLoader loader) {
		Method m = CLASS_LOADER_NAME;
		if (m != null) {
			try {
				String name = (String) m.invoke(loader);
				if (name != null)
					return name;
			} catch (ReflectiveOperationException ex) {
				throw new IllegalStateException("Could not get class loader name", ex);
			}
		}
		if (loader == SCL) {
			return "Application ClassLoader";
		} else if (loader == SCL.getParent()) {
			return "Ext ClassLoader";
		}
		return loader.toString();
	}

	static {
		Method getClassLoaderName;
		try {
			// Added in Java 9
			getClassLoaderName = ClassLoader.class.getMethod("getName");
			getClassLoaderName.setAccessible(true);
		} catch (NoSuchMethodException ignored) {
			getClassLoaderName = null;
		}
		CLASS_LOADER_NAME = getClassLoaderName;
	}
}