package software.coley.instrument.data;

import software.coley.instrument.ApiConstants;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public final class ServerClassLoaderInfo extends BasicClassLoaderInfo {
	public static final ServerClassLoaderInfo BOOTSTRAP
			= new ServerClassLoaderInfo(null, ApiConstants.BOOTSTRAP_CLASSLOADER_ID, "Bootstrap ClassLoader");
	public static final ServerClassLoaderInfo SYSTEM
			= new ServerClassLoaderInfo(ClassLoader.getSystemClassLoader(), ApiConstants.SYSTEM_CLASSLOADER_ID, "System ClassLoader");
	public static final ClassLoader SCL = ClassLoader.getSystemClassLoader();
	private static final WeakReference<ClassLoader> BOOTSTRAP_REF = new WeakReference<>(null);
	private static final WeakReference<ClassLoader> SCL_REF = new WeakReference<>(ClassLoader.getSystemClassLoader());
	private static final Method CLASS_LOADER_NAME;
	private final WeakReference<ClassLoader> ref;

	public ServerClassLoaderInfo(ClassLoader loader, int id, String name) {
		super(id, name);
		if (loader == null)
			ref = BOOTSTRAP_REF;
		else if (loader == SCL)
			ref = SCL_REF;
		else
			ref = new WeakReference<>(loader);
	}

	public static ServerClassLoaderInfo fromLoader(ClassLoader loader) {
		if (loader == null) {
			return BOOTSTRAP;
		} else if (loader == SCL) {
			return SYSTEM;
		} else {
			String name = lookupClassLoaderName(loader);
			int id = loader.hashCode();
			return new ServerClassLoaderInfo(loader, id, name);
		}
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

	public ClassLoader getClassLoader() {
		return ref.get();
	}

	public boolean isValid() {
		WeakReference<ClassLoader> ref = this.ref;
		return ref == BOOTSTRAP_REF || ref.get() != null;
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
