package software.coley.instrument.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Util for handling internal type names.
 *
 * @author Matt Coley
 */
public class DescUtil {
	// Primitives
	public static final String INT_DESC = getDescriptor(Integer.class);
	public static final String BOOL_DESC = getDescriptor(Boolean.class);
	public static final String BYTE_DESC = getDescriptor(Byte.class);
	public static final String CHAR_DESC = getDescriptor(Character.class);
	public static final String SHORT_DESC = getDescriptor(Short.class);
	public static final String DOUBLE_DESC = getDescriptor(Double.class);
	public static final String FLOAT_DESC = getDescriptor(Float.class);
	public static final String LONG_DESC = getDescriptor(Long.class);
	// Common objects
	public static final String STRING_DESC = getDescriptor(String.class);
	public static final String CHAR_SEQUENCE_DESC = getDescriptor(CharSequence.class);
	public static final String STRING_BUILDER_DESC = getDescriptor(StringBuilder.class);
	public static final String STRING_BUFFER_DESC = getDescriptor(StringBuffer.class);
	public static final String FILE_DESC = getDescriptor(File.class);
	public static final String PATH_DESC = getDescriptor(Path.class);

	/**
	 * @param clazz
	 * 		Input class.
	 *
	 * @return Descriptor type.
	 */
	public static String getDescriptor(Class<?> clazz) {
		StringBuilder sb = new StringBuilder();
		appendDescriptor(clazz, sb);
		return sb.toString();
	}

	/**
	 * @param field
	 * 		Input field.
	 *
	 * @return Descriptor of field type.
	 */
	public static String getDescriptor(Field field) {
		return getDescriptor(field.getType());
	}

	/**
	 * @param method
	 * 		Input method.
	 *
	 * @return Descriptor of method type.
	 */
	public static String getDescriptor(Method method) {
		StringBuilder sb = new StringBuilder("(");
		for (Class<?> parameterType : method.getParameterTypes())
			appendDescriptor(parameterType, sb);
		sb.append(")");
		appendDescriptor(method.getReturnType(), sb);
		return sb.toString();
	}

	/**
	 * @param clazz
	 * 		Input primitive class.
	 *
	 * @return Descriptor type.
	 */
	public static char getPrimitiveName(Class<?> clazz) {
		if (clazz == Integer.TYPE)
			return 'I';
		else if (clazz == Void.TYPE)
			return 'V';
		else if (clazz == Boolean.TYPE)
			return 'Z';
		else if (clazz == Byte.TYPE)
			return 'B';
		else if (clazz == Character.TYPE)
			return 'C';
		else if (clazz == Short.TYPE)
			return 'S';
		else if (clazz == Double.TYPE)
			return 'D';
		else if (clazz == Float.TYPE)
			return 'F';
		else if (clazz == Long.TYPE)
			return 'J';
		else
			throw new IllegalStateException("Unsupported primitive class: " + clazz);
	}

	/**
	 * @param type
	 * 		Input primitive class descriptor character
	 *
	 * @return Descriptor type.
	 */
	public static Class<?> getPrimitiveName(char type) {
		switch (type) {
			case 'I':
				return Integer.TYPE;
			case 'V':
				return Void.TYPE;
			case 'Z':
				return Boolean.TYPE;
			case 'B':
				return Byte.TYPE;
			case 'C':
				return Character.TYPE;
			case 'S':
				return Short.TYPE;
			case 'D':
				return Double.TYPE;
			case 'F':
				return Float.TYPE;
			case 'J':
				return Long.TYPE;
			default:
				throw new IllegalStateException("Unsupported primitive class desc: " + type);
		}
	}

	/**
	 * @param type
	 * 		Descriptor character.
	 *
	 * @return {@code true} for primitive indicator.
	 */
	public static boolean isPrimitiveName(char type) {
		switch (type) {
			case 'I':
			case 'V':
			case 'Z':
			case 'B':
			case 'C':
			case 'S':
			case 'D':
			case 'F':
			case 'J':
				return true;
			default:
				return false;
		}
	}

	/**
	 * @param clazz
	 * 		Class type.
	 * @param sb
	 * 		Builder to append to.
	 */
	private static void appendDescriptor(Class<?> clazz, StringBuilder sb) {
		Class<?> currentClass = clazz;
		while (currentClass.isArray()) {
			sb.append('[');
			currentClass = currentClass.getComponentType();
		}
		if (currentClass.isPrimitive()) {
			char descriptor = getPrimitiveName(currentClass);
			sb.append(descriptor);
		} else {
			sb.append('L').append(clazz.getName().replace('.', '/')).append(';');
		}
	}
}
