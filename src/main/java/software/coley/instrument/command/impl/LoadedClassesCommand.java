package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.ClientListener;
import software.coley.instrument.Server;
import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.util.ByteGen;
import software.coley.instrument.util.DescUtil;

import java.io.DataInputStream;
import java.io.IOException;

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

	@Override
	public void handleClient(Client client) {
		if (classNames == null)
			throw new IllegalStateException("Class names not set before usage!");
		ClientListener listener = client.getListener();
		if (listener != null)
			listener.onReceiveLoadedClasses(classNames);
	}

	@Override
	public void handleServer(Server server) throws IOException {
		Class<?>[] allLoadedClasses = server.getInstrumentation().getAllLoadedClasses();
		classNames = new String[allLoadedClasses.length];
		for (int i = 0; i < classNames.length; i++)
			classNames[i] = DescUtil.getDescriptor(allLoadedClasses[i]);
		server.getLink().send(this);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		int count = in.readInt();
		classNames = new String[count];
		for (int i = 0; i < count; i++)
			classNames[i] = in.readUTF();
	}

	@Override
	public byte[] generate() {
		if (classNames == null)
			throw new IllegalStateException("Class names not set before usage!");
		ByteGen gen = new ByteGen()
				.appendByte(key())
				.appendInt(classNames.length);
		for (String className : classNames)
			gen.appendString(className);
		return gen.build();
	}

	@Override
	public String toString() {
		if (classNames == null)
			return "LoadedClassesCommand[empty]";
		return "LoadedClassesCommand[" + classNames.length + "]";
	}
}
