package software.coley.instrument.data;

import software.coley.instrument.io.codec.CommonCodecs;
import software.coley.instrument.io.codec.StructureCodec;

import java.util.Arrays;
import java.util.List;

/**
 * Thread data wrapper.
 *
 * @author Matt Coley
 */
public class ThreadData {
	public static final StructureCodec<StackTraceElement> TRACE_CODEC =
			StructureCodec.compose(input -> new StackTraceElement(input.readUTF(), input.readUTF(), input.readUTF(),
							input.readInt()),
					(output, value) -> {
						String fileName = value.getFileName();
						output.writeUTF(value.getClassName());
						output.writeUTF(value.getMethodName());
						output.writeUTF(fileName == null ? "" : fileName);
						output.writeInt(value.getLineNumber());
					});
	public static final StructureCodec<ThreadData> CODEC =
			StructureCodec.compose(input -> new ThreadData(input.readLong(), input.readUTF(), input.readUTF(),
							CommonCodecs.arrayList(TRACE_CODEC).decode(input)),
					(output, value) -> {
						output.writeLong(value.getId());
						output.writeUTF(value.getName());
						output.writeUTF(value.getState());
						CommonCodecs.arrayList(TRACE_CODEC).encode(output, value.getTrace());
					});
	private final long id;
	private final String name;
	private final String state;
	private final List<StackTraceElement> trace;

	/**
	 * @param thread
	 * 		Thread to pull data from.
	 */
	public ThreadData(Thread thread) {
		this.id = thread.getId();
		this.name = thread.getName();
		this.state = thread.getState().name();
		this.trace = Arrays.asList(thread.getStackTrace());
	}

	/**
	 * @param id
	 * 		Thread ID.
	 * @param name
	 * 		Thread name.
	 * @param state
	 * 		Thread state.
	 * @param trace
	 * 		Thread stack trace.
	 */
	public ThreadData(long id, String name, String state, List<StackTraceElement> trace) {
		this.id = id;
		this.name = name;
		this.state = state;
		this.trace = trace;
	}

	/**
	 * @return Thread ID.
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return Thread name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Thread state.
	 */
	public String getState() {
		return state;
	}

	/**
	 * @return Thread stack trace.
	 */
	public List<StackTraceElement> getTrace() {
		return trace;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ThreadData other = (ThreadData) o;
		if (id != other.id) return false;
		if (!name.equals(other.name)) return false;
		if (!state.equals(other.state)) return false;
		return trace.equals(other.trace);
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + name.hashCode();
		result = 31 * result + state.hashCode();
		result = 31 * result + trace.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ThreadData{" +
				"id=" + id +
				", name='" + name + '\'' +
				", state='" + state + '\'' +
				", trace=" + trace +
				'}';
	}
}
