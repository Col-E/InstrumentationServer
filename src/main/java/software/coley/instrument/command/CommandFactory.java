package software.coley.instrument.command;

import software.coley.instrument.command.impl.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CommandFactory implements CommandConstants {
	private static final Map<Integer, Supplier<AbstractCommand>> COMMAND_SUPPLIERS = new HashMap<>();

	public static AbstractCommand create(int key) {
		Supplier<AbstractCommand> supplier = COMMAND_SUPPLIERS.get(key);
		if (supplier == null)
			return null;
		return supplier.get();
	}

	static {
		COMMAND_SUPPLIERS.put(ID_COMMON_SHUTDOWN, ShutdownCommand::new);
		COMMAND_SUPPLIERS.put(ID_COMMON_DISCONNECT, DisconnectCommand::new);
		COMMAND_SUPPLIERS.put(ID_COMMON_PING, PingCommand::new);
		COMMAND_SUPPLIERS.put(ID_COMMON_PONG, PongCommand::new);
		COMMAND_SUPPLIERS.put(ID_CL_REQUEST_PROPERTIES, PropertiesCommand::new);
		COMMAND_SUPPLIERS.put(ID_CL_SET_PROPERTY, SetPropertyCommand::new);
		COMMAND_SUPPLIERS.put(ID_CL_GET_FIELD, GetFieldCommand::new);
		COMMAND_SUPPLIERS.put(ID_CL_SET_FIELD, SetFieldCommand::new);
		COMMAND_SUPPLIERS.put(ID_CL_LOADED_CLASSES, LoadedClassesCommand::new);
		COMMAND_SUPPLIERS.put(ID_CL_GET_CLASS, GetClassCommand::new);
		COMMAND_SUPPLIERS.put(ID_CL_REDEFINE_CLASS, RedefineClassCommand::new);
	}
}
