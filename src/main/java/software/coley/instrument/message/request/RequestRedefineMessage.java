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
			StructureCodec.compose(input -> new RequestRedefineMessage(input.readInt(), input.readUTF(), CommonCodecs.BYTE_ARRAY.decode(input)),
					(output, value) -> {
						output.writeInt(value.getLoaderId());
						output.writeUTF(value.getClassName());
						CommonCodecs.BYTE_ARRAY.encode(output, value.getBytecode());
					});
	private final int loaderId;
	private final String name;
	private final byte[] code;

	/**
	 * @param loaderId
	 * 		Classloader id.
	 * @param name
	 * 		Class name.
	 * @param code
	 * 		Class bytecode.
	 */
	public RequestRedefineMessage(int loaderId, String name, byte[] code) {
		this.loaderId = loaderId;
		this.name = name;
		this.code = code;
	}

	/**
	 * @return Classloader id.
	 */
	public int getLoaderId() {
		return loaderId;
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
