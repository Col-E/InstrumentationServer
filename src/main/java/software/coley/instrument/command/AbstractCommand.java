package software.coley.instrument.command;

import software.coley.instrument.Client;
import software.coley.instrument.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class AbstractCommand implements CommandConstants {
	public abstract void handleClient(Client client);

	public abstract void handleServer(Server server);

	public abstract int key();

	public void read(DataInputStream out) throws IOException {
		// no-op by default
	}

	public void write(DataOutputStream out) throws IOException {
		// no-op by default
	}
}
