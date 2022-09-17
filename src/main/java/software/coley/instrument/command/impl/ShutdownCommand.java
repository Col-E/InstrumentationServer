package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;

/**
 * Handles shutdown of entities.
 *
 * @author Matt Coley
 */
public class ShutdownCommand extends AbstractCommand {
	private static final byte[] DATA = {ID_COMMON_SHUTDOWN, 0, 0, 0, 0};

	public ShutdownCommand() {
		super(ID_COMMON_SHUTDOWN);
	}

	@Override
	public byte[] generate() {
		return DATA;
	}
}
