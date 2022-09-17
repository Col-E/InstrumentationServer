package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;

/**
 * Basic ping reply.
 *
 * @author Matt Coley
 */
public class PongCommand extends AbstractCommand {
	private static final byte[] DATA = {ID_COMMON_PONG, 0, 0, 0, 0};

	public PongCommand() {
		super(ID_COMMON_PONG);
	}

	@Override
	public byte[] generate() {
		return DATA;
	}
}
