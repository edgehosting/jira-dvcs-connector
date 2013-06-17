package com.atlassian.jira.plugins.dvcs.service.message;

/**
 * Serializer/Unserializer of provided message payload.
 * 
 * @author Stanislav Dvorscak
 * 
 * @param <P>
 *            type of message
 */
public interface MessageSerializer<P>
{

    /**
     * @param payload
     *            of message
     * @return serialized message, necessary for persisting
     */
    byte[] serialize(P payload);

    /**
     * @param payload
     *            serialized version of message payload
     * @return deserialized message
     */
    P deserialize(byte[] payload);

    /**
     * @return type of payload
     */
    Class<P> getPayloadType();

}
