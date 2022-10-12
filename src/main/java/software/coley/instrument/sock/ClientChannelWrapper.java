package software.coley.instrument.sock;

import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.util.Logger;

import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * Wrapper for {@link software.coley.instrument.Client}'s {@link java.nio.channels.AsynchronousSocketChannel}.
 *
 * @author Matt Coley
 */
public class ClientChannelWrapper extends ChannelWrapper {
	private final AsynchronousSocketChannel channel;

	/**
	 * @param channel
	 * 		Wrapped channel.
	 * @param allocator
	 * 		Allocator for creating messages.
	 */
	public ClientChannelWrapper(AsynchronousSocketChannel channel, ByteBufferAllocator allocator) {
		super(channel, allocator);
		this.channel = channel;
	}

	/**
	 * @param address
	 * 		The target address to connect to.
	 *
	 * @return {@code true} on successful connect.
	 */
	public boolean connect(SocketAddress address) {
		try {
			channel.connect(address).get();
			return true;
		} catch (Exception ex) {
			Logger.error("Failed to connect to host: " + address + " - " + ex);
			return false;
		}
	}
}
