package software.coley.instrument.command.reply;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.request.RequestFieldSetCommand;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to acknowledge a {@link RequestFieldSetCommand} command.
 *
 * @author Matt Coley
 */
public class ReplyFieldSetCommand extends AbstractCommand {
	public static final StructureCodec<ReplyFieldSetCommand> CODEC =
			StructureCodec.compose(input -> new ReplyFieldSetCommand(input.readUTF()),
					(output, value) -> output.writeUTF(value.getMessage()));
	public static final String MESSAGE_SUCCESS = ".";
	private final String message;

	/**
	 * @param message
	 * 		Field-set message.
	 */
	public ReplyFieldSetCommand(String message) {
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
