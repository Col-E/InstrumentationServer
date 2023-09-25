package software.coley.instrument.message.reply;

import software.coley.instrument.data.ThreadData;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.request.RequestThreadsMessage;

import java.util.List;

/**
 * Message to notify {@link software.coley.instrument.message.request.RequestThreadsMessage} completion.
 *
 * @author Matt Coley
 * @see RequestThreadsMessage
 */
public class ReplyThreadsMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplyThreadsMessage> CODEC =
			StructureCodec.compose(input -> new ReplyThreadsMessage(CommonCodecs.arrayList(ThreadData.CODEC).decode(input)),
					(output, value) -> CommonCodecs.arrayList(ThreadData.CODEC).encode(output, value.getThreads()));

	private final List<ThreadData> threads;

	/**
	 * @param threads
	 * 		Thread info.
	 */
	public ReplyThreadsMessage(List<ThreadData> threads) {
		this.threads = threads;
	}

	/**
	 * @return Thread info.
	 */
	public List<ThreadData> getThreads() {
		return threads;
	}
}
