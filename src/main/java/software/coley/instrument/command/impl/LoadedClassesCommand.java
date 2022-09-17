package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.ByteGen;
import software.coley.instrument.util.DescUtil;

import java.lang.instrument.Instrumentation;
import java.nio.ByteBuffer;

/**
 * Handles requesting classes.
 *
 * @author Matt Coley
 */
public class LoadedClassesCommand extends AbstractCommand {
	private String[] classNames = new String[0];

	public LoadedClassesCommand() {
		super(ID_CL_LOADED_CLASSES);
	}

	public String[] getClassNames() {
		return classNames;
	}

	public String[] lookupNames(Instrumentation instrumentation) {
		Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
		classNames = new String[allLoadedClasses.length];
		for (int i = 0; i < classNames.length; i++)
			classNames[i] = DescUtil.getDescriptor(allLoadedClasses[i]);
		return classNames;
	}

	@Override
	public void read(ByteBuffer in) {
		int count = in.getInt();
		classNames = new String[count];
		for (int i = 0; i < count; i++)
			classNames[i] = Buffers.getString(in);
	}

	@Override
	public byte[] generate() {
		if (classNames == null)
			throw new IllegalStateException("Class names not set before usage!");
		ByteGen gen = new ByteGen()
				.appendInt(classNames.length);
		for (String className : classNames)
			gen.appendString(className);
		return gen.build((byte) key());
	}

	@Override
	public String toString() {
		if (classNames == null)
			return "LoadedClassesCommand[empty]";
		return "LoadedClassesCommand[" + classNames.length + "]";
	}
}
