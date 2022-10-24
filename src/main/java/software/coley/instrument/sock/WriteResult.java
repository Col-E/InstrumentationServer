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
	private final CompletableFuture<Void> future = new CompletableFuture<>();
	private final StructureEncoder<T> encoder;
	private final int frameId;
	private final int decoderKey;
	private final T value;

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
		return future;
	}

	/**
	 * @return Frame ID of write call.
	 */
	public int getFrameId() {
		return frameId;
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
		future.complete(null);
	}
}
