package software.coley.instrument.command.reply;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.data.ClassData;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to request bytecode of a class.
 *
 * @author Matt Coley
 */
public class ReplyClassCommand extends AbstractCommand {
	public static final StructureCodec<ReplyClassCommand> CODEC =
			StructureCodec.compose(input -> {
						boolean hasData = input.readBoolean();
						if (hasData) {
							return new ReplyClassCommand(ClassData.CODEC.decode(input));
						} else {
							return new ReplyClassCommand();
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
	public ReplyClassCommand() {
		this.data = null;
		this.hasInfo = false;
	}

	/**
	 * @param data
	 * 		Found class data.
	 */
	public ReplyClassCommand(ClassData data) {
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
