package software.coley.instrument.message.reply;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Message to reply to a ping with a pong.
 *
 * @author Matt Coley
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
