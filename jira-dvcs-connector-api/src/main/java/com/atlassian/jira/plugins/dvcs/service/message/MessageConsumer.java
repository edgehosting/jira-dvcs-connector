package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Message;

/**
 * Consumes a received message.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <K>
 *            type of key
 * @param <P>
 *            type of payload
 */
public interface MessageConsumer<P extends HasProgress>
{

    /**
     * @return Identity of consumer resp. consumer queue.
     */
    String getId();

    /**
     * Called when a payload.
     * 
     * @param message
     *            identity of message
     */
    void onReceive(Message<P> message);

    /**
     * @return key of messages which will be received
     */
    MessageKey<P> getKey();

    boolean shouldDiscard(int messageId, int retryCount, P payload, String[] tags);

    void afterDiscard(int messageId, int retryCount, P payload, String[] tags);

}
