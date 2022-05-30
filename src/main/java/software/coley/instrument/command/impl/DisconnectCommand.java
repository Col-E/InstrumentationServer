package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Handles disconnecting.
 *
 * @author Matt Coley
 */
public class DisconnectCommand extends AbstractCommand {
	@Override
	public void handleClient(Client client) {
		// Server shouldn't send this, but if its closing we need to stop as well.
		client.stopInputLoop();
	}

	@Override
	public void handleServer(Server server) {
		// no-op, client will disconnect and stream gets closed
	}

	@Override
	public int key() {
		return ID_COMMON_DISCONNECT;
	}
}
