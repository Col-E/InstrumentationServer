package software.coley.instrument.sock;

import software.coley.instrument.message.AbstractMessage;

/**
 * Listener to handle all all write operations.
 *
 * @author Matt Coley
 */
public interface WriteListener {
	/**
	 * @param frameId
	 * 		Message ID.
	 * @param message
	 * 		Message content.
	 */
	void onWrite(int frameId, AbstractMessage message);
}
