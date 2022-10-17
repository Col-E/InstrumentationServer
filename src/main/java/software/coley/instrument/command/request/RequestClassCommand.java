package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to request bytecode of a class.
 *
 * @author Matt Coley
 */
public class RequestClassCommand extends AbstractCommand {
	public static final StructureCodec<RequestClassCommand> CODEC =
			StructureCodec.compose(input -> new RequestClassCommand(input.readInt(), input.readUTF()),
					(output, value) -> {
						output.writeInt(value.getLoaderId());
						output.writeUTF(value.getName());
					});
	private final int loaderId;
	private final String name;

	/**
	 * @param loaderId
	 * 		Classloader id.
	 * @param name
	 * 		Class name.
	 */
	public RequestClassCommand(int loaderId, String name) {
		this.loaderId = loaderId;
		this.name = name;
	}

	/**
	 * @return Classloader id.
	 */
	public int getLoaderId() {
		return loaderId;
	}

	/**
	 * @return Class name.
	 */
	public String getName() {
		return name;
	}
}
