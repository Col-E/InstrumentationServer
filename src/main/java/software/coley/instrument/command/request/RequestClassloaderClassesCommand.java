package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to request all classes belonging to a classloader.
 *
 * @author Matt Coley
 */
public class RequestClassloaderClassesCommand extends AbstractCommand {
	public static final StructureCodec<RequestClassloaderClassesCommand> CODEC =
			StructureCodec.compose(input -> new RequestClassloaderClassesCommand(input.readInt()),
					((output, value) -> output.writeInt(value.getLoaderId())));
	private final int loaderId;

	/**
	 * @param loaderId
	 * 		ClassLoader id.
	 */
	public RequestClassloaderClassesCommand(int loaderId) {
		this.loaderId = loaderId;
	}

	/**
	 * @return ClassLoader id.
	 */
	public int getLoaderId() {
		return loaderId;
	}
}
