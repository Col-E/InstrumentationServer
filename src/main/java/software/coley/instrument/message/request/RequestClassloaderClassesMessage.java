package software.coley.instrument.message.request;

import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.reply.ReplyClassloaderClassesMessage;

/**
 * Message to request all classes belonging to a classloader.
 *
 * @author Matt Coley
 */
public class RequestClassloaderClassesMessage extends AbstractRequestMessage<ReplyClassloaderClassesMessage> {
	public static final StructureCodec<RequestClassloaderClassesMessage> CODEC =
			StructureCodec.compose(input -> new RequestClassloaderClassesMessage(input.readInt()),
					((output, value) -> output.writeInt(value.getLoaderId())));
	private final int loaderId;

	/**
	 * @param loaderId
	 * 		ClassLoader id.
	 */
	public RequestClassloaderClassesMessage(int loaderId) {
		this.loaderId = loaderId;
	}

	/**
	 * @return ClassLoader id.
	 */
	public int getLoaderId() {
		return loaderId;
	}
}
