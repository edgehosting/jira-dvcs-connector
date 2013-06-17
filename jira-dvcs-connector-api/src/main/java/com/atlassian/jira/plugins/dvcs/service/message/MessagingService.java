package com.atlassian.jira.plugins.dvcs.service.message;

/**
 * Provides services related to messaging.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface MessagingService
{

    /**
     * Publishes a message with provided payload under provided key.
     * 
     * @param key
     *            for publication
     * @param payload
     *            for publication
     */
    <K extends MessageKey<P>, P> void publish(K key, P payload);

}
