package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;

/**
 * Handles disconnecting.
 *
 * @author Matt Coley
 */
public class DisconnectCommand extends AbstractCommand {
	private static final byte[] DATA = {ID_COMMON_DISCONNECT, 0, 0, 0, 0};

	public DisconnectCommand() {
		super(ID_COMMON_DISCONNECT);
	}

	@Override
	public byte[] generate() {
		return DATA;
	}
}
