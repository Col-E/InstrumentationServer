package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;

/**
 * Handles disconnecting.
 *
 * @author Matt Coley
 */
public class DisconnectCommand extends AbstractCommand {
	private static final byte[] DATA = {ID_COMMON_DISCONNECT};

	public DisconnectCommand() {
		super(ID_COMMON_DISCONNECT);
	}

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
	public byte[] generate() {
		return DATA;
	}
}
