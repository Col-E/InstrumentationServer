package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.ClientListener;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.ByteGen;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles transferring system properties.
 *
 * @author Matt Coley
 */
public class PropertiesCommand extends AbstractCommand {
	private String properties;

	public PropertiesCommand() {
		super(ID_CL_REQUEST_PROPERTIES);
	}

	@Override
	public void handleClient(Client client) {
		ClientListener listener = client.getListener();
		if (listener != null) {
			if (properties == null)
				throw new IllegalStateException("Properties string not set before usage!");
			Map<String, String> propertiesMap = new HashMap<>();
			String[] lines = properties.split("\n");
			for (String line : lines) {
				int index = line.indexOf('=');
				if (index > 0) {
					String key = line.substring(0, index);
					String value = line.substring(index + 1);
					propertiesMap.put(key, value);
				}
			}
			listener.onReceiveProperties(propertiesMap);
		}
	}

	@Override
	public void handleServer(Server server) throws IOException {
		StringBuilder sb = new StringBuilder();
		System.getProperties().forEach((key, value) -> sb.append(key).append('=').append(value).append('\n'));
		properties = sb.toString();
		server.getLink().send(this);
	}

	@Override
	public byte[] generate() {
		if (properties == null)
			throw new IllegalStateException("Properties string not set before usage!");
		return new ByteGen()
				.appendByte(key())
				.appendString(properties)
				.build();
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		properties = in.readUTF();
	}

	@Override
	public String toString() {
		if (properties == null)
			return "PropertiesCommand[empty]";
		return "PropertiesCommand[" + properties.replace("\n", ", ") + "]";
	}
}
