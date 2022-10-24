package software.coley.instrument.message;

/**
 * Constants for messages.
 *
 * @author Matt Coley
 */
public interface MessageConstants {
	// request
	int ID_REQ_PING= 100;
	int ID_REQ_PROPERTIES = 101;
	int ID_REQ_SET_PROPERTY = 102;
	int ID_REQ_CLASSLOADERS = 110;
	int ID_REQ_CLASSLOADER_CLASSES = 111;
	int ID_REQ_GET_CLASS = 112;
	int ID_REQ_REDEFINE = 113;
	int ID_REQ_FIELD_GET = 114;
	int ID_REQ_FIELD_SET = 115;
	// reply
	int ID_REP_PONG = 200;
	int ID_REP_PROPERTIES = 201;
	int ID_REP_SET_PROPERTY = 202;
	int ID_REP_CLASSLOADERS = 210;
	int ID_REP_CLASSLOADER_CLASSES = 211;
	int ID_REP_GET_CLASS = 212;
	int ID_REP_REDEFINE = 213;
	int ID_REP_FIELD_GET = 214;
	int ID_REP_FIELD_SET = 215;
	// broadcast
	int ID_BROADCAST_LOADER = 300;
	int ID_BROADCAST_CLASS = 301;
	//
	long TIMEOUT_SECONDS = 5;
}
