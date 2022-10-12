package software.coley.instrument.command;

/**
 * Command outline.
 *
 * @author Matt Coley
 */
public abstract class AbstractCommand implements CommandConstants {
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
