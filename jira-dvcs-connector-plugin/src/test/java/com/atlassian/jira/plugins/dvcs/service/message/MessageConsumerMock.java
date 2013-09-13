package com.atlassian.jira.plugins.dvcs.service.message;

import org.mockito.Mockito;

/**
 * Mock's support for {@link MessageConsumer}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessageConsumerMock
{

    /**
     * Only static members.
     */
    private MessageConsumerMock()
    {
    }

    /**
     * Creates {@link MessageConsumer} mock for provided values.
     * 
     * @param id
     *            {@link MessageConsumer#getId()}
     * @param key
     *            {@link MessageConsumer#getKey()}
     * @return created mock
     */
    public static <P extends HasProgress> MessageConsumer<P> newMessageConsumer(String id, MessageKey<P> key)
    {
        @SuppressWarnings("unchecked")
        MessageConsumer<P> result = Mockito.mock(MessageConsumer.class);
        Mockito.doReturn(id).when(result).getId();
        Mockito.doReturn(key).when(result).getKey();
        return result;
    }

}
