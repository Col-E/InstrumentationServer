package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.ByteGen;

import java.io.DataInputStream;
import java.io.IOException;

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

	@Override
	public void handleClient(Client client) {
		// no-op
	}

	@Override
	public void handleServer(Server server) {
		if (key == null || value == null)
			throw new IllegalStateException("Key or value not set before usage");
		System.getProperties().put(key, value);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		key = in.readUTF();
		value = in.readUTF();
	}

	@Override
	public byte[] generate() {
		if (key == null || value == null)
			throw new IllegalStateException("Key or value not set before usage");
		return new ByteGen()
				.appendByte(key())
				.appendString(key)
				.appendString(value)
				.build();
	}

	@Override
	public String toString() {
		return "SetPropertyCommand[" +
				"key='" + key + '\'' +
				", value='" + value + '\'' +
				']';
	}
}
