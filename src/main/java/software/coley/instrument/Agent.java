package software.coley.instrument;

import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.message.MessageFactory;
import software.coley.instrument.sock.ChannelHandler;
import software.coley.instrument.util.Logger;

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
		agent(agentArgs == null ? "" : agentArgs, instrumentation);
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
		agent(agentArgs == null ? "" : agentArgs, instrumentation);
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
		// Configure logging if set, defaults to NONE
		if (agentArgs.contains("error")) Logger.level = Logger.ERROR;
		else if (agentArgs.contains("warn")) Logger.level = Logger.WARN;
		else if (agentArgs.contains("info")) Logger.level = Logger.INFO;
		else if (agentArgs.contains("debug")) Logger.level = Logger.DEBUG;

		// Disable nicely named threads to evade simple thread name checks in targeted applications
		if (agentArgs.contains("namelessThreads")) {
			ChannelHandler.threadNameEventHandle = null;
			ChannelHandler.threadNameEventLoop = null;
			ChannelHandler.threadNameRead = null;
			ChannelHandler.threadNameWrite = null;
		}

		// Disable tracking reflection-backing trampoline classes
		if (agentArgs.contains("notrampolines")) {
			InstrumentationHelper.notrampolines = true;
		}

		// Start server
		if (server == null || server.isClosed()) {
			Logger.prefix = "[Server]";
			// Determine port
			int port = getPort(agentArgs);
			// Create server
			server = Server.open(instrumentation,
					new InetSocketAddress("127.0.0.1", port),
					ByteBufferAllocator.HEAP,
					MessageFactory.create());
			Runtime.getRuntime().addShutdownHook(new Thread(() -> server.close()));
		}
	}

	private static int getPort(String agentArgs) {
		if (agentArgs.contains("port=")) {
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
