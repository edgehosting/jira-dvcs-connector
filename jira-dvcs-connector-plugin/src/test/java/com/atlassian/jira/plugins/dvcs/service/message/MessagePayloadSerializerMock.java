package com.atlassian.jira.plugins.dvcs.service.message;

import org.mockito.Mockito;

/**
 * Mock's support for {@link MessagePayloadSerializer}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessagePayloadSerializerMock
{

    /**
     * Only static members.
     */
    private MessagePayloadSerializerMock()
    {
    }

    /**
     * Creates one value mock serializer for provided type and appropriate value.
     * 
     * @param payloadType
     *            {@link MessagePayloadSerializer#getPayloadType()}
     * @param payloadValue
     *            {@link MessagePayloadSerializer#deserialize(String)}
     * @return created mock's serializer
     */
    public static <P extends HasProgress> MessagePayloadSerializer<P> newMessagePayloadSerializer(Class<P> payloadType, P payloadValue)
    {
        @SuppressWarnings("unchecked")
        MessagePayloadSerializer<P> result = Mockito.mock(MessagePayloadSerializer.class);
        Mockito.doReturn(payloadType).when(result).getPayloadType();
        Mockito.doReturn(payloadValue).when(result).deserialize(Mockito.anyString());
        return result;
    }

}
