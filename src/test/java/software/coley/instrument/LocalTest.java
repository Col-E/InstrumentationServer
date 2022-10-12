package software.coley.instrument;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.instrument.command.reply.ReplyFieldGetCommand;
import software.coley.instrument.command.reply.ReplyPingCommand;
import software.coley.instrument.command.reply.ReplyPropertiesCommand;
import software.coley.instrument.command.request.RequestFieldGetCommand;
import software.coley.instrument.command.request.RequestPingCommand;
import software.coley.instrument.command.request.RequestPropertiesCommand;
import software.coley.instrument.data.MemberData;
import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.util.Logger;

import java.net.InetSocketAddress;
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
		Server.open(null, new InetSocketAddress("localhost", port), ByteBufferAllocator.DIRECT);

		Client client = new Client("localhost", Server.DEFAULT_PORT, ByteBufferAllocator.DIRECT);
		assertTrue(client.connect());

		// Ping-Pong
		for (int i = 0; i < 20; i++) {
			client.sendBlocking(new RequestPingCommand(), reply -> {
				assertTrue(reply instanceof ReplyPingCommand);
			});
		}

		// Properties lookup
		client.sendBlocking(new RequestPropertiesCommand(), (ReplyPropertiesCommand reply) -> {
			Map<String, String> results = reply.mapValue();
			assertNotNull(results);
		});

		// Field lookup
		MemberData memberData = new MemberData("java/lang/Integer", "MAX_VALUE", "I");
		client.sendBlocking(new RequestFieldGetCommand(memberData), (ReplyFieldGetCommand reply) -> {
			assertEquals(String.valueOf(Integer.MAX_VALUE), reply.getValueText());
		});
	}
}