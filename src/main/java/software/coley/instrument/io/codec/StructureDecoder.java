package software.coley.instrument.io.codec;

import java.io.DataInput;
import java.io.IOException;

/**
 * Decoder of structure 'T'.
 *
 * @author xxDark
 */
public interface StructureDecoder<T> {
	/**
	 * @param input
	 * 		Input.
	 *
	 * @return Encoded value.
	 *
	 * @throws IOException
	 * 		When the value cannot be read from the input.
	 */
	T decode(DataInput input) throws IOException;

	/**
	 * @param encoder
	 * 		Encoder to apply.
	 *
	 * @return Completed codec for 'T'.
	 */
	default StructureCodec<T> with(StructureEncoder<T> encoder) {
		return StructureCodec.compose(this, encoder);
	}

	static <T> StructureDecoder<T> decoder(StructureDecoder<T> decoder) {
		return decoder;
	}
}
