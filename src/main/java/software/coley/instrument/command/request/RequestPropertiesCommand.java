package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to request system {@link java.util.Properties} from the server.
 *
 * @author Matt Coley
 */
public class RequestPropertiesCommand extends AbstractCommand {
	public static final StructureCodec<RequestPropertiesCommand> CODEC =
			CommonCodecs.emptyCommand(RequestPropertiesCommand::new);

	/**
	 * Empty request.
	 */
	public RequestPropertiesCommand() {
	}
}
