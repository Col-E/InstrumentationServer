package software.coley.instrument.command;

import software.coley.instrument.command.reply.*;
import software.coley.instrument.command.request.*;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.util.Logger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class CommandFactory implements CommandConstants {
	private final Map<Class<?>, CommandInfo> packetTypeMap = new IdentityHashMap<>();
	private final Map<Integer, CommandInfo> packetIdMap = new HashMap<>();

	private CommandFactory() {
		register(ID_REQ_PING, RequestPingCommand.CODEC);
		register(ID_REP_PONG, ReplyPingCommand.CODEC);
		//
		register(ID_REQ_CLASSLOADERS, RequestClassloadersCommand.CODEC);
		register(ID_REP_CLASSLOADERS, ReplyClassloadersCommand.CODEC);
		//
		register(ID_REQ_PROPERTIES, RequestPropertiesCommand.CODEC);
		register(ID_REP_PROPERTIES, ReplyPropertiesCommand.CODEC);
		//
		register(ID_REQ_SET_PROPERTY, RequestSetPropertyCommand.CODEC);
		register(ID_REP_SET_PROPERTY, ReplySetPropertyCommand.CODEC);
		//
		register(ID_REQ_REDEFINE, RequestRedefineCommand.CODEC);
		register(ID_REP_REDEFINE, ReplyRedefineCommand.CODEC);
		//
		register(ID_REQ_GET_CLASS, RequestClassCommand.CODEC);
		register(ID_REP_GET_CLASS, ReplyClassCommand.CODEC);
		//
		register(ID_REQ_CLASSLOADER_CLASSES, RequestClassloaderClassesCommand.CODEC);
		register(ID_REP_CLASSLOADER_CLASSES, ReplyClassloaderClassesCommand.CODEC);
		//
		register(ID_REQ_FIELD_GET, RequestFieldGetCommand.CODEC);
		register(ID_REP_FIELD_GET, ReplyFieldGetCommand.CODEC);
		//
		register(ID_REQ_FIELD_SET, RequestFieldSetCommand.CODEC);
		register(ID_REP_FIELD_SET, ReplyFieldSetCommand.CODEC);
	}

	@SuppressWarnings("unchecked")
	public <T> T decode(DataInput input) throws IOException {
		int id = input.readInt();
		CommandInfo info = packetIdMap.get(id);
		if (info == null)
			throw new IOException("Unknown packet id " + id);
		T decoded = (T) info.codec.decode(input);
		Logger.debug("Decode command[" + id + "] - " + decoded.getClass().getSimpleName());
		return decoded;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void encode(DataOutput output, Object value) throws IOException {
		CommandInfo info = packetTypeMap.get(value.getClass());
		if (info == null)
			throw new IOException("Could not encode value: " + value);
		Logger.debug("Encode command[" + info.id + "] - " + value.getClass().getSimpleName());
		output.writeInt(info.id);
		((StructureCodec) info.codec).encode(output, value);
	}

	private <T> void register(int id, StructureCodec<T> codec, T... typeHint) {
		Class<?> type = typeHint.getClass().getComponentType();
		CommandInfo info = new CommandInfo(codec, id);
		packetTypeMap.put(type, info);
		packetIdMap.put(id, info);
	}

	public static CommandFactory create() {
		return new CommandFactory();
	}

	private static final class CommandInfo {
		final StructureCodec<?> codec;
		final int id;

		CommandInfo(StructureCodec<?> codec, int id) {
			this.codec = codec;
			this.id = id;
		}
	}
}
