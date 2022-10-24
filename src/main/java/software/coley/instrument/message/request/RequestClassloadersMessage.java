package software.coley.instrument.message.request;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.reply.ReplyClassloadersMessage;

/**
 * Message to request {@link software.coley.instrument.data.ClassLoaderInfo} from the server.
 *
 * @author xxDark
 */
public class RequestClassloadersMessage extends AbstractRequestMessage<ReplyClassloadersMessage> {
	public static final StructureCodec<RequestClassloadersMessage> CODEC =
			CommonCodecs.emptyMessage(RequestClassloadersMessage::new);

	/**
	 * Empty request.
	 */
	public RequestClassloadersMessage() {
	}
}
