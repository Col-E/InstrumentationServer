package software.coley.instrument;

import software.coley.instrument.command.impl.SetPropertyCommand;
import software.coley.instrument.link.ClientSocketCommunicationsLink;
import software.coley.instrument.link.CommunicationsLink;

import java.io.IOException;

/**
 * Client which talks to a server over {@link CommunicationsLink} in order to do remote instrumentation work.
 *
 * @author Matt Coley
 */
public class Client extends Entity<CommunicationsLink<Client>> {
	private final CommunicationsLink<Client> link;
	private ClientListener listener;

	/**
	 * Client on localhost for the {@link Server#DEFAULT_PORT default port}.
	 *
	 * @throws IOException
	 * 		When localhost cannot be connected to over the default port.
	 */
	public Client() throws IOException {
		this(new ClientSocketCommunicationsLink("localhost", Server.DEFAULT_PORT));
	}

	/**
	 * Client over the given link.
	 *
	 * @param link
	 * 		Communications link.
	 */
	public Client(CommunicationsLink<Client> link) {
		this.link = link;
	}

	@Override
	public CommunicationsLink<Client> getLink() {
		return link;
	}

	/**
	 * @param key
	 * 		Property key.
	 * @param value
	 * 		Property value.
	 *
	 * @throws IOException
	 * 		When the request cannot be sent.
	 */
	public void setProperty(String key, String value) throws IOException {
		SetPropertyCommand command = new SetPropertyCommand();
		command.setKey(key);
		command.setValue(value);
		getLink().send(command);
	}

	/**
	 * @return Current listener.
	 */
	public ClientListener getListener() {
		return listener;
	}

	/**
	 * @param listener
	 * 		New listener.
	 */
	public void setListener(ClientListener listener) {
		this.listener = listener;
	}
}
