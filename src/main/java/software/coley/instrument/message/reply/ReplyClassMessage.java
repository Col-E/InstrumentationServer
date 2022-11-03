package software.coley.instrument.message.reply;

import software.coley.instrument.data.ClassData;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Message to request bytecode of a class.
 *
 * @author Matt Coley
 */
public class ReplyClassMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplyClassMessage> CODEC =
			StructureCodec.compose(input -> {
						boolean hasData = input.readBoolean();
						if (hasData) {
							return new ReplyClassMessage(ClassData.CODEC.decode(input));
						} else {
							return new ReplyClassMessage();
						}
					},
					(output, value) -> {
						if (value.hasData()) {
							output.writeBoolean(true);
							ClassData.CODEC.encode(output, value.getData());
						} else {
							output.writeBoolean(false);
						}
					});
	private final ClassData data;

	/**
	 * Failure to find class info.
	 */
	public ReplyClassMessage() {
		this.data = null;
	}

	/**
	 * @param data
	 * 		Found class data.
	 */
	public ReplyClassMessage(ClassData data) {
		this.data = data;
	}

	/**
	 * @return {@code true} when {@link #getData()} has a value.
	 */
	public boolean hasData() {
		return data != null && data.hasCode();
	}

	/**
	 * @return Class info.
	 */
	public ClassData getData() {
		return data;
	}
}
