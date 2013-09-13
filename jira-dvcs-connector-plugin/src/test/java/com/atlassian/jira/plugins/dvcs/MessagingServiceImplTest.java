package com.atlassian.jira.plugins.dvcs;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.service.MessagingServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessageRouter;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;

/**
 * Unit tests over {@link MessagingServiceImpl}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Test
public class MessagingServiceImplTest
{

    /**
     * Target tested object.
     */
    private MessagingService testedObject;

    /**
     * {@link MessageKey} mock.
     */
    @Mock
    private MessageKey<?> key;

    /**
     * {@link MessageRouter} mock.
     */
    @Mock
    private MessageRouter messageRouter;

    /**
     * Prepares test environment.
     */
    @BeforeMethod
    public void before() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        testedObject = new MessagingServiceImpl();
        ReflectionTestUtils.setField(testedObject, "messageRouter", messageRouter);
    }

    /**
     * Unit test of {@link MessagingServiceImpl#publish(MessageKey, Object)}.
     * 
     * @throws Exception
     */
    @Test
    public void testPublish() throws Exception
    {
        testedObject.publish(key, null);
        Mockito.verify(messageRouter).publish(key, null);
    }

}
