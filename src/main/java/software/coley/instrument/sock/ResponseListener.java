package software.coley.instrument.sock;

import software.coley.instrument.message.AbstractMessage;

/**
 * Listener to handle a response to some request message, indicated by the frame ID.
 *
 * @author Matt Coley
 */
public interface ResponseListener {
	/**
	 * @param frameId
	 * 		Message ID.
	 * @param message
	 * 		Message content.
	 */
	void onReceive(int frameId, AbstractMessage message);
}