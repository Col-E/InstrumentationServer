package software.coley.instrument;

import software.coley.instrument.command.impl.*;
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
	 * @throws IOException
	 * 		When the request cannot be sent.
	 */
	public void requestProperties() throws IOException {
		getLink().send(new PropertiesCommand());
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
	public void requestSetProperty(String key, String value) throws IOException {
		SetPropertyCommand command = new SetPropertyCommand();
		command.setKey(key);
		command.setValue(value);
		getLink().send(command);
	}

	/**
	 * @param owner
	 * 		Declaring class of field.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field desc.
	 * @param value
	 * 		Value to set. See {@link SetFieldCommand} for supported types.
	 *
	 * @throws IOException
	 * 		When the request cannot be sent.
	 */
	public void requestSetStaticField(String owner, String name, String desc, String value) throws IOException {
		SetFieldCommand command = new SetFieldCommand();
		command.setOwner(owner);
		command.setName(name);
		command.setDesc(desc);
		command.setValueText(value);
		getLink().send(command);
	}

	/**
	 * @param owner
	 * 		Declaring class of field.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field desc.
	 *
	 * @throws IOException
	 * 		When the request cannot be sent.
	 */
	public void requestGetStaticField(String owner, String name, String desc) throws IOException {
		GetFieldCommand command = new GetFieldCommand();
		command.setOwner(owner);
		command.setName(name);
		command.setDesc(desc);
		command.setValueText("");
		getLink().send(command);
	}

	/**
	 * @throws IOException
	 * 		When the request cannot be sent.
	 */
	public void requestLoadedClasses() throws IOException {
		getLink().send(new LoadedClassesCommand());
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
