package software.coley.instrument.message.reply;

import software.coley.instrument.io.codec.StructureCodec;

/**
 * Message to reply to a redefine request, indicating success or not.
 *
 * @author Matt Coley
 */
public class ReplyRedefineMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplyRedefineMessage> CODEC =
			StructureCodec.compose(input -> new ReplyRedefineMessage(input.readUTF()),
					(output, value) -> output.writeUTF(value.getMessage()));
	public static final String MESSAGE_SUCCESS = ".";
	private final String message;

	/**
	 * @param ex
	 * 		Failure reason.
	 */
	public ReplyRedefineMessage(Exception ex) {
		this(ex.toString());
	}

	/**
	 * @param message
	 * 		Message to reply with.
	 */
	public ReplyRedefineMessage(String message) {
		this.message = message;
	}

	/**
	 * @return {@code true} when redefine was a success.
	 */
	public boolean isSuccess() {
		return MESSAGE_SUCCESS.equals(message);
	}

	/**
	 * @return Redefinition result.
	 */
	public String getMessage() {
		return message;
	}
}
