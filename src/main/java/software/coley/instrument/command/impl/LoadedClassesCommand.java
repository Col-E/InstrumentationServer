package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.ByteGen;
import software.coley.instrument.util.DescUtil;

import java.lang.instrument.Instrumentation;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Handles requesting classes.
 *
 * @author Matt Coley
 */
public class LoadedClassesCommand extends AbstractCommand {
	private Collection<String> classNames = Collections.emptyList();

	public LoadedClassesCommand() {
		super(ID_CL_LOADED_CLASSES);
	}

	public Collection<String> getClassNames() {
		return classNames;
	}

	public void setClassNames(Collection<String> classNames) {
		this.classNames = classNames;
	}

	@Override
	public void read(ByteBuffer in) {
		int count = in.getInt();
		classNames = new ArrayList<>();
		for (int i = 0; i < count; i++)
			classNames.add(Buffers.getString(in));
	}

	@Override
	public byte[] generate() {
		if (classNames == null)
			throw new IllegalStateException("Class names not set before usage!");
		ByteGen gen = new ByteGen()
				.appendInt(classNames.size());
		// TODO: should find a way to optimize by using common parents or zip compression
		for (String className : classNames)
			gen.appendString(className);
		return gen.build((byte) key());
	}

	@Override
	public String toString() {
		if (classNames == null)
			return "LoadedClassesCommand[empty]";
		return "LoadedClassesCommand[" + classNames.size() + "]";
	}
}
