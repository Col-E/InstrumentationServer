package software.coley.instrument.message.reply;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.request.RequestSetPropertyMessage;

/**
 * Message to notify {@link RequestSetPropertyMessage} completion.
 *
 * @author Matt Coley
 * @see RequestSetPropertyMessage
 */
public class ReplySetPropertyMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplySetPropertyMessage> CODEC =
			CommonCodecs.emptyMessage(ReplySetPropertyMessage::new);

	/**
	 * Empty reply.
	 */
	public ReplySetPropertyMessage() {
	}
}
