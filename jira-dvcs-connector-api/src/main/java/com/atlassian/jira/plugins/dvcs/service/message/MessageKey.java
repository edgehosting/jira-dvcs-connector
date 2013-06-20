package com.atlassian.jira.plugins.dvcs.service.message;

/**
 * Represents key of a message.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <P>
 *            type of message payload
 */
public interface MessageKey<P>
{

    /**
     * @return identity of key
     */
    String getId();
    
    /**
     * @return type of payload
     */
    Class<P> getPayloadType();

}
