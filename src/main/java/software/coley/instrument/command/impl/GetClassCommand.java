package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.ByteGen;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Handles retrieving a class.
 *
 * @author Matt Coley
 */
public class GetClassCommand extends AbstractCommand {
	private String name;
	private byte[] code;

	public GetClassCommand() {
		super(ID_CL_GET_CLASS);
	}

	public GetClassCommand(String name, byte[] code) {
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

	public void setCode(byte[] code) {
		this.code = code;
	}

	@Override
	public void read(ByteBuffer in) {
		name = Buffers.getString(in);
		code = Buffers.getArray(in);
	}

	@Override
	public byte[] generate() {
		if (name == null)
			throw new IllegalStateException("Name not set before usage");
		return new ByteGen()
				.appendString(name)
				.appendByteArray(code)
				.build((byte) key());
	}

	@Override
	public String toString() {
		return "GetClassCommand{" +
				"name='" + name + '\'' +
				", code=" + Arrays.toString(code) +
				'}';
	}
}
