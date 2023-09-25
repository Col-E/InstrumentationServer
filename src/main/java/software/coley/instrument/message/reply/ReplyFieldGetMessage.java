package software.coley.instrument.message.reply;

import software.coley.instrument.data.MemberData;
import software.coley.instrument.io.codec.StructureCodec;
import software.coley.instrument.message.request.RequestFieldGetMessage;

/**
 * Message response to {@link RequestFieldGetMessage} containing the
 * {@link Object#toString()} of the field value.
 *
 * @author Matt Coley
 * @see RequestFieldGetMessage
 */
public class ReplyFieldGetMessage extends AbstractReplyMessage {
	public static final StructureCodec<ReplyFieldGetMessage> CODEC =
			StructureCodec.compose(input -> new ReplyFieldGetMessage(MemberData.CODEC.decode(input), input.readUTF()),
					((output, value) -> {
						MemberData.CODEC.encode(output, value.getMemberInfo());
						output.writeUTF(value.getValueText());
					}));
	private static final String UNKNOWN_VALUE = "?";
	private final MemberData memberData;
	private final String valueText;

	/**
	 * @param memberData
	 * 		Field member data.
	 * @param valueText
	 * 		Field value as a string.
	 */
	public ReplyFieldGetMessage(MemberData memberData, String valueText) {
		this.memberData = memberData;
		this.valueText = valueText != null ? valueText : UNKNOWN_VALUE;
	}

	/**
	 * @return {@code true} when the value is unknown <i>(lookup failure)</i>
	 */
	public boolean isValueUnknown() {
		return UNKNOWN_VALUE.equals(valueText);
	}

	/**
	 * @return Field member data.
	 */
	public MemberData getMemberInfo() {
		return memberData;
	}

	/**
	 * @return Field value as a string.
	 */
	public String getValueText() {
		return valueText;
	}

	@Override
	public String toString() {
		String owner = memberData.getOwner();
		String name = memberData.getName();
		String desc = memberData.getDesc();
		return "ReplyFieldGetMessage[" +
				"owner='" + owner + '\'' +
				", name='" + name + '\'' +
				", desc='" + desc + '\'' +
				", valueText='" + valueText + '\'' +
				']';
	}
}
