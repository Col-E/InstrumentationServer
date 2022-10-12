package software.coley.instrument.command.reply;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.data.BasicClassLoaderInfo;
import software.coley.instrument.data.ClassLoaderInfo;
import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

import java.util.Collection;

/**
 * Command to reply to client with {@link software.coley.instrument.data.ClassLoaderInfo} values.
 *
 * @author xxDark
 */
public class ReplyClassloadersCommand extends AbstractCommand {
	public static final StructureCodec<ReplyClassloadersCommand> CODEC =
			CommonCodecs.collectionCommand(ReplyClassloadersCommand::new,
					ReplyClassloadersCommand::getClassLoaders,
					CommonCodecs.collection(BasicClassLoaderInfo.CODEC));
	private final Collection<ClassLoaderInfo> classLoaders;

	/**
	 * @param classLoaders
	 * 		Reply value.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ClassLoaderInfo> ReplyClassloadersCommand(Collection<T> classLoaders) {
		this.classLoaders = (Collection<ClassLoaderInfo>) classLoaders;
	}

	/**
	 * @return Classloaders from the server.
	 */
	public Collection<ClassLoaderInfo> getClassLoaders() {
		return classLoaders;
	}
}
