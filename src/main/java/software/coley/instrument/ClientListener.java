package software.coley.instrument;

import java.lang.instrument.Instrumentation;
import java.util.Map;

/**
 * Listener for a {@link Client} to handle feedback from a {@link Server}.
 *
 * @author Matt Coley
 */
public interface ClientListener {
	/**
	 * @param properties
	 * 		Remote properties map.
	 */
	void onReceiveProperties(Map<String, String> properties);

	/**
	 * @param classNames
	 *        {@link Instrumentation#getAllLoadedClasses()}
	 */
	void onReceiveLoadedClasses(String[] classNames);
}
