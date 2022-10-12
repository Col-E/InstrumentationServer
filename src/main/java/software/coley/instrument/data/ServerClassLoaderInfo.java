package software.coley.instrument.data;

import java.lang.ref.WeakReference;

public final class ServerClassLoaderInfo extends BasicClassLoaderInfo {
    private static final WeakReference<ClassLoader> BOOTSTRAP = new WeakReference<>(null);
    private final WeakReference<ClassLoader> ref;

    public ServerClassLoaderInfo(ClassLoader loader, int id, String name) {
        super(id, name);
        ref = loader == null ? BOOTSTRAP : new WeakReference<>(loader);
    }

    public ClassLoader getClassLoader() {
        return ref.get();
    }

    public boolean isValid() {
        WeakReference<ClassLoader> ref = this.ref;
        return ref == BOOTSTRAP || ref.get() != null;
    }
}
