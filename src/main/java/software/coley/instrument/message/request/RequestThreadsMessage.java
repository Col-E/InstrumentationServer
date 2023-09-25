package software.coley.instrument.message.request;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.reply.ReplyThreadsMessage;

/**
 * Message to request a thread dump.
 *
 * @author Matt Coley
 * @see ReplyThreadsMessage
 */
public class RequestThreadsMessage extends AbstractRequestMessage<ReplyThreadsMessage> {
	public static final StructureCodec<RequestThreadsMessage> CODEC =
			CommonCodecs.emptyMessage(RequestThreadsMessage::new);
}
