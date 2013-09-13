package com.atlassian.jira.plugins.dvcs.service.message;

import org.mockito.Mockito;

/**
 * Mock's support for {@link MessageKey}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessageKeyMock
{

    /**
     * Only static members.
     */
    private MessageKeyMock()
    {
    }

    /**
     * Creates {@link MessageKey} mock with provided values.
     * 
     * @param id
     *            {@link MessageKey#getId()}
     * @param payloadType
     *            {@link MessageKey#getPayloadType()}
     * @return created mock
     */
    public static <P extends HasProgress> MessageKey<P> newMessageKey(String id, Class<P> payloadType)
    {
        @SuppressWarnings("unchecked")
        MessageKey<P> result = Mockito.mock(MessageKey.class);
        Mockito.doReturn(id).when(result).getId();
        Mockito.doReturn(payloadType).when(result).getPayloadType();
        return result;
    }

}
