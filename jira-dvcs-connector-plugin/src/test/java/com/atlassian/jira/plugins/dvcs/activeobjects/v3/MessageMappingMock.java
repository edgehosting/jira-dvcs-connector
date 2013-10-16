package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import org.mockito.Mockito;

/**
 * Mock's support for {@link MessageMapping}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessageMappingMock
{

    /**
     * Only static members.
     */
    private MessageMappingMock()
    {
    }

    /**
     * Creates {@link MessageMapping} mock with provided values.
     * 
     * @param id
     *            {@link MessageMapping#getID()}
     * @param consumers
     *            {@link MessageMapping#getConsumers()}
     * @return created mock
     */
    public static MessageMapping newMessageMapping(int id, MessageConsumerMapping... consumers)
    {
        MessageMapping result = Mockito.mock(MessageMapping.class);
        Mockito.doReturn(id).when(result).getID();
        Mockito.doReturn(new MessageTagMapping[] {}).when(result).getTags();
        Mockito.doReturn(consumers).when(result).getConsumers();
        return result;
    }

}
