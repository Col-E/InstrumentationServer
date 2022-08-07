package software.coley.instrument.command.impl;

import software.coley.instrument.Client;
import software.coley.instrument.ClientListener;
import software.coley.instrument.Server;
import software.coley.instrument.util.ByteGen;
import software.coley.instrument.util.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static software.coley.instrument.util.DescUtil.getDescriptor;
import static software.coley.instrument.util.DescUtil.isPrimitiveName;

/**
 * Handles getting a static field.
 *
 * @author Matt Coley
 */
public class GetFieldCommand extends AbstractMemberCommand {
	private String valueText;

	public GetFieldCommand() {
		super(ID_CL_GET_FIELD);
	}

	/**
	 * @return Field value as a string.
	 */
	public String getValueText() {
		return valueText;
	}

	/**
	 * @param valueText
	 * 		Field value as a string.
	 */
	public void setValueText(String valueText) {
		this.valueText = valueText;
	}

	@Override
	public void handleClient(Client client) {
		ClientListener listener = client.getListener();
		if (listener != null) {
			if (owner == null || name == null || desc == null || valueText == null)
				throw new IllegalStateException("Field indicators not set before usage");
			listener.onReceiveStaticFieldValue(owner, name, desc, valueText);
		}
	}

	@Override
	public void handleServer(Server server) {
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
			server.getLink().send(this);
		} catch (Exception ex) {
			Logger.error("Failed to set field value: " + ex);
		}
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		owner = in.readUTF();
		name = in.readUTF();
		desc = in.readUTF();
		valueText = in.readUTF();
	}

	@Override
	public byte[] generate() {
		if (owner == null || name == null || desc == null || valueText == null)
			throw new IllegalStateException("Field indicators not set before usage");
		return new ByteGen()
				.appendByte(key())
				.appendString(owner)
				.appendString(name)
				.appendString(desc)
				.appendString(valueText)
				.build();
	}

	@Override
	public String toString() {
		return "GetFieldCommand[" +
				"owner='" + owner + '\'' +
				", name='" + name + '\'' +
				", desc='" + desc + '\'' +
				", valueText='" + valueText + '\'' +
				']';
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
