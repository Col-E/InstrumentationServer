package software.coley.instrument.command.reply;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.StructureCodec;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Command to reply with system {@link java.util.Properties}.
 *
 * @author Matt Coley
 */
public class ReplyPropertiesCommand extends AbstractCommand {
	public static final StructureCodec<ReplyPropertiesCommand> CODEC =
			StructureCodec.compose(input -> new ReplyPropertiesCommand(input.readUTF()),
					(output, value) -> output.writeUTF(value.getProperties()));
	private static final char SEPARATOR = '\u0000';
	private final String properties;

	/**
	 * @param properties
	 * 		System properties.
	 */
	public ReplyPropertiesCommand(Properties properties) {
		this(encode(properties));
	}

	/**
	 * @param properties
	 * 		Encoded properties string.
	 */
	public ReplyPropertiesCommand(String properties) {
		this.properties = properties;
	}

	/**
	 * @return Encoded properties string.
	 */
	public String getProperties() {
		return properties;
	}

	/**
	 * @return Map representation of properties.
	 */
	public Map<String, String> mapValue() {
		Map<String, String> propertiesMap = new HashMap<>();
		String[] lines = properties.split(String.valueOf(SEPARATOR));
		for (String line : lines) {
			int index = line.indexOf('=');
			if (index > 0) {
				String key = line.substring(0, index);
				String value = line.substring(index + 1);
				propertiesMap.put(key, value);
			}
		}
		return propertiesMap;
	}

	@Override
	public String toString() {
		if (properties == null)
			return "ReplyPropertiesCommand[empty]";
		int count = properties.length() - properties.replace(String.valueOf(SEPARATOR), "").length();
		return "ReplyPropertiesCommand[" + count + "]";
	}

	private static String encode(Properties properties) {
		StringBuilder sb = new StringBuilder();
		properties.forEach((key, value) -> sb.append(key).append('=').append(value).append(SEPARATOR));
		return sb.toString();
	}
}
