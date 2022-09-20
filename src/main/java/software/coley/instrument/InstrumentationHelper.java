package software.coley.instrument;

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

/**
 * Wrapper around {@link Instrumentation} and {@link ClassFileTransformer}.
 *
 * @author Matt Coley
 */
public class InstrumentationHelper implements ClassFileTransformer {
	private final Instrumentation instrumentation;
	private final Map<ClassLoader, Set<String>> loaderToClasses = new IdentityHashMap<>();
	private final Map<String, ClassLoader> classesToLoader = new HashMap<>();
	private final Map<String, byte[]> classesToCode = new HashMap<>();
	private final Map<String, Class<?>> classesToRef = new HashMap<>();
	private final SortedSet<String> sortedClassNames = new TreeSet<>();
	private final Set<String> newClassNames = new HashSet<>();

	public InstrumentationHelper(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
		this.instrumentation.addTransformer(this, true);
		populateExisting();
	}

	@Override
	public byte[] transform(ClassLoader loader, String name, Class<?> cls, ProtectionDomain domain, byte[] code) {
		recordClass(loader, name, cls, code);
		return code;
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
	public Set<ClassLoader> getLoaders() {
		return loaderToClasses.keySet();
	}

	/**
	 * @param className
	 * 		Name of class.
	 *
	 * @return Containing classloader.
	 */
	public ClassLoader getLoaderOfClass(String className) {
		return classesToLoader.get(className);
	}

	/**
	 * @param loader
	 * 		Loader instance.
	 *
	 * @return All class names the loader is responsible for.
	 */
	public Set<String> getLoaderClasses(ClassLoader loader) {
		return loaderToClasses.getOrDefault(loader, Collections.emptySet());
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
			ref = Class.forName(className.replace('/', '.'), false, classesToLoader.get(className));
			classesToRef.put(className, ref);
		}
		return ref;
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
		classesToLoader.put(className, loader);
		loaderToClasses.computeIfAbsent(loader, l -> new HashSet<>()).add(className);
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
}
