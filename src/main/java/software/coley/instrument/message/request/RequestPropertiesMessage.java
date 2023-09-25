package software.coley.instrument.message.request;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.reply.ReplyPropertiesMessage;

/**
 * Message to request system {@link java.util.Properties} from the server.
 *
 * @author Matt Coley
 * @see ReplyPropertiesMessage
 */
public class RequestPropertiesMessage extends AbstractRequestMessage<ReplyPropertiesMessage> {
	public static final StructureCodec<RequestPropertiesMessage> CODEC =
			CommonCodecs.emptyMessage(RequestPropertiesMessage::new);

	/**
	 * Empty request.
	 */
	public RequestPropertiesMessage() {
	}
}
