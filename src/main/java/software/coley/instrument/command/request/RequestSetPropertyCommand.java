package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command to request a setting a property in the system {@link java.util.Properties}.
 *
 * @author Matt Coley
 */
public class RequestSetPropertyCommand extends AbstractCommand {
	public static final StructureCodec<RequestSetPropertyCommand> CODEC =
			StructureCodec.compose(input -> new RequestSetPropertyCommand(input.readUTF(), input.readUTF()),
					(output, value) -> {
						output.writeUTF(value.getKey());
						output.writeUTF(value.getValue());
					});
	private final String key;
	private final String value;

	/**
	 * @param key
	 * 		Property key.
	 * @param value
	 * 		Property value.
	 */
	public RequestSetPropertyCommand(String key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * @return Property key.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return Property value.
	 */
	public String getValue() {
		return value;
	}
}
