package software.coley.instrument.sock;

import software.coley.instrument.io.codec.StructureEncoder;
import software.coley.instrument.message.AbstractMessage;

import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for write information.
 *
 * @author Matt Coley
 */
public class WriteResult<T extends AbstractMessage> {
	private final StructureEncoder<T> encoder;
	private final int frameId;
	private final int decoderKey;
	private final T value;
	private volatile CompletableFuture<Void> future;


	public WriteResult(StructureEncoder<T> encoder, int frameId, int decoderKey, T value) {
		this.encoder = encoder;
		this.frameId = frameId;
		this.decoderKey = decoderKey;
		this.value = value;
	}

	/**
	 * @return Future of write completion.
	 */
	public CompletableFuture<Void> getFuture() {
		// For reasons I don't quite understand, the future has to be lazily instantiated.
		// From: https://github.com/Col-E/InstrumentationServer/issues/13
		//  - Making this final and doing inline instantiation in the field triggers
		//    "NoClassDefFoundError: Could not initialize class java.util.concurrent.CompletableFuture"
		//    on newer JDK's (works on 8 - 11, but 14+ yield this error)
		//  - Why? I have no idea. But this fixes that.
		if (future == null) {
			synchronized (WriteResult.class) {
				if (future == null) {
					future = new CompletableFuture<>();
				}
			}
		}
		return future;
	}

	/**
	 * @return Frame ID of write call.
	 */
	public int getFrameId() {
		return frameId;
	}

	/**
	 * @return Message type.
	 */
	public int getDecoderKey() {
		return decoderKey;
	}

	/**
	 * @return Message content to write.
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Writes the message header to the given output.
	 *
	 * @param output
	 * 		Output to write to.
	 *
	 * @throws IOException
	 * 		When the destination cannot be written to.
	 */
	public void writeHeader(DataOutput output) throws IOException {
		output.writeInt(frameId);
		output.writeShort(decoderKey);
		output.writeInt(-1); // Template for length
	}

	/**
	 * Writes the message content to the given output.
	 *
	 * @param output
	 * 		Output to write to.
	 *
	 * @throws IOException
	 * 		When the destination cannot be written to.
	 */
	public void writeTo(DataOutput output) throws IOException {
		encoder.encode(output, value);
	}

	/**
	 * Completes the {@link #getFuture() future}, called by channel handler.
	 */
	public void complete() {
		getFuture().complete(null);
	}
}
