package software.coley.instrument.data;


import software.coley.instrument.ApiConstants;
import software.coley.instrument.io.codec.StructureCodec;

public interface ClassLoaderInfo {
	StructureCodec<ClassLoaderInfo> CODEC = StructureCodec.compose(input -> {
		return new BasicClassLoaderInfo(input.readInt(), input.readUTF());
	}, (output, value) -> {
		output.writeInt(value.getId());
		output.writeUTF(value.getName());
	});

	default boolean isBootstrap() {
		return getId() == ApiConstants.BOOTSTRAP_CLASSLOADER_ID;
	}

	default boolean isSystem() {
		return getId() == ApiConstants.SYSTEM_CLASSLOADER_ID;
	}

	int getId();

	String getName();
}
