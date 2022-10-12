package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to request {@link software.coley.instrument.data.ClassLoaderInfo} from the server.
 *
 * @author xxDark
 */
public class RequestClassloadersCommand extends AbstractCommand {
	public static final StructureCodec<RequestClassloadersCommand> CODEC =
			CommonCodecs.emptyCommand(RequestClassloadersCommand::new);

	/**
	 * Empty request.
	 */
	public RequestClassloadersCommand() {
	}
}
