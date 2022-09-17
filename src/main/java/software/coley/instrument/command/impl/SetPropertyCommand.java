package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.ByteGen;

import java.nio.ByteBuffer;

/**
 * Handles setting a property.
 *
 * @author Matt Coley
 */
public class SetPropertyCommand extends AbstractCommand {
	private String key;
	private String value;

	public SetPropertyCommand() {
		super(ID_CL_SET_PROPERTY);
	}

	public SetPropertyCommand(String key, String value) {
		this();
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
	 * @param key
	 * 		Property key.
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return Property value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 * 		Property value.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Assigns the property value.
	 */
	public void assignValue() {
		System.getProperties().put(key, value);
	}

	@Override
	public void read(ByteBuffer in) {
		key = Buffers.getString(in);
		value = Buffers.getString(in);
	}

	@Override
	public byte[] generate() {
		if (key == null || value == null)
			throw new IllegalStateException("Key or value not set before usage");
		return new ByteGen()
				.appendString(key)
				.appendString(value)
				.build((byte) key());
	}

	@Override
	public String toString() {
		return "SetPropertyCommand[" +
				"key='" + key + '\'' +
				", value='" + value + '\'' +
				']';
	}
}
