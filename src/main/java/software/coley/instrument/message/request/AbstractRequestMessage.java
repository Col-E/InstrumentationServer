package software.coley.instrument.message.request;

import software.coley.instrument.message.AbstractMessage;
import software.coley.instrument.message.reply.AbstractReplyMessage;

/**
 * Base type for request messages.
 *
 * @param <R>
 * 		Reply message type.
 *
 * @author Matt Coley
 */
public class AbstractRequestMessage<R extends AbstractReplyMessage> extends AbstractMessage {
}
