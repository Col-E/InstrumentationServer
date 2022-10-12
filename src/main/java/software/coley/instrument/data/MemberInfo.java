package software.coley.instrument.data;

import software.coley.instrument.io.codec.StructureCodec;

import java.util.Objects;

/**
 * Data wrapper for a member declaration info.
 *
 * @author Matt Coley
 */
public class MemberInfo {
	public static final StructureCodec<MemberInfo> CODEC =
			StructureCodec.compose(input -> new MemberInfo(input.readUTF(), input.readUTF(), input.readUTF()),
					(output, value) -> {
						output.writeUTF(value.getOwner());
						output.writeUTF(value.getName());
						output.writeUTF(value.getDesc());
					});
	private final String owner;
	private final String name;
	private final String desc;

	public MemberInfo(String owner, String name, String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	public String getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MemberInfo info = (MemberInfo) o;
		if (!Objects.equals(owner, info.owner)) return false;
		if (!Objects.equals(name, info.name)) return false;
		return Objects.equals(desc, info.desc);
	}

	@Override
	public int hashCode() {
		int result = owner != null ? owner.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (desc != null ? desc.hashCode() : 0);
		return result;
	}
}
