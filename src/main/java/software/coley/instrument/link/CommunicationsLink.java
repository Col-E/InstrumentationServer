package software.coley.instrument.link;

import software.coley.instrument.Server;

import java.io.IOException;

/**
 * Abstraction of communication layer.
 * Allowing a {@link Server} to operate over different communication mediums.
 *
 * @author Matt Coley
 * @see ServerSocketCommunicationsLink Socket implementation.
 */
public interface CommunicationsLink {
	/**
	 * Opens the communication link.
	 *
	 * @throws IOException
	 * 		When opening the link fails.
	 */
	void open() throws IOException;

	/**
	 * Closes the communication link.
	 *
	 * @throws IOException
	 * 		When closing the link fails.
	 */
	void close() throws IOException;
}
