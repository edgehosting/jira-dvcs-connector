package com.atlassian.jira.plugins.dvcs.service.message;

/**
 * Consumes a received message.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <P>
 *            type of payload
 */
public interface MessageConsumer<P>
{

    /**
     * @return Identity of consumer resp. consumer queue.
     */
    String getId();

    /**
     * Called when a payload.
     * 
     * @param payload
     */
    void onReceive(P payload);

    /**
     * @return key of messages which will be received
     */
    MessageKey<P> getKey();

}
