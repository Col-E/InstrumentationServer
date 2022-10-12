package software.coley.instrument.io.codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author xxDark
 */
public interface StructureCodec<T> extends StructureDecoder<T>, StructureEncoder<T> {
	static <T> StructureCodec<T> compose(StructureDecoder<T> decoder, StructureEncoder<T> encoder) {
		return new StructureCodec<T>() {
			@Override
			public T decode(DataInput input) throws IOException {
				return decoder.decode(input);
			}

			@Override
			public void encode(DataOutput output, T value) throws IOException {
				encoder.encode(output, value);
			}
		};
	}

	static <T> StructureCodec<T> singleton(T instance) {
		return new StructureCodec<T>() {
			@Override
			public T decode(DataInput input) throws IOException {
				return instance;
			}

			@Override
			public void encode(DataOutput output, T value) throws IOException {
			}
		};
	}
}
