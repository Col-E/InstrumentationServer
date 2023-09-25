package software.coley.instrument.message.request;

import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.reply.ReplyClassMessage;

/**
 * Message to request bytecode of a class.
 *
 * @author Matt Coley
 * @see ReplyClassMessage
 */
public class RequestClassMessage extends AbstractRequestMessage<ReplyClassMessage> {
	public static final StructureCodec<RequestClassMessage> CODEC =
			StructureCodec.compose(input -> new RequestClassMessage(input.readInt(), input.readUTF()),
					(output, value) -> {
						output.writeInt(value.getLoaderId());
						output.writeUTF(value.getName());
					});
	private final int loaderId;
	private final String name;

	/**
	 * @param loaderId
	 * 		Classloader id.
	 * @param name
	 * 		Class name.
	 */
	public RequestClassMessage(int loaderId, String name) {
		this.loaderId = loaderId;
		this.name = name;
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
	public String getName() {
		return name;
	}
}
