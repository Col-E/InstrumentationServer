package software.coley.instrument.message.request;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.reply.ReplyPingMessage;

/**
 * Message to request a pong reply from the server.
 *
 * @author Matt Coley
 * @see ReplyPingMessage
 */
public class RequestPingMessage extends AbstractRequestMessage<ReplyPingMessage> {
	public static final StructureCodec<RequestPingMessage> CODEC =
			CommonCodecs.emptyMessage(RequestPingMessage::new);

	/**
	 * Empty request.
	 */
	public RequestPingMessage() {
	}
}
