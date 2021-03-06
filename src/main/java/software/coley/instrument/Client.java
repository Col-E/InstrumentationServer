package software.coley.instrument;

import software.coley.instrument.link.CommunicationsLink;

/**
 * Client which talks to a server over {@link CommunicationsLink} in order to do remote instrumentation work.
 *
 * @author Matt Coley
 */
public class Client extends Entity<CommunicationsLink<Client>> {
	private final CommunicationsLink<Client> link;

	/**
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
}
