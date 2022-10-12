package software.coley.instrument.data;

import java.util.Objects;

public class BasicClassLoaderInfo implements ClassLoaderInfo {
    private final int id;
    private final String name;

    public BasicClassLoaderInfo(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicClassLoaderInfo info = (BasicClassLoaderInfo) o;
        if (id != info.id) return false;
        return Objects.equals(name, info.name);
    }

    @Override
    public int hashCode() {
        return id;
    }
}