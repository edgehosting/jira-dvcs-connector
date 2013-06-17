package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;

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
final class MessageKeyRouter<K extends MessageKey<P>, P> implements Runnable
{

    /**
     * Thread which realizes routing.
     */
    private final Thread thread;

    /**
     * Messages determined for routing over a {@link #getKey()}.
     */
    private final BlockingQueue<P> messages = new LinkedBlockingQueue<P>();

    /**
     * Consumers listening over a {@link #getKey()}.
     */
    private final List<MessageConsumerRouter<Object>> consumers = new CopyOnWriteArrayList<MessageConsumerRouter<Object>>();

    /**
     * @see #getKey()
     */
    private final MessageKey<?> key;

    /**
     * @see #stop()
     */
    private boolean stop;

    /**
     * Constructor.
     * 
     * @param key
     *            messages routed for this key
     */
    public MessageKeyRouter(MessageKey<?> key)
    {
        this.key = key;

        this.thread = new Thread(this, "Messages consumer @ " + key);
        this.thread.start();
    }

    /**
     * Stops messages routing.
     */
    public void stop()
    {
        stop = true;
        thread.interrupt();

        try
        {
            thread.join();

        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);

        }

        for (MessageConsumerRouter<?> consumer : consumers)
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
    @SuppressWarnings("unchecked")
    public void addConsumer(MessageConsumerRouter<?> consumerMessagesRouter)
    {
        consumers.add((MessageConsumerRouter<Object>) consumerMessagesRouter);
    }

    /**
     * Routes provided message to consumers.
     * 
     * @param message
     *            for publishing
     */
    public void route(P message)
    {
        try
        {
            messages.put(message);
        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
        while (!stop)
        {
            try
            {
                Object message = messages.take();
                for (MessageConsumerRouter<Object> consumer : consumers)
                {
                    consumer.put(message);
                }

            } catch (InterruptedException e)
            {
                if (!stop)
                {
                    throw new RuntimeException(e);

                }

            }
        }
    }
}
