package software.coley.instrument.message.request;

import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.reply.ReplySetPropertyMessage;

/**
 * Message to request a setting a property in the system {@link java.util.Properties}.
 *
 * @author Matt Coley
 * @see ReplySetPropertyMessage
 */
public class RequestSetPropertyMessage extends AbstractRequestMessage<ReplySetPropertyMessage> {
	public static final StructureCodec<RequestSetPropertyMessage> CODEC =
			StructureCodec.compose(input -> new RequestSetPropertyMessage(input.readUTF(), input.readUTF()),
					(output, value) -> {
						output.writeUTF(value.getKey());
						output.writeUTF(value.getValue());
					});
	private final String key;
	private final String value;

	/**
	 * @param key
	 * 		Property key.
	 * @param value
	 * 		Property value.
	 */
	public RequestSetPropertyMessage(String key, String value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * @return Property key.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return Property value.
	 */
	public String getValue() {
		return value;
	}
}
