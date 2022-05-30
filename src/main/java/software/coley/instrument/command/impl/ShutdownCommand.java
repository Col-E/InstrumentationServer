package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Handles shutdown of entities.
 *
 * @author Matt Coley
 */
public class ShutdownCommand extends AbstractCommand {
	@Override
	public void handleClient(Client client) {
		// Server is closing, we should too.
		client.stopInputLoop();
	}

	@Override
	public void handleServer(Server server) {
		// Client requested we stop.
		server.stopInputLoop();
	}


	@Override
	public int key() {
		return ID_COMMON_SHUTDOWN;
	}
}
