package software.coley.instrument.command.reply;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to reply to a ping with a pong.
 *
 * @author Matt Coley
 */
public class ReplyPingCommand extends AbstractCommand {
	public static final StructureCodec<ReplyPingCommand> CODEC =
			CommonCodecs.emptyCommand(ReplyPingCommand::new);

	/**
	 * Empty reply.
	 */
	public ReplyPingCommand() {
	}
}
