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
	private final boolean hasInfo;
	private final ClassData data;

	/**
	 * Failure to find class info.
	 */
	public ReplyClassMessage() {
		this.data = null;
		this.hasInfo = false;
	}

	/**
	 * @param data
	 * 		Found class data.
	 */
	public ReplyClassMessage(ClassData data) {
		this.data = data;
		this.hasInfo = data != null;
	}

	/**
	 * @return {@code true} when {@link #getData()} has a value.
	 */
	public boolean hasData() {
		return hasInfo;
	}

	/**
	 * @return Class info.
	 */
	public ClassData getData() {
		return data;
	}
}
