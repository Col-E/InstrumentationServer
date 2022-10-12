package software.coley.instrument.command.reply;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

import java.util.Collection;

/**
 * Command response of all classes belonging to a classloader.
 *
 * @author Matt Coley
 */
public class ReplyClassloaderClassesCommand extends AbstractCommand {
	public static final StructureCodec<ReplyClassloaderClassesCommand> CODEC =
			StructureCodec.compose(input -> {
						int id = input.readInt();
						Collection<String> classes = CommonCodecs.collection(CommonCodecs.STRING).decode(input);
						return new ReplyClassloaderClassesCommand(id, classes);
					},
					((output, value) -> {
						output.writeInt(value.getLoaderId());
						CommonCodecs.collection(CommonCodecs.STRING).encode(output, value.getClasses());
					}));
	private final int loaderId;
	private final Collection<String> classes;

	/**
	 * @param loaderId
	 * 		ClassLoader id.
	 * @param classes
	 * 		Names of classes belonging to the loader.
	 */
	public ReplyClassloaderClassesCommand(int loaderId, Collection<String> classes) {
		this.loaderId = loaderId;
		this.classes = classes;
	}

	/**
	 * @return ClassLoader id.
	 */
	public int getLoaderId() {
		return loaderId;
	}

	/**
	 * @return Names of classes belonging to the loader.
	 */
	public Collection<String> getClasses() {
		return classes;
	}
}
