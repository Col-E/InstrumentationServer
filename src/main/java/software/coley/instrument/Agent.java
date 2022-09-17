package software.coley.instrument;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
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
		if (server == null) {
			// Determine port
			int port = getPort(agentArgs);
			// Create server
			server = new Server(instrumentation, port);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> server.close()));
			// Accept new client
			server.acceptAsync(null);
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
