package software.coley.instrument.message.request;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.reply.ReplyRedefineMessage;

/**
 * Message to request a redefinition of a class on the server.
 *
 * @author Matt Coley
 */
public class RequestRedefineMessage extends AbstractRequestMessage<ReplyRedefineMessage> {
	public static final StructureCodec<RequestRedefineMessage> CODEC =
			StructureCodec.compose(input -> new RequestRedefineMessage(input.readUTF(), CommonCodecs.BYTE_ARRAY.decode(input)),
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
	public RequestRedefineMessage(String name, byte[] code) {
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
