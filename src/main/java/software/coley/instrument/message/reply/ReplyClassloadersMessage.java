package software.coley.instrument.message.reply;

import software.coley.instrument.data.BasicClassLoaderInfo;
import software.coley.instrument.data.ClassLoaderInfo;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

import java.util.Collection;

/**
 * Message to reply to client with {@link software.coley.instrument.data.ClassLoaderInfo} values.
 *
 * @author xxDark
 */
public class ReplyClassloadersMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplyClassloadersMessage> CODEC =
			CommonCodecs.collectionMessage(ReplyClassloadersMessage::new,
					ReplyClassloadersMessage::getClassLoaders,
					CommonCodecs.collection(BasicClassLoaderInfo.CODEC));
	private final Collection<ClassLoaderInfo> classLoaders;

	/**
	 * @param classLoaders
	 * 		Reply value.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ClassLoaderInfo> ReplyClassloadersMessage(Collection<T> classLoaders) {
		this.classLoaders = (Collection<ClassLoaderInfo>) classLoaders;
	}

	/**
	 * @return Classloaders from the server.
	 */
	public Collection<ClassLoaderInfo> getClassLoaders() {
		return classLoaders;
	}
}
