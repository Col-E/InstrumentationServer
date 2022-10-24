package software.coley.instrument.message.reply;

import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.request.RequestFieldSetMessage;

/**
 * Message to acknowledge a {@link RequestFieldSetMessage} message.
 *
 * @author Matt Coley
 */
public class ReplyFieldSetMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplyFieldSetMessage> CODEC =
			StructureCodec.compose(input -> new ReplyFieldSetMessage(input.readUTF()),
					(output, value) -> output.writeUTF(value.getMessage()));
	public static final String MESSAGE_SUCCESS = ".";
	private final String message;

	/**
	 * @param message
	 * 		Field-set message.
	 */
	public ReplyFieldSetMessage(String message) {
		this.message = message;
	}

	/**
	 * @return {@code true} when field-set was a success.
	 */
	public boolean isSuccess() {
		return MESSAGE_SUCCESS.endsWith(message);
	}

	/**
	 * @return Field-set message.
	 */
	public String getMessage() {
		return message;
	}
}
