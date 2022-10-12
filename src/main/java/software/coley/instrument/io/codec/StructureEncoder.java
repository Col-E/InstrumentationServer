package software.coley.instrument.io.codec;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Encoder of a structure 'T'.
 *
 * @author xxDark
 */
public interface StructureEncoder<T> {
	/**
	 * @param output
	 * 		Output.
	 * @param value
	 * 		Value to encode.
	 *
	 * @throws IOException
	 * 		When the value cannot be written to the output.
	 */
	void encode(DataOutput output, T value) throws IOException;

	/**
	 * @param decoder
	 * 		Decoder to apply.
	 *
	 * @return Completed codec for 'T'.
	 */
	default StructureCodec<T> with(StructureDecoder<T> decoder) {
		return StructureCodec.compose(decoder, this);
	}

	static <T> StructureEncoder<T> encoder(StructureEncoder<T> encoder) {
		return encoder;
	}
}
