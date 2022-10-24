package software.coley.instrument.message;

/**
 * Message outline.
 *
 * @author Matt Coley
 */
public abstract class AbstractMessage implements MessageConstants {
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
