package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Message;

/**
 * Consumes a received message
 * @param <P> type of payload
 */
public interface MessageConsumer<P extends HasProgress>
{
    int THREADS_PER_CONSUMER = Integer.getInteger("dvcs.connector.synchronization.threads_per_consumer", 2);

    /**
     * @return Identity of consumer resp. consumer queue.
     */
    String getQueue();

    /**
     * Called when a payload.
     * 
     * @param message
     *            identity of message
     * @param payload
     *            deserialized payload
     */
    void onReceive(Message<P> message, P payload);

    /**
     * @return address of messages, which will be received
     */
    MessageAddress<P> getAddress();
    
    /**
     * @return Count of parallel threads, which can be used for processing.
     */
    int getParallelThreads();
}
