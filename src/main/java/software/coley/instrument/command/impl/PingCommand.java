package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;

/**
 * Basic ping request.
 *
 * @author Matt Coley
 */
public class PingCommand extends AbstractCommand {
	private static final byte[] DATA = {ID_COMMON_PING, 0, 0, 0, 0};

	public PingCommand() {
		super(ID_COMMON_PING);
	}

	@Override
	public byte[] generate() {
		return DATA;
	}
}
