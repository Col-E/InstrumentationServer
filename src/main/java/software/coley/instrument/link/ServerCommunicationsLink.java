package software.coley.instrument.link;

import software.coley.instrument.Server;

import java.io.IOException;

/**
 * {@link CommunicationsLink} for {@link Server}s.
 * <br>
 * Allows calling back to the server instance for command handling in the {@link #inputLoop(Server) input loop}.
 *
 * @author Matt Coley
 */
public interface ServerCommunicationsLink extends CommunicationsLink {

	/**
	 * Loops until the other end of the link closes its link with us.
	 *
	 * @param server
	 * 		Server to call back to for actual instrumentation capabilities.
	 *
	 * @throws IOException
	 * 		When the loop encounters an error.
	 */
	void inputLoop(Server server) throws IOException;
}
