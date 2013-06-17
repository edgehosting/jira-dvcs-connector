package com.atlassian.jira.plugins.dvcs;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.service.MessagingServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
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
     * Mock key of {@link MockMessageConsumer}.
     */
    private final MessageKey<Integer> MESSAGE_MOCK_KEY = new MessageKey<Integer>()
    {

        /**
         * Serial version id.
         */
        private static final long serialVersionUID = 1L;

    };

    /**
     * Reference to consumer, which counts sum of all received messages (integer).
     */
    private final MockMessageConsumer mockMessageConsumer = new MockMessageConsumer();

    /**
     * Implementation.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private final class MockMessageConsumer implements MessageConsumer<Integer>
    {

        /**
         * @see #getSum()
         */
        private int sum;

        /**
         * @return sum of all received messages
         */
        public int getSum()
        {
            return sum;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(Integer payload)
        {
            sum += payload != null ? payload : 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MessageKey<Integer> getKey()
        {
            return MESSAGE_MOCK_KEY;
        }
    }

    /**
     * Tested object.
     */
    private MessagingService testedObject;

    @BeforeMethod
    public void before()
    {
        testedObject = new MessagingServiceImpl();
        ((MessagingServiceImpl) testedObject).setConsumers(new MessageConsumer<?>[] { mockMessageConsumer });
    }

    /**
     * Unit test of {@link MessagingServiceImpl#publish(MessageKey, Object)}.
     * 
     * @throws Exception
     */
    public void testPublish() throws Exception
    {
        testedObject.publish(MESSAGE_MOCK_KEY, 1);
        testedObject.publish(MESSAGE_MOCK_KEY, 3);
        testedObject.publish(MESSAGE_MOCK_KEY, 5);
        ((MessagingServiceImpl) testedObject).destroy();

        Assert.assertEquals(mockMessageConsumer.getSum(), 9);
    }
}
