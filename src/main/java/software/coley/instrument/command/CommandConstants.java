package software.coley.instrument.command;

/**
 * Constants for commands.
 *
 * @author Matt Coley
 */
public interface CommandConstants {
	int ID_COMMON_SHUTDOWN = 0;
	int ID_COMMON_DISCONNECT = 1;
	int ID_COMMON_PING = 2;
	int ID_COMMON_PONG = 3;
	//
	int ID_CL_REQUEST_PROPERTIES = 100;
	int ID_CL_SET_PROPERTY = 101;
	int ID_CL_LOADED_CLASSES = 102;
}
