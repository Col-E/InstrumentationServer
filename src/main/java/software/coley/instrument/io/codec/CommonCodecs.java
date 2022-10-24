package software.coley.instrument.io.codec;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Common codec implementations and utilities.
 *
 * @author xxDark
 */
@SuppressWarnings("StatementWithEmptyBody")
public final class CommonCodecs {
	public static final StructureCodec<long[]> LONG_ARRAY = StructureDecoder.decoder(input -> {
		int len = input.readInt();
		long[] arr = new long[len];
		for (int i = 0; i < len; arr[i++] = input.readLong()) ;
		return arr;
	}).with((output, value) -> {
		output.writeInt(value.length);
		for (int i = 0, j = value.length; i < j; output.writeLong(value[i++])) ;
	});
	public static final StructureCodec<double[]> DOUBLE_ARRAY = StructureDecoder.decoder(input -> {
		int len = input.readInt();
		double[] arr = new double[len];
		for (int i = 0; i < len; arr[i++] = input.readDouble()) ;
		return arr;
	}).with((output, value) -> {
		output.writeInt(value.length);
		for (int i = 0, j = value.length; i < j; output.writeDouble(value[i++])) ;
	});
	public static final StructureCodec<int[]> INT_ARRAY = StructureDecoder.decoder(input -> {
		int len = input.readInt();
		int[] arr = new int[len];
		for (int i = 0; i < len; arr[i++] = input.readInt()) ;
		return arr;
	}).with((output, value) -> {
		output.writeInt(value.length);
		for (int i = 0, j = value.length; i < j; output.writeInt(value[i++])) ;
	});
	public static final StructureCodec<byte[]> BYTE_ARRAY = StructureDecoder.decoder(input -> {
		int len = input.readInt();
		byte[] arr = new byte[len];
		for (int i = 0; i < len; arr[i++] = input.readByte()) ;
		return arr;
	}).with((output, value) -> {
		output.writeInt(value.length);
		for (int i = 0, j = value.length; i < j; output.writeByte(value[i++])) ;
	});
	public static final StructureCodec<float[]> FLOAT_ARRAY = StructureDecoder.decoder(input -> {
		int len = input.readInt();
		float[] arr = new float[len];
		for (int i = 0; i < len; arr[i++] = input.readFloat()) ;
		return arr;
	}).with((output, value) -> {
		output.writeInt(value.length);
		for (int i = 0, j = value.length; i < j; output.writeFloat(value[i++])) ;
	});
	public static final StructureCodec<UUID> UUID = StructureDecoder.decoder(input -> new UUID(input.readLong(), input.readLong())).with((output, value) -> {
		output.writeLong(value.getMostSignificantBits());
		output.writeLong(value.getLeastSignificantBits());
	});
	public static final StructureCodec<String> STRING = StructureDecoder.decoder(DataInput::readUTF)
			.with(DataOutput::writeUTF);

	private CommonCodecs() {
	}

	/**
	 * @param valueCodec
	 * 		Codec for the value type.
	 * @param allocator
	 * 		Collection allocator.
	 * @param <T>
	 * 		Collection value type.
	 * @param <C>
	 * 		Collection type.
	 *
	 * @return Codec for creating the given collection type.
	 */
	public static <T, C extends Collection<T>> StructureCodec<C> collection(StructureCodec<T> valueCodec,
																			IntFunction<? extends C> allocator) {
		return StructureDecoder.decoder(input -> {
			int size = input.readInt();
			C c = allocator.apply(size);
			while (size-- != 0) {
				c.add(valueCodec.decode(input));
			}
			return c;
		}).with((output, value) -> {
			output.writeInt(value.size());
			for (T v : value) {
				valueCodec.encode(output, v);
			}
		});
	}

	/**
	 * @param valueCodec
	 * 		Codec for a value type.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Codec for creating a {@link ArrayList} of the value type.
	 */
	public static <T> StructureCodec<List<T>> arrayList(StructureCodec<T> valueCodec) {
		return collection(valueCodec, ArrayList::new);
	}

	/**
	 * @param valueCodec
	 * 		Codec for a value type.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Codec for creating a {@link HashSet} of the value type.
	 */
	public static <T> StructureCodec<Set<T>> hashSet(StructureCodec<T> valueCodec) {
		return collection(valueCodec, HashSet::new);
	}

	/**
	 * @param valueCodec
	 * 		Codec for a value type.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Codec for creating a {@link LinkedHashSet} of the value type.
	 */
	public static <T> StructureCodec<Set<T>> linkedHashSet(StructureCodec<T> valueCodec) {
		return collection(valueCodec, LinkedHashSet::new);
	}

	/**
	 * @param valueCodec
	 * 		Codec for a value type.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Codec for creating a {@link TreeSet} of the value type.
	 */
	public static <T extends Comparable<T>> StructureCodec<SortedSet<T>> treeSet(StructureCodec<T> valueCodec) {
		return collection(valueCodec, __ -> new TreeSet<>());
	}

	/**
	 * @param valueCodec
	 * 		Codec for a value type.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Codec for creating a collection <i>(list)</i> of the value type.
	 */
	public static <T> StructureCodec<Collection<T>> collection(StructureCodec<T> valueCodec) {
		return collection(valueCodec, ArrayList::new);
	}

	/**
	 * @param constructor
	 * 		Message implementation constructor.
	 * @param <T>
	 * 		Message type.
	 *
	 * @return Codec for creating an empty message type.
	 */
	public static <T> StructureCodec<T> emptyMessage(Supplier<T> constructor) {
		return StructureCodec.compose(input -> constructor.get(), (output, value) -> {});
	}

	/**
	 * @param constructor
	 * 		Message type constructor taking in a collection.
	 * @param collectionGetter
	 * 		Message type getter for the collection.
	 * @param listCodec
	 * 		Collection codec.
	 * @param <T>
	 * 		Message type
	 * @param <C>
	 * 		Message collection value type.
	 *
	 * @return Codec for creating a message with only a collection property.
	 */
	public static <T, C> StructureCodec<T> collectionMessage(Function<Collection<C>, T> constructor,
															 Function<T, Collection<C>> collectionGetter,
															 StructureCodec<Collection<C>> listCodec) {
		// decode 'input' into new T(collection<C>)
		// encode 'output' to encode(collection<C>)
		return StructureCodec.compose(input -> constructor.apply(listCodec.decode(input)),
				(output, value) -> listCodec.encode(output, collectionGetter.apply(value)));
	}
}
