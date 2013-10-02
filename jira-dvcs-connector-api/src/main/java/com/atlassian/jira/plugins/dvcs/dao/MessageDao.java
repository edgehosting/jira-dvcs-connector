package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;

/**
 * {@link Message} related mappings.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface MessageDao
{

    /**
     * Saves or updates provided message.
     * 
     * @param message
     *            to save/update
     */
    <P extends HasProgress> void save(Message<P> message);

    /**
     * Deletes provided message.
     * 
     * @param message
     *            to delete
     */
    <P extends HasProgress> void delete(Message<P> message);

    /**
     * Marks message as queued.
     * 
     * @param message
     *            which was queued
     */
    <P extends HasProgress> void markQueued(Message<P> message);

    /**
     * Marks provided message that was successfully consumed by provided {@link MessageConsumer#getId()}.
     * 
     * @param message
     *            for marking
     * @param consumer
     *            for which consumer
     */
    <P extends HasProgress> void markOk(Message<P> message, MessageConsumer<P> consumer);

    /**
     * Marks provided message that was unsuccessfully consumed by provided {@link MessageConsumer#getId()}.
     * 
     * @param messageId
     *            for marking
     * @param consumer
     *            for which consumer
     */
    <P extends HasProgress> void markFail(Message<P> message, MessageConsumer<P> consumer);

    /**
     * @param id
     *            {@link Message#getId()}
     * @return resolved message
     */
    <P extends HasProgress> Message<P> getById(int id);

    /**
     * @param key
     *            {@link Message#getKey()}
     * @param consumerId
     *            {@link MessageConsumer#getId()}
     * @return next message for consuming or null, if queue is already empty
     */
    public <P extends HasProgress> Message<P> getNextMessageForConsuming(String key, String consumerId);

    /**
     * @param key
     *            {@link Message#getKey()}
     * @param tag
     *            {@link Message#getTags()}
     * @return count of messages which are waiting for processing
     */
    public int getMessagesForConsumingCount(String key, String tag);

}
