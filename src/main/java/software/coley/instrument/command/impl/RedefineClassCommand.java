package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.ByteGen;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Handles redefining a class.
 *
 * @author Matt Coley
 */
public class RedefineClassCommand extends AbstractCommand {
	private String name;
	private byte[] code;

	public RedefineClassCommand() {
		super(ID_CL_REDEFINE_CLASS);
	}

	public RedefineClassCommand(String name, byte[] code) {
		this();
		this.name = name;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public byte[] getCode() {
		return code;
	}

	@Override
	public void read(ByteBuffer in) {
		name = Buffers.getString(in);
		code = Buffers.getArray(in);
	}

	@Override
	public byte[] generate() {
		if (name == null || code == null)
			throw new IllegalStateException("Name or bytecode not set before usage");
		return new ByteGen()
				.appendString(name)
				.appendByteArray(code)
				.build((byte) key());
	}

	@Override
	public String toString() {
		return "RedefineClassCommand{" +
				"name='" + name + '\'' +
				", code=" + Arrays.toString(code) +
				'}';
	}
}
