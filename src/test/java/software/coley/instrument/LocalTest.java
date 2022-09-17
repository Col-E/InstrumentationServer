package software.coley.instrument;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.instrument.command.impl.PingCommand;
import software.coley.instrument.command.impl.PongCommand;
import software.coley.instrument.util.Logger;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalTest {
	@BeforeAll
	public static void setup() {
		// Setup client logging (server will use defaults)
		Logger.level = Logger.DEBUG;
	}

	@Test
	public void testPingPong() throws Exception {
		int port = Server.DEFAULT_PORT;

		Server server = new Server(null, port);
		server.acceptAsync(channel -> System.out.println("Connected!"));

		Client client = new Client(port);
		client.connect();

		for (int i = 0; i < 20; i++) {
			client.sendBlocking(new PingCommand(), reply -> {
				assertTrue(reply instanceof PongCommand);
			}).get();
		}
	}
}