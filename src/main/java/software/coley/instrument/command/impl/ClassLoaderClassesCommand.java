package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.Buffers;
import software.coley.instrument.util.ByteGen;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Handles requesting classes from a specific classloader.
 *
 * @author Matt Coley
 */
public class ClassLoaderClassesCommand extends AbstractCommand {
	private int loaderKey;
	private Collection<String> classNames = Collections.emptyList();

	public ClassLoaderClassesCommand() {
		super(ID_CL_CLASSLOADER_LOADED_CLASSES);
	}

	public ClassLoaderClassesCommand(int loaderKey) {
		this();
		setLoaderKey(loaderKey);
	}

	public Collection<String> getClassNames() {
		return classNames;
	}

	public void setClassNames(Collection<String> classNames) {
		this.classNames = classNames;
	}

	public int getLoaderKey() {
		return loaderKey;
	}

	public void setLoaderKey(int loaderKey) {
		this.loaderKey = loaderKey;
	}

	@Override
	public void read(ByteBuffer in) {
		loaderKey = in.getInt();
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
				.appendInt(loaderKey)
				.appendInt(classNames.size());
		for (String className : classNames)
			gen.appendString(className);
		return gen.build((byte) key());
	}

	@Override
	public String toString() {
		if (classNames == null)
			return "ClassLoaderClassesCommand[empty]";
		return "ClassLoaderClassesCommand[" + classNames.size() + "]";
	}
}
