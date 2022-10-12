package software.coley.instrument.command.reply;

import software.coley.instrument.command.AbstractCommand;
import software.coley.instrument.command.request.RequestFieldGetCommand;
import software.coley.instrument.data.MemberInfo;
import software.coley.instrument.io.codec.StructureCodec;

/**
 * Command response to {@link RequestFieldGetCommand} containing the
 * {@link Object#toString()} of the field value.
 *
 * @author Matt Coley
 */
public class ReplyFieldGetCommand extends AbstractCommand {
	public static final StructureCodec<ReplyFieldGetCommand> CODEC =
			StructureCodec.compose(input -> new ReplyFieldGetCommand(MemberInfo.CODEC.decode(input), input.readUTF()),
					((output, value) -> {
						MemberInfo.CODEC.encode(output, value.getMemberInfo());
						output.writeUTF(value.getValueText());
					}));
	private static final String UNKNOWN_VALUE = "?";
	private final MemberInfo memberInfo;
	private final String valueText;

	/**
	 * @param memberInfo
	 * 		Field member info.
	 * @param valueText
	 * 		Field value as a string.
	 */
	public ReplyFieldGetCommand(MemberInfo memberInfo, String valueText) {
		this.memberInfo = memberInfo;
		this.valueText = valueText != null ? valueText : UNKNOWN_VALUE;
	}

	/**
	 * @return {@code true} when the value is unknown <i>(lookup failure)</i>
	 */
	public boolean isValueUnknown() {
		return UNKNOWN_VALUE.equals(valueText);
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

	@Override
	public String toString() {
		String owner = memberInfo.getOwner();
		String name = memberInfo.getName();
		String desc = memberInfo.getDesc();
		return "ReplyFieldGetCommand[" +
				"owner='" + owner + '\'' +
				", name='" + name + '\'' +
				", desc='" + desc + '\'' +
				", valueText='" + valueText + '\'' +
				']';
	}
}
