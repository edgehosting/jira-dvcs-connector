package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.cache.memory.MemoryCacheManager;
import com.atlassian.jira.plugins.dvcs.model.DiscardReason;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.MessageState;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A {@link com.atlassian.jira.plugins.dvcs.service.message.MessagingService} mock implementation.
 *
 * @author Miroslav Stencel
 */
public class MessagingServiceImplMock extends MessagingServiceImpl
{
    private final LinkedList<Message> messageQueue = new LinkedList<Message>();
    private final List<Message> running = new ArrayList<Message>();
    private final Multimap<Integer, String> messageTags = LinkedListMultimap.create();
    private int messageIdSequence = 1;

    public MessagingServiceImplMock()
    {
        super(new MemoryCacheManager());
    }

    @Override
    protected <P extends HasProgress> void createMessage(final Message<P> message, final MessageState state, final String... tags)
    {
        int messageId = messageIdSequence++;
        message.setId(messageId);
        messageQueue.offer(message);
        messageTags.putAll(messageId, Arrays.asList(tags));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pause(String tag)
    {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void running(MessageConsumer<P> consumer, Message<P> message)
    {
        running.add(message);
        messageQueue.remove(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void ok(MessageConsumer<P> consumer, Message<P> message)
    {
        messageQueue.remove(message);
        running.remove(message);
        messageTags.removeAll(message.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void fail(MessageConsumer<P> consumer, Message<P> message, Throwable t)
    {
        ok(consumer, message);
    }

    @Override
    public <P extends HasProgress> void discard(final MessageConsumer<P> consumer, final Message<P> message, final DiscardReason discardReason)
    {
        ok(consumer, message);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P extends HasProgress> Message<P> getNextMessageForConsuming(MessageConsumer<P> consumer, String address)
    {
        return messageQueue.peek();
    }

    @Override
    public int getQueuedCount(String tag)
    {
        return messageQueue.size() + running.size();
    }

    @Override
    public void cancel(final String tag)
    {
        messageQueue.clear();
        running.clear();
        messageTags.clear();
    }

    @Override
    public void retry(final String tag, final int auditId)
    {
        // nop
    }
}
