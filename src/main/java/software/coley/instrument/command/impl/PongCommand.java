package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Logger;

import java.io.IOException;

/**
 * Basic ping reply.
 *
 * @author Matt Coley
 */
public class PongCommand extends AbstractCommand {
	private static final byte[] DATA = {ID_COMMON_PONG};

	public PongCommand() {
		super(ID_COMMON_PONG);
	}

	@Override
	public void handleClient(Client client) {
		// no-op
	}

	@Override
	public void handleServer(Server server) {
		// no-op
	}

	@Override
	public byte[] generate() {
		return DATA;
	}
}
