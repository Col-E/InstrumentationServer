package sun.instrument;

import software.coley.instrument.InstrumentationImplHookTransformer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;

/**
 * Bootstrap-visible bridge invoked from {@link InstrumentationImpl}.
 * @see InstrumentationImplHookTransformer
 */
@SuppressWarnings("unused")
public class InstrumentationHookBridge {
	private static volatile Method callback;
	private static volatile boolean unavailable;

	public static void transformerAdded(ClassFileTransformer transformer, boolean canRetransform) {
		// Skip if we've failed to find the callback method in the past.
		if (unavailable)
			return;

		try {
			// Lazily find the callback method to invoke.
			Method method = callback;
			if (method == null) {
				Class<?> helper = Class.forName("software.coley.instrument.InstrumentationHelper", true,
						ClassLoader.getSystemClassLoader());
				method = helper.getMethod("onTransformerAdded", ClassFileTransformer.class, boolean.class);
				callback = method;
			}
			method.invoke(null, transformer, canRetransform);
		} catch (Throwable ignored) {
			// Welp...
			unavailable = true;
		}
	}
}
