package software.coley.instrument.command.reply;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to notify {@link software.coley.instrument.command.request.RequestSetPropertyCommand} completion.
 *
 * @author Matt Coley
 */
public class ReplySetPropertyCommand extends AbstractCommand {
	public static final StructureCodec<ReplySetPropertyCommand> CODEC =
			CommonCodecs.emptyCommand(ReplySetPropertyCommand::new);

	/**
	 * Empty reply.
	 */
	public ReplySetPropertyCommand() {
	}
}
