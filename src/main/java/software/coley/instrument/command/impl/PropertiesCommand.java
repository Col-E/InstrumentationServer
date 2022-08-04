package software.coley.instrument.command.impl;

import software.coley.instrument.util.ByteGen;
import software.coley.instrument.Client;
import software.coley.instrument.ClientListener;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;

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
	private final Map<String, String> properties = new HashMap<>();

	@Override
	public void handleClient(Client client) {
		ClientListener listener = client.getListener();
		if (listener != null)
			listener.onReceiveProperties(properties);
	}

	@Override
	public void handleServer(Server server) throws IOException {
		StringBuilder sb = new StringBuilder();
		System.getProperties().forEach((key, value) -> sb.append(key).append('=').append(value).append('\n'));
		server.getLink().send(new ByteGen().appendString(sb.toString()).build());
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		properties.clear();
		String propertiesRaw = in.readUTF();
		String[] lines = propertiesRaw.split("\n");
		for (String line : lines) {
			int index = line.indexOf('=');
			if (index > 0) {
				String key = line.substring(0, index);
				String value = line.substring(index + 1);
				properties.put(key, value);
			}
		}
	}

	@Override
	public int key() {
		return ID_CL_REQUEST_PROPERTIES;
	}
}
