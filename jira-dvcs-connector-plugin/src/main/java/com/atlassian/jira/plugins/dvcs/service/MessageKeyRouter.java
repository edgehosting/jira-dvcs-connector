package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessageTag;

/**
 * Routes messages to consumers listening on a {@link #getKey()}.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <K>
 *            type of message key
 * @param <P>
 *            type of message payload
 */
final class MessageKeyRouter<K extends MessageKey<P>, P>
{

    /**
     * Consumers listening over a {@link #getKey()}.
     */
    private final List<MessageConsumerRouter<K, P>> consumers = new CopyOnWriteArrayList<MessageConsumerRouter<K, P>>();

    /**
     * @see #getKey()
     */
    private final MessageKey<?> key;

    /**
     * Constructor.
     * 
     * @param key
     *            messages routed for this key
     */
    public MessageKeyRouter(MessageKey<?> key)
    {
        this.key = key;
    }

    /**
     * Stops messages routing.
     */
    public void stop()
    {
        for (MessageConsumerRouter<?, ?> consumer : consumers)
        {
            consumer.stop();
        }
    }

    /**
     * @return over which key is realized routing.
     */
    public MessageKey<?> getKey()
    {
        return key;
    }

    /**
     * Adds routing for {@link #getKey()} and provided message consumer.
     * 
     * @param consumerMessagesRouter
     */
    public void addConsumer(MessageConsumerRouter<K, P> consumerMessagesRouter)
    {
        consumers.add(consumerMessagesRouter);
    }

    /**
     * Routes provided message to consumers.
     * 
     * @param message
     *            for publishing
     */
    public void route(Message<K, P> message)
    {
        for (MessageConsumerRouter<K, P> consumer : consumers)
        {
            consumer.route(message);
        }
    }

    /**
     * @param tag
     *            message discriminator
     * @return Count of queued messages for provided message tag.
     */
    public int getQueuedCount(MessageTag tag)
    {
        int result = 0;
        for (MessageConsumerRouter<K, P> consumer : consumers)
        {
            result = Math.max(result, consumer.getQueuedCount(tag));
        }

        return result;
    }

}
