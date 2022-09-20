package software.coley.instrument.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[10240];
		int len;
		while ((len = in.read(buffer)) != -1)
			baos.write(buffer, 0, len);
		return baos.toByteArray();
	}
}
