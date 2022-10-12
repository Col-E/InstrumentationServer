package software.coley.instrument.data;


import software.coley.instrument.io.codec.StructureCodec;

public interface ClassLoaderInfo {
	StructureCodec<ClassLoaderInfo> CODEC = StructureCodec.compose(input -> {
		return new BasicClassLoaderInfo(input.readInt(), input.readUTF());
	}, (output, value) -> {
		output.writeInt(value.getId());
		output.writeUTF(value.getName());
	});

	default boolean isBootstrap() {
		return getId() == 0;
	}

	default boolean isSystem() {
		return getId() == 1;
	}

	int getId();

	String getName();
}
