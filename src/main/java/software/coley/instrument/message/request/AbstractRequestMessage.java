package software.coley.instrument.message.request;

import software.coley.instrument.message.AbstractMessage;
import software.coley.instrument.message.reply.AbstractReplyMessage;

/**
 * @param <R> Reply message type.
 */
public class AbstractRequestMessage<R extends AbstractReplyMessage> extends AbstractMessage {
}
