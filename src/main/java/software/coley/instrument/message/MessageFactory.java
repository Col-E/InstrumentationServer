package software.coley.instrument.message;

import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.broadcast.BroadcastClassMessage;
import software.coley.instrument.message.broadcast.BroadcastClassloaderMessage;
import software.coley.instrument.message.reply.*;
import software.coley.instrument.message.request.*;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Factory to support {@link AbstractMessage} serialization.
 *
 * @author Matt Coley
 */
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
		//
		register(ID_REQ_THREADS, RequestThreadsMessage.CODEC);
		register(ID_REP_THREADS, ReplyThreadsMessage.CODEC);
		//
		register(ID_BROADCAST_LOADER, BroadcastClassloaderMessage.CODEC);
		register(ID_BROADCAST_CLASS, BroadcastClassMessage.CODEC);
	}

	/**
	 * @param value
	 * 		Message instance.
	 *
	 * @return Message structure information.
	 */
	public MessageInfo getInfo(Object value) {
		MessageInfo info = messageTypeMap.get(value.getClass());
		if (info == null)
			throw new IllegalStateException("Unsupported value type: " + value.getClass());
		return info;
	}

	/**
	 * @param key
	 * 		Message ID.
	 *
	 * @return Message structure information.
	 */
	public MessageInfo getInfo(int key) {
		MessageInfo info = messageIdMap.get(key);
		if (info == null)
			throw new IllegalStateException("Unsupported value key: " + key);
		return info;
	}

	/**
	 * @param id
	 * 		Message ID.
	 * @param codec
	 * 		Message structure codec for encoding/decoding.
	 * @param typeHint
	 * 		Magic.
	 * @param <T>
	 * 		Message type.
	 */
	public <T extends AbstractMessage> void register(int id, StructureCodec<T> codec, T... typeHint) {
		Class<?> type = typeHint.getClass().getComponentType();
		MessageInfo info = new MessageInfo(codec, id);
		messageTypeMap.put(type, info);
		messageIdMap.put(id, info);
	}

	/**
	 * @return New factory instance.
	 */
	public static MessageFactory create() {
		return new MessageFactory();
	}

	public static final class MessageInfo {
		private final StructureCodec<AbstractMessage> codec;
		private final int id;

		@SuppressWarnings("unchecked")
		<T extends AbstractMessage> MessageInfo(StructureCodec<T> codec, int id) {
			this.codec = (StructureCodec<AbstractMessage>) codec;
			this.id = id;
		}

		@SuppressWarnings("unchecked")
		public <T extends AbstractMessage> StructureCodec<T> getCodec() {
			return (StructureCodec<T>) codec;
		}

		public int getId() {
			return id;
		}
	}
}