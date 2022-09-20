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
	int ID_CL_SET_FIELD = 102;
	int ID_CL_GET_FIELD = 103;
	int ID_CL_LOADED_CLASSES = 110;
	int ID_CL_GET_CLASS = 111;
	int ID_CL_REDEFINE_CLASS = 112;
	//
	int HEADER_SIZE = 5;
	byte HEADER_PART_DONE = -1;
	byte[] HEADER_DONE = {HEADER_PART_DONE, HEADER_PART_DONE, HEADER_PART_DONE, HEADER_PART_DONE, HEADER_PART_DONE};
	//
	long TIMEOUT_SECONDS = 5;
}
