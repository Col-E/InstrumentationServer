package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to request a pong reply from the server.
 *
 * @author Matt Coley
 */
public class RequestPingCommand extends AbstractCommand {
	public static final StructureCodec<RequestPingCommand> CODEC =
			CommonCodecs.emptyCommand(RequestPingCommand::new);

	/**
	 * Empty request.
	 */
	public RequestPingCommand() {
	}
}
