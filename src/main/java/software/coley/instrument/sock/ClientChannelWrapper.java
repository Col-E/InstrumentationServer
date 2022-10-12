package software.coley.instrument.sock;

import software.coley.instrument.io.ByteBufferAllocator;
import software.coley.instrument.util.Logger;

import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper for {@link software.coley.instrument.Client}'s {@link java.nio.channels.AsynchronousSocketChannel}.
 *
 * @author Matt Coley
 */
public class ClientChannelWrapper extends ChannelWrapper {
	private final AsynchronousSocketChannel channel;
	private final Map<Integer, ResponseListener> listenerMap = new ConcurrentHashMap<>();

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
	 * Add a listener that will be notified when a response with the matching ID is found.
	 *
	 * @param frameId
	 * 		Frame ID to listen to for a response.
	 * @param listener
	 * 		Listener to use.
	 */
	public void setResponseListener(int frameId, ResponseListener listener) {
		listenerMap.put(frameId, listener);
	}

	/**
	 * Handles reading loop for taking in new values from the remote server.
	 */
	private void readLoop() {
		read().thenAccept(readResult -> {
			int frameId = readResult.getFrameId();
			ResponseListener responseListener = listenerMap.remove(frameId);
			if (responseListener != null)
				responseListener.onGet(readResult.getValue());
			readLoop();
		});
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
			readLoop();
			return true;
		} catch (Exception ex) {
			Logger.error("Failed to connect to host: " + address + " - " + ex);
			return false;
		}
	}
}
