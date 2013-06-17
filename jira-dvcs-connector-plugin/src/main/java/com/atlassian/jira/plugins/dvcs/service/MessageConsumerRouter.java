package com.atlassian.jira.plugins.dvcs.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;

/**
 * Routes messages to consumer.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <P>
 *            type of message
 */
final class MessageConsumerRouter<P> implements Runnable
{

    /**
     * Each consumer has own thread.
     */
    private final Thread thread;

    /**
     * @see #getDelegate()
     */
    private final MessageConsumer<P> delegate;

    /**
     * @see #stop()
     */
    private boolean stop;

    /**
     * Holds messages determined for this consumer.
     */
    private final BlockingQueue<P> messageQueue = new LinkedBlockingQueue<P>();

    /**
     * Constructor.
     * 
     * @param delegate
     *            {@link #getDelegate()}
     */
    public MessageConsumerRouter(MessageConsumer<P> delegate)
    {
        this.delegate = delegate;

        thread = new Thread(this, delegate.getKey().getClass().getCanonicalName() + "@" + delegate.toString());
        thread.start();
    }

    /**
     * @return Original consumer of messages.
     */
    public MessageConsumer<P> getDelegate()
    {
        return delegate;
    }

    /**
     * @param message
     *            adds message which is determined for this queue
     */
    public void put(P message)
    {
        try
        {
            messageQueue.put(message);

        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Stops message consumers - e.g.: when shutdown process happened.
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
                P message = messageQueue.take();
                delegate.onReceive(message);
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
