package software.coley.instrument.command;

import software.coley.instrument.Client;
import software.coley.instrument.Server;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class AbstractCommand implements CommandConstants {
	public abstract void handleClient(Client client) throws IOException;

	public abstract void handleServer(Server server) throws IOException;

	public abstract int key();

	public void read(DataInputStream in) throws IOException {
		// no-op by default
	}
}
