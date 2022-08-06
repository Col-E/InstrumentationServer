package software.coley.instrument.link;

import software.coley.instrument.Entity;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;

import java.io.IOException;

/**
 * Abstraction of communication layer.
 * Allowing a {@link Server} to operate over different communication mediums.
 *
 * @param <T>
 * 		Type of user of the link.
 *
 * @author Matt Coley
 * @see ServerSocketCommunicationsLink Socket implementation.
 */
public interface CommunicationsLink<T extends Entity<?>> {
	/**
	 * Loops until the other end of the link closes its link with us.
	 *
	 * @param entity
	 * 		Entity callback to for handling feedback from the other end.
	 *
	 * @throws IOException
	 * 		When the loop encounters an error.
	 */
	void inputLoop(T entity) throws IOException;

	/**
	 * Sends a message to the other end.
	 *
	 * @param message
	 * 		Contents of message to send.
	 *
	 * @throws IOException
	 * 		When the message could not be sent.
	 */
	void send(AbstractCommand message) throws IOException;

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
