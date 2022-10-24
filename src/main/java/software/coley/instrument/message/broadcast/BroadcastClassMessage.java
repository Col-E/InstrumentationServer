package software.coley.instrument.message.broadcast;

import software.coley.instrument.data.ClassData;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Message to broadcast new {@link ClassData} values.
 *
 * @author Matt Coley
 */
public class BroadcastClassMessage extends AbstractBroadcastMessage {
	public static final StructureCodec<BroadcastClassMessage> CODEC = StructureCodec.compose(
			input -> new BroadcastClassMessage(ClassData.CODEC.decode(input)),
			(output, value) -> ClassData.CODEC.encode(output, value.getData()));
	private final ClassData data;

	/**
	 * @param data
	 * 		Updated class info.
	 */
	public BroadcastClassMessage(ClassData data) {
		this.data = data;
	}

	/**
	 * @return Updated class info.
	 */
	public ClassData getData() {
		return data;
	}
}
