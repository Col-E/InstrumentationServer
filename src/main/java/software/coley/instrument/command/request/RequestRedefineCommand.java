package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to request a redefinition of a class on the server.
 *
 * @author Matt Coley
 */
public class RequestRedefineCommand extends AbstractCommand {
	public static final StructureCodec<RequestRedefineCommand> CODEC =
			StructureCodec.compose(input -> new RequestRedefineCommand(input.readUTF(), CommonCodecs.BYTE_ARRAY.decode(input)),
					(output, value) -> {
						output.writeUTF(value.getClassName());
						CommonCodecs.BYTE_ARRAY.encode(output, value.getBytecode());
					});
	private final String name;
	private final byte[] code;

	/**
	 * @param name
	 * 		Class name.
	 * @param code
	 * 		Class bytecode.
	 */
	public RequestRedefineCommand(String name, byte[] code) {
		this.name = name;
		this.code = code;
	}

	/**
	 * @return Class name.
	 */
	public String getClassName() {
		return name;
	}

	/**
	 * @return Class bytecode to redefine.
	 */
	public byte[] getBytecode() {
		return code;
	}
}
