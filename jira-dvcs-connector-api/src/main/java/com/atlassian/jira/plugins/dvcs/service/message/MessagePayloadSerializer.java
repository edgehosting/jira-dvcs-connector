package com.atlassian.jira.plugins.dvcs.service.message;

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
     * @param payload
     *            serialized version of message payload
     * @return deserialized message
     */
    P deserialize(int messageId, String payload, int repoId);

    /**
     * @return type of payload
     */
    Class<P> getPayloadType();

}
