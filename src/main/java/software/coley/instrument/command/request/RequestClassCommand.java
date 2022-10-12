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
			StructureCodec.compose(input -> new RequestClassCommand(input.readUTF()),
					(output, value) -> output.writeUTF(value.getName()));
	private final String name;

	/**
	 * @param name
	 * 		Class name.
	 */
	public RequestClassCommand(String name) {
		this.name = name;
	}

	/**
	 * @return Class name.
	 */
	public String getName() {
		return name;
	}
}
