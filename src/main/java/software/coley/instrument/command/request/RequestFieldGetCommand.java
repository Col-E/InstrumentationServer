package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.data.MemberInfo;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.util.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static software.coley.instrument.util.DescUtil.getDescriptor;
import static software.coley.instrument.util.DescUtil.isPrimitiveName;

/**
 * Request to get the content of a static field.
 *
 * @author Matt Coley
 */
public class RequestFieldGetCommand extends AbstractCommand {
	public static final StructureCodec<RequestFieldGetCommand> CODEC =
			StructureCodec.compose(input -> new RequestFieldGetCommand(MemberInfo.CODEC.decode(input)),
					((output, value) -> MemberInfo.CODEC.encode(output, value.getMemberInfo())));
	private final MemberInfo memberInfo;

	/**
	 * @param memberInfo
	 * 		Field member info.
	 */
	public RequestFieldGetCommand(MemberInfo memberInfo) {
		this.memberInfo = memberInfo;
	}

	/**
	 * @return Field member info.
	 */
	public MemberInfo getMemberInfo() {
		return memberInfo;
	}


	/**
	 * @return Text representation of field value.
	 * {@code null} when cannot be found.
	 */
	public String lookupValue() {
		String owner = memberInfo.getOwner();
		String name = memberInfo.getName();
		String desc = memberInfo.getDesc();
		String valueText = null;
		if (owner == null || name == null || desc == null)
			throw new IllegalStateException("Field indicators not set before usage");
		try {
			Class<?> clazz = Class.forName(owner.replace('/', '.'));
			for (Field declaredField : clazz.getDeclaredFields()) {
				if ((declaredField.getModifiers() & Modifier.STATIC) > 0 &&
						declaredField.getName().equals(name) &&
						desc.equals(getDescriptor(declaredField))) {
					// TODO: Handle setting even in locked down cases using unsafe?
					declaredField.setAccessible(true);
					char first = desc.charAt(0);
					if (isPrimitiveName(first)) {
						switch (first) {
							case 'I':
								valueText = String.valueOf(declaredField.getInt(null));
								break;
							case 'Z':
								valueText = String.valueOf(declaredField.getBoolean(null));
								break;
							case 'B':
								valueText = String.valueOf(declaredField.getByte(null));
								break;
							case 'C':
								valueText = String.valueOf(declaredField.getChar(null));
								break;
							case 'S':
								valueText = String.valueOf(declaredField.getShort(null));
								break;
							case 'D':
								valueText = String.valueOf(declaredField.getDouble(null));
								break;
							case 'F':
								valueText = String.valueOf(declaredField.getFloat(null));
								break;
							case 'J':
								valueText = String.valueOf(declaredField.getLong(null));
								break;
							default:
								throw new IllegalStateException("Unsupported primitive class desc: " + first);
						}
					} else {
						valueText = mapObjectValue(declaredField.get(null));
					}
					break;
				}
			}
			return valueText;
		} catch (Exception ex) {
			Logger.error("Failed to set field value: " + ex);
			return null;
		}
	}

	private String mapObjectValue(Object value) {
		if (value == null)
			return "null";
		Class<?> type = value.getClass();
		if (type.isArray())
			return arrayToString(value);
		return value.toString();
	}

	private static String arrayToString(Object value) {
		if (value == null)
			return "null";
		if (value instanceof int[]) {
			return Arrays.toString((int[]) value);
		} else if (value instanceof boolean[]) {
			return Arrays.toString((boolean[]) value);
		} else if (value instanceof byte[]) {
			return Arrays.toString((byte[]) value);
		} else if (value instanceof char[]) {
			return Arrays.toString((char[]) value);
		} else if (value instanceof short[]) {
			return Arrays.toString((short[]) value);
		} else if (value instanceof double[]) {
			return Arrays.toString((double[]) value);
		} else if (value instanceof float[]) {
			return Arrays.toString((float[]) value);
		} else if (value instanceof long[]) {
			return Arrays.toString((long[]) value);
		}
		return Arrays.toString((Object[]) value);
	}
}
