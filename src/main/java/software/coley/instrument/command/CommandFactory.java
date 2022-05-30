package software.coley.instrument.command;

import software.coley.instrument.command.impl.ShutdownCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CommandFactory implements CommandConstants {
	private static final Map<Integer, Supplier<AbstractCommand>> COMMAND_SUPPLIERS = new HashMap<>();

	public static AbstractCommand create(int key) {
		Supplier<AbstractCommand> supplier = COMMAND_SUPPLIERS.get(key);
		if (supplier == null) {
			throw new IllegalStateException("Unknown command key: " + key);
		}
		return supplier.get();
	}

	static {
		COMMAND_SUPPLIERS.put(ID_COMMON_SHUTDOWN, ShutdownCommand::new);
		COMMAND_SUPPLIERS.put(ID_COMMON_DISCONNECT, ShutdownCommand::new);
	}
}
