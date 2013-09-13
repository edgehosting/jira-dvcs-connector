package com.atlassian.jira.plugins.dvcs.service.message;

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
     * @param messageId
     *            identity of message
     * @param payload
     *            of message
     */
    void onReceive(int messageId, P payload, String [] tags);

    /**
     * @return key of messages which will be received
     */
    MessageKey<P> getKey();
    
    boolean shouldDiscard(int messageId, int retryCount, P payload, String [] tags);
    
    void beforeDiscard(int messageId, int retryCount, P payload, String [] tags);

}
