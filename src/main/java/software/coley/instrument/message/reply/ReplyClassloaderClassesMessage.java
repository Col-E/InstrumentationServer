package software.coley.instrument.message.reply;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.request.RequestClassloaderClassesMessage;

import java.util.Collection;

/**
 * Message response of all classes belonging to a classloader.
 *
 * @author Matt Coley
 * @see RequestClassloaderClassesMessage
 */
public class ReplyClassloaderClassesMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplyClassloaderClassesMessage> CODEC =
			StructureCodec.compose(input -> {
						int id = input.readInt();
						Collection<String> classes = CommonCodecs.collection(CommonCodecs.STRING).decode(input);
						return new ReplyClassloaderClassesMessage(id, classes);
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
	public ReplyClassloaderClassesMessage(int loaderId, Collection<String> classes) {
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
