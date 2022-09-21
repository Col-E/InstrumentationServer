package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.ByteGen;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Handles requesting classloaders.
 *
 * @author Matt Coley
 */
public class GetClassLoadersCommand extends AbstractCommand {
	private Collection<LoaderInfo> items = Collections.emptyList();

	public GetClassLoadersCommand() {
		super(ID_CL_GET_CLASSLOADERS);
	}

	public Collection<LoaderInfo> getItems() {
		return items;
	}

	public void setItems(Collection<LoaderInfo> items) {
		this.items = items;
	}

	@Override
	public void read(ByteBuffer in) {
		int count = in.getInt();
		items = new ArrayList<>();
		for (int i = 0; i < count; i++)
			items.add(LoaderInfo.read(in));
	}

	@Override
	public byte[] generate() {
		if (items == null)
			throw new IllegalStateException("Loader items not set before usage!");
		ByteGen gen = new ByteGen()
				.appendInt(items.size());
		for (LoaderInfo item : items)
			item.append(gen);
		return gen.build((byte) key());
	}

	@Override
	public String toString() {
		if (items == null)
			return "ClassLoadersCommand[empty]";
		return "ClassLoadersCommand[" + items.size() + "]";
	}

	public static class LoaderInfo implements Comparable<LoaderInfo> {
		private final String typeName;
		private final int hashCode;

		public static LoaderInfo from(ClassLoader loader) {
			if (loader == null)
				return new LoaderInfo("", 0);
			return new LoaderInfo(loader.getClass().getName().replace('.', '/'), loader.hashCode());
		}

		public LoaderInfo(String typeName, int hashCode) {
			this.typeName = typeName;
			this.hashCode = hashCode;
		}

		public static LoaderInfo read(ByteBuffer in) {
			String typeName = Buffers.getString(in);
			int hashCode = in.getInt();
			return new LoaderInfo(typeName, hashCode);
		}

		public void append(ByteGen gen) {
			gen.appendString(typeName);
			gen.appendInt(hashCode);
		}

		public String getTypeName() {
			return typeName;
		}

		public int getHashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			if (isBootstrap())
				return "LoaderInfo{Bootstrap}";
			return "LoaderInfo{" +
					"typeName='" + typeName + '\'' +
					", hashCode=" + hashCode +
					'}';
		}

		public boolean isBootstrap() {
			return hashCode == 0;
		}

		@Override
		public int compareTo(LoaderInfo o) {
			return typeName.compareTo(o.typeName);
		}
	}
}
