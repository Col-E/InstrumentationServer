package software.coley.instrument.util;

/**
 * Util for handling internal type names.
 *
 * @author Matt Coley
 */
public class TypeUtil {
	/**
	 * @param clazz
	 * 		Input class.
	 *
	 * @return Internal type.
	 */
	public static String getName(Class<?> clazz) {
		if (clazz.isPrimitive()) {
			if (clazz == Integer.TYPE)
				return "I";
			else if (clazz == Void.TYPE)
				return "V";
			else if (clazz == Boolean.TYPE)
				return "Z";
			else if (clazz == Byte.TYPE)
				return "B";
			else if (clazz == Character.TYPE)
				return "C";
			else if (clazz == Short.TYPE)
				return "S";
			else if (clazz == Double.TYPE)
				return "D";
			else if (clazz == Float.TYPE)
				return "F";
			else if (clazz == Long.TYPE)
				return "J";
			else
				throw new IllegalStateException("Unsupported primitive class: " + clazz);
		} else {
			StringBuilder sb = new StringBuilder();
			appendDescriptor(clazz, sb);
			return sb.toString();
		}
	}

	private static void appendDescriptor(Class<?> clazz, StringBuilder sb) {
		Class<?> currentClass = clazz;
		while (currentClass.isArray()) {
			sb.append('[');
			currentClass = currentClass.getComponentType();
		}
		if (currentClass.isPrimitive()) {
			char descriptor;
			if (currentClass == Integer.TYPE)
				descriptor = 'I';
			else if (currentClass == Void.TYPE)
				descriptor = 'V';
			else if (currentClass == Boolean.TYPE)
				descriptor = 'Z';
			else if (currentClass == Byte.TYPE)
				descriptor = 'B';
			else if (currentClass == Character.TYPE)
				descriptor = 'C';
			else if (currentClass == Short.TYPE)
				descriptor = 'S';
			else if (currentClass == Double.TYPE)
				descriptor = 'D';
			else if (currentClass == Float.TYPE)
				descriptor = 'F';
			else if (currentClass == Long.TYPE)
				descriptor = 'J';
			else
				throw new IllegalStateException("Unsupported primitive class: " + currentClass);

			sb.append(descriptor);
		} else {
			sb.append('L').append(clazz.getName().replace('.', '/')).append(';');
		}
	}
}
