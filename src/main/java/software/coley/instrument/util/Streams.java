package software.coley.instrument.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Stream IO utils.
 *
 * @author Matt Coley
 */
public class Streams {
	/**
	 * @param in
	 * 		Stream to read from.
	 *
	 * @return Output bytes.
	 *
	 * @throws IOException
	 * 		When the stream could not be read from.
	 */
	public static byte[] readStream(InputStream in) throws IOException {
		byte[] buffer = new byte[8192];
		int offset = 0;
		while (true) {
			int r = in.read(buffer, offset, buffer.length - offset);
			if (r == -1) {
				return Arrays.copyOf(buffer, offset);
			}
			if (r == 0) {
				buffer = Arrays.copyOf(buffer, buffer.length + 1024);
			}
			offset += r;
		}
	}
}
