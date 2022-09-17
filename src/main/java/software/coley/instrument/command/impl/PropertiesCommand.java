package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.ByteGen;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles transferring system properties.
 *
 * @author Matt Coley
 */
public class PropertiesCommand extends AbstractCommand {
	private static final char SEPARATOR = '\u0000';
	private String properties = "";

	public PropertiesCommand() {
		super(ID_CL_REQUEST_PROPERTIES);
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

	/**
	 * @return Single string of properties, split by {@link #SEPARATOR}.
	 */
	public String populateValue() {
		StringBuilder sb = new StringBuilder();
		System.getProperties().forEach((key, value) -> sb.append(key).append('=').append(value).append(SEPARATOR));
		properties = sb.toString();
		return properties;
	}

	@Override
	public byte[] generate() {
		if (properties == null)
			throw new IllegalStateException("Properties string not set before usage!");
		return new ByteGen()
				.appendString(properties)
				.build((byte) key());
	}

	@Override
	public void read(ByteBuffer in) {
		properties = Buffers.getString(in);
	}

	@Override
	public String toString() {
		if (properties == null)
			return "PropertiesCommand[empty]";
		int count = properties.length() - properties.replace(String.valueOf(SEPARATOR), "").length();
		return "PropertiesCommand[" + count + "]";
	}
}
