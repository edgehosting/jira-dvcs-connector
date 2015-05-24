package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Message;

/**
 * Serializer/Unserializer of provided message payload.
 *
 * @author Stanislav Dvorscak
 *
 * @param <P>
 *            type of message
 */
public interface MessagePayloadSerializer<P extends HasProgress>
{

    /**
     * @param payload
     *            of message
     * @return serialized message, necessary for persisting
     */
    String serialize(P payload);

    /**
     * Deserialize the message
     * @param message serialized version of message payload
     * @return deserialized message
     */
    P deserialize(Message<P> message);

    /**
     * @return type of payload
     */
    Class<P> getPayloadType();

}
