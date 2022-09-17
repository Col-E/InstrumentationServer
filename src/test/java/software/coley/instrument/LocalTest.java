package software.coley.instrument;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.instrument.command.impl.GetFieldCommand;
import software.coley.instrument.command.impl.PingCommand;
import software.coley.instrument.command.impl.PongCommand;
import software.coley.instrument.command.impl.PropertiesCommand;
import software.coley.instrument.util.Logger;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LocalTest {
	@BeforeAll
	public static void setup() {
		// Setup client logging (server will use defaults)
		Logger.level = Logger.DEBUG;
	}

	@Test
	public void test() throws Exception {
		int port = Server.DEFAULT_PORT;

		Server server = new Server(null, port);
		server.acceptAsync(channel -> System.out.println("Connected!"));

		Client client = new Client(port);
		client.connect();

		// Ping-Pong
		for (int i = 0; i < 20; i++) {
			client.sendBlocking(new PingCommand(), reply -> {
				assertTrue(reply instanceof PongCommand);
			}).get();
		}

		// Properties lookup
		client.sendBlocking(new PropertiesCommand(), reply -> {
			Map<String, String> results = ((PropertiesCommand) reply).mapValue();
			assertNotNull(results);
		});

		// Field lookup
		client.sendBlocking(new GetFieldCommand("java/lang/Integer", "MAX_VALUE", "I"), reply -> {
			assertEquals(String.valueOf(Integer.MAX_VALUE), ((GetFieldCommand) reply).getValueText());
		});
	}
}