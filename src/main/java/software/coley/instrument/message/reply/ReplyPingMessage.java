package software.coley.instrument.message.reply;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.request.RequestPingMessage;

/**
 * Message to reply to a ping with a pong.
 *
 * @author Matt Coley
 * @see RequestPingMessage
 */
public class ReplyPingMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplyPingMessage> CODEC =
			CommonCodecs.emptyMessage(ReplyPingMessage::new);

	/**
	 * Empty reply.
	 */
	public ReplyPingMessage() {
	}
}
