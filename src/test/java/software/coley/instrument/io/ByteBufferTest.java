package software.coley.instrument.io;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ByteBufferTest {

	@MethodSource("utf8Text")
	@ParameterizedTest
	public void testUtf8(String text) {
		ByteBufferDataOutput output = new ByteBufferDataOutput(ByteBufferAllocator.HEAP);
		output.writeUTF(text);
		ByteBuffer buffer = output.consume();
		assertEquals(0, buffer.position());
		ByteBufferDataInput input = new ByteBufferDataInput(buffer);
		assertEquals(text, input.readUTF());
		assertFalse(buffer.hasRemaining());
	}

	private static List<String> utf8Text() {
		return Arrays.asList(
				"Hello, World!",
				repeat('A', 512),
				repeat('B', 1321)
		);
	}

	private static String repeat(char c, int times) {
		char[] buf = new char[times];
		Arrays.fill(buf, c);
		return new String(buf);
	}
}
