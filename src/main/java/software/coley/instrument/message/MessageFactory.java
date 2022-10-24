package software.coley.instrument.message;

import software.coley.instrument.message.reply.*;
import software.coley.instrument.message.request.*;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.util.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class MessageFactory implements MessageConstants {
	private final Map<Class<?>, MessageInfo> messageTypeMap = new IdentityHashMap<>();
	private final Map<Integer, MessageInfo> messageIdMap = new HashMap<>();

	private MessageFactory() {
		register(ID_REQ_PING, RequestPingMessage.CODEC);
		register(ID_REP_PONG, ReplyPingMessage.CODEC);
		//
		register(ID_REQ_CLASSLOADERS, RequestClassloadersMessage.CODEC);
		register(ID_REP_CLASSLOADERS, ReplyClassloadersMessage.CODEC);
		//
		register(ID_REQ_PROPERTIES, RequestPropertiesMessage.CODEC);
		register(ID_REP_PROPERTIES, ReplyPropertiesMessage.CODEC);
		//
		register(ID_REQ_SET_PROPERTY, RequestSetPropertyMessage.CODEC);
		register(ID_REP_SET_PROPERTY, ReplySetPropertyMessage.CODEC);
		//
		register(ID_REQ_REDEFINE, RequestRedefineMessage.CODEC);
		register(ID_REP_REDEFINE, ReplyRedefineMessage.CODEC);
		//
		register(ID_REQ_GET_CLASS, RequestClassMessage.CODEC);
		register(ID_REP_GET_CLASS, ReplyClassMessage.CODEC);
		//
		register(ID_REQ_CLASSLOADER_CLASSES, RequestClassloaderClassesMessage.CODEC);
		register(ID_REP_CLASSLOADER_CLASSES, ReplyClassloaderClassesMessage.CODEC);
		//
		register(ID_REQ_FIELD_GET, RequestFieldGetMessage.CODEC);
		register(ID_REP_FIELD_GET, ReplyFieldGetMessage.CODEC);
		//
		register(ID_REQ_FIELD_SET, RequestFieldSetMessage.CODEC);
		register(ID_REP_FIELD_SET, ReplyFieldSetMessage.CODEC);
	}

	@SuppressWarnings("unchecked")
	public <T> T decode(DataInput input) throws IOException {
		int id = input.readInt();
		MessageInfo info = messageIdMap.get(id);
		if (info == null)
			throw new IOException("Unknown message id " + id);
		T decoded = (T) info.codec.decode(input);
		Logger.debug("Decode message[" + id + "] - " + decoded.getClass().getSimpleName());
		return decoded;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void encode(DataOutput output, Object value) throws IOException {
		MessageInfo info = messageTypeMap.get(value.getClass());
		if (info == null)
			throw new IOException("Could not encode value: " + value);
		Logger.debug("Encode message[" + info.id + "] - " + value.getClass().getSimpleName());
		output.writeInt(info.id);
		((StructureCodec) info.codec).encode(output, value);
	}

	private <T> void register(int id, StructureCodec<T> codec, T... typeHint) {
		Class<?> type = typeHint.getClass().getComponentType();
		MessageInfo info = new MessageInfo(codec, id);
		messageTypeMap.put(type, info);
		messageIdMap.put(id, info);
	}

	public static MessageFactory create() {
		return new MessageFactory();
	}

	private static final class MessageInfo {
		final StructureCodec<?> codec;
		final int id;

		MessageInfo(StructureCodec<?> codec, int id) {
			this.codec = codec;
			this.id = id;
		}
	}
}
