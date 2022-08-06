package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Logger;

import java.io.IOException;

/**
 * Basic ping request.
 *
 * @author Matt Coley
 */
public class PingCommand extends AbstractCommand {
	private static final byte[] DATA = {ID_COMMON_PING};

	public PingCommand() {
		super(ID_COMMON_PING);
	}

	@Override
	public void handleClient(Client client) {
		try {
			client.getLink().send(new PongCommand());
		} catch (IOException ex) {
			Logger.error("Could not reply to ping: " + ex);
		}
	}

	@Override
	public void handleServer(Server server) throws IOException {
		server.getLink().send(new PongCommand());
	}

	@Override
	public byte[] generate() {
		return DATA;
	}
}
