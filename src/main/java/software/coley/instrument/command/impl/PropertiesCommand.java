package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class PropertiesCommand extends AbstractCommand {

	@Override
	public void handleClient(Client client) {
		// TODO: read from server
	}

	@Override
	public void handleServer(Server server) {
		// TODO: Package up
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		System.getProperties().list(pw);

	}

	@Override
	public int key() {
		return ID_CL_REQUEST_PROPERTIES;
	}
}
