package software.coley.instrument.command.request;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.data.MemberInfo;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.util.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;

import static software.coley.instrument.util.DescUtil.*;

/**
 * Command that handles setting a static field.
 *
 * @author Matt Coley
 */
public class RequestFieldSetCommand extends AbstractCommand {
	public static final StructureCodec<RequestFieldSetCommand> CODEC =
			StructureCodec.compose(input -> new RequestFieldSetCommand(MemberInfo.CODEC.decode(input), input.readUTF()),
					((output, value) -> {
						MemberInfo.CODEC.encode(output, value.getMemberInfo());
						output.writeUTF(value.getValueText());
					}));
	private final MemberInfo memberInfo;
	private final String valueText;

	/**
	 * @param memberInfo
	 * 		Field member info.
	 * @param valueText
	 * 		Field value as a string.
	 */
	public RequestFieldSetCommand(MemberInfo memberInfo, String valueText) {
		this.memberInfo = memberInfo;
		this.valueText = valueText;
	}

	/**
	 * @return Field member info.
	 */
	public MemberInfo getMemberInfo() {
		return memberInfo;
	}

	/**
	 * @return Field value as a string.
	 */
	public String getValueText() {
		return valueText;
	}

	/**
	 * Lookup the field and assign the value.
	 */
	public void assignValue() {
		String owner = memberInfo.getOwner();
		String name = memberInfo.getName();
		String desc = memberInfo.getDesc();
		if (owner == null || name == null || desc == null || valueText == null)
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
								declaredField.setInt(null, mapInt());
								break;
							case 'Z':
								declaredField.setBoolean(null, mapBoolean());
								break;
							case 'B':
								declaredField.setByte(null, mapByte());
								break;
							case 'C':
								declaredField.setChar(null, mapChar());
								break;
							case 'S':
								declaredField.setShort(null, mapShort());
								break;
							case 'D':
								declaredField.setDouble(null, mapDouble());
								break;
							case 'F':
								declaredField.setFloat(null, mapFloat());
								break;
							case 'J':
								declaredField.setLong(null, mapLong());
								break;
							default:
								throw new IllegalStateException("Unsupported primitive class desc: " + first);
						}
					} else {
						declaredField.set(null, mapObjectValue());
					}
				}
			}
		} catch (Exception ex) {
			Logger.error("Failed to set field value: " + ex);
		}
	}

	@Override
	public String toString() {
		String owner = memberInfo.getOwner();
		String name = memberInfo.getName();
		String desc = memberInfo.getDesc();
		return "RequestFieldSetCommand[" +
				"owner='" + owner + '\'' +
				", name='" + name + '\'' +
				", desc='" + desc + '\'' +
				", valueText='" + valueText + '\'' +
				']';
	}

	private int mapInt() {
		if (valueText.toLowerCase().startsWith("0x"))
			return Integer.parseInt(valueText, 16);
		else if (valueText.toLowerCase().startsWith("0b"))
			return Integer.parseInt(valueText, 1);
		return Integer.parseInt(valueText);
	}

	private boolean mapBoolean() {
		return Boolean.parseBoolean(valueText);
	}

	private byte mapByte() {
		if (valueText.toLowerCase().startsWith("0x"))
			return Byte.parseByte(valueText, 16);
		else if (valueText.toLowerCase().startsWith("0b"))
			return Byte.parseByte(valueText, 1);
		return Byte.parseByte(valueText);
	}

	private char mapChar() {
		return valueText.charAt(0);
	}

	private short mapShort() {
		if (valueText.toLowerCase().startsWith("0x"))
			return Short.parseShort(valueText, 16);
		else if (valueText.toLowerCase().startsWith("0b"))
			return Short.parseShort(valueText, 1);
		return Short.parseShort(valueText);
	}

	private double mapDouble() {
		String text = valueText.toLowerCase();
		if (text.endsWith("d"))
			text = text.substring(0, text.length() - 1);
		return Double.parseDouble(text);
	}

	private float mapFloat() {
		String text = valueText.toLowerCase();
		if (text.endsWith("f"))
			text = text.substring(0, text.length() - 1);
		return Float.parseFloat(text);
	}

	private long mapLong() {
		String text = valueText.toLowerCase();
		if (text.endsWith("l"))
			text = text.substring(0, text.length() - 1);
		if (text.startsWith("0x"))
			return Long.parseLong(text, 16);
		else if (text.startsWith("0b"))
			return Long.parseLong(text, 1);
		return Long.parseLong(text);
	}

	private Object mapObjectValue() {
		if (valueText.equals("null"))
			return null;
		// Type implementations
		String desc = memberInfo.getDesc();
		if (desc.equals(INT_DESC))
			return mapInt();
		else if (desc.equals(BOOL_DESC))
			return mapBoolean();
		else if (desc.equals(BYTE_DESC))
			return mapByte();
		else if (desc.equals(CHAR_DESC))
			return mapChar();
		else if (desc.equals(SHORT_DESC))
			return mapShort();
		else if (desc.equals(DOUBLE_DESC))
			return mapDouble();
		else if (desc.equals(FLOAT_DESC))
			return mapFloat();
		else if (desc.equals(LONG_DESC))
			return mapLong();
		else if (desc.equals(STRING_DESC) || desc.equals(CHAR_SEQUENCE_DESC))
			return valueText;
		else if (desc.equals(STRING_BUILDER_DESC))
			return new StringBuilder(valueText);
		else if (desc.equals(STRING_BUFFER_DESC))
			return new StringBuffer(valueText);
		else if (desc.equals(FILE_DESC))
			return new File(valueText);
		else if (desc.equals(PATH_DESC))
			return Paths.get(valueText);
		// Unsupported
		throw new IllegalStateException("Unsupported field type: " + desc);
	}
}
