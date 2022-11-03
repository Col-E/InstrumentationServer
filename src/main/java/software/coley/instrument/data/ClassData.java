package software.coley.instrument.data;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

import java.util.Arrays;

public class ClassData {
	private static final byte[] EMPTY_ARRAY = new byte[0];
	public static final StructureCodec<ClassData> CODEC =
			StructureCodec.compose(input -> new ClassData(input.readUTF(), input.readInt(), CommonCodecs.BYTE_ARRAY.decode(input)),
					(output, value) -> {
						output.writeUTF(value.getName());
						output.writeInt(value.getClassLoaderId());
						CommonCodecs.BYTE_ARRAY.encode(output, value.getCode());
					});
	private final String name;
	private final int classLoaderId;
	private final byte[] code;

	public ClassData(String name, int classLoaderId, byte[] code) {
		this.name = name;
		this.classLoaderId = classLoaderId;
		this.code = code == null ? EMPTY_ARRAY : code;
	}

	public boolean hasCode() {
		return code != EMPTY_ARRAY;
	}

	public String getName() {
		return name;
	}

	public int getClassLoaderId() {
		return classLoaderId;
	}

	public byte[] getCode() {
		return code;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ClassData classData = (ClassData) o;
		if (classLoaderId != classData.classLoaderId) return false;
		if (!name.equals(classData.name)) return false;
		return Arrays.equals(code, classData.code);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + Arrays.hashCode(code);
		result = 31 * result + classLoaderId;
		return result;
	}

	@Override
	public String toString() {
		return "ClassData{" +
				"name='" + name + '\'' +
				", classLoaderId=" + classLoaderId +
				", code=byte[" + code.length +
				"]}";
	}
}
