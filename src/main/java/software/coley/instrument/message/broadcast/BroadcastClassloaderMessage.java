package software.coley.instrument.message.broadcast;

import software.coley.instrument.data.BasicClassLoaderInfo;
import software.coley.instrument.data.ClassLoaderInfo;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Message to broadcast new {@link ClassLoaderInfo} values.
 *
 * @author Matt Coley
 */
public class BroadcastClassloaderMessage extends AbstractBroadcastMessage {
	public static final StructureCodec<BroadcastClassloaderMessage> CODEC = StructureCodec.compose(
			input -> new BroadcastClassloaderMessage(BasicClassLoaderInfo.CODEC.decode(input)),
			(output, value) -> BasicClassLoaderInfo.CODEC.encode(output, value.getClassLoader()));
	private final ClassLoaderInfo classLoader;

	/**
	 * @param classLoader
	 * 		New classloader info.
	 */
	public BroadcastClassloaderMessage(ClassLoaderInfo classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * @return New classloader info.
	 */
	public ClassLoaderInfo getClassLoader() {
		return classLoader;
	}
}
