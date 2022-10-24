package software.coley.instrument.sock;

import software.coley.instrument.message.broadcast.AbstractBroadcastMessage;

/**
 * Listener to receive broadcast messages.
 * These are messages not associated with any request message.
 *
 * @author Matt Coley
 */
public interface BroadcastListener {
	/**
	 * @param message
	 * 		Message content.
	 */
	void onReceive(AbstractBroadcastMessage message);
}