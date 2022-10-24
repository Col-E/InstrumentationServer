package software.coley.instrument;

import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.message.MessageFactory;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent entry point which initializes a {@link Server}.
 *
 * @author Matt Coley
 */
public class Agent {
	private static Server server;

	/**
	 * @param agentArgs
	 * 		Server agent arguments.
	 * @param instrumentation
	 * 		Instrumentation instance.
	 *
	 * @throws Exception
	 * 		When the server could not be initialized.
	 */
	public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
		agent(agentArgs, instrumentation);
	}

	/**
	 * @param agentArgs
	 * 		Server agent arguments.
	 * @param instrumentation
	 * 		Instrumentation instance.
	 *
	 * @throws Exception
	 * 		When the server could not be initialized.
	 */
	public static void agentmain(String agentArgs, Instrumentation instrumentation) throws Exception {
		agent(agentArgs, instrumentation);
	}

	/**
	 * @param agentArgs
	 * 		Server agent arguments.
	 * @param instrumentation
	 * 		Instrumentation instance.
	 *
	 * @throws IOException
	 * 		When the server could not be initialized.
	 */
	private static void agent(String agentArgs, Instrumentation instrumentation) throws IOException {
		if (server == null || server.isClosed()) {
			// Determine port
			int port = getPort(agentArgs);
			// Create server
			server = Server.open(instrumentation,
					new InetSocketAddress("localhost", port),
					ByteBufferAllocator.HEAP,
					MessageFactory.create());
			Runtime.getRuntime().addShutdownHook(new Thread(() -> server.close()));
		}
	}

	private static int getPort(String agentArgs) {
		if (agentArgs != null && agentArgs.contains("port=")) {
			try {
				int startPos = agentArgs.indexOf("port=") + 5;
				Matcher matcher = Pattern.compile("\\d+")
						.matcher(agentArgs);
				if (matcher.find(startPos)) {
					String matched = matcher.group();
					return Integer.parseInt(matched);
				}
			} catch (Exception ex) {
				// ignored
			}
		}
		// Default port
		return Server.DEFAULT_PORT;
	}
}
