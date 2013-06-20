package com.atlassian.jira.plugins.dvcs;

import org.easymock.classextension.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.service.MessagingServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.sal.api.transaction.TransactionCallback;

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
     * Mock implementation for {@link MockMessageConsumer} payload.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private final class MockMessagePayloadSerializer implements MessagePayloadSerializer<Integer>
    {

        /**
         * {@inheritDoc}
         */
        @Override
        public String serialize(Integer payload)
        {
            return payload.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Integer deserialize(String payload)
        {
            return Integer.parseInt(payload);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Class<Integer> getPayloadType()
        {
            return Integer.class;
        }

    }

    /**
     * Mock key of {@link MockMessageConsumer}.
     */
    private final MessageKey<Integer> MESSAGE_MOCK_KEY = new MessageKey<Integer>()
    {

        @Override
        public String getId()
        {
            return MockMessageConsumer.class.getCanonicalName();
        }

        @Override
        public Class<Integer> getPayloadType()
        {
            return Integer.class;
        }

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
        public String getId()
        {
            return MockMessageConsumer.class.getCanonicalName();
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

    /**
     * Mocked {@link ActiveObjects} dependency.
     */
    private ActiveObjects activeObjects;

    /**
     * Mocked {@link MessageMapping} entity.
     */
    private MessageMapping message;

    /**
     * {@inheritDoc}
     */
    @BeforeMethod
    public void before()
    {
        MessagingServiceImpl messagingServiceImpl = new MessagingServiceImpl();

        testedObject = messagingServiceImpl;
        activeObjects = EasyMock.createStrictMock(ActiveObjects.class);
        message = EasyMock.createStrictMock(MessageMapping.class);

        messagingServiceImpl.setMessagePayloadSerializer(new MessagePayloadSerializer<?>[] { new MockMessagePayloadSerializer() });
        messagingServiceImpl.setActiveObjects(activeObjects);
    }

    /**
     * Unit test of {@link MessagingServiceImpl#publish(MessageKey, Object)}.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testPublish() throws Exception
    {
        // first mock - initialization
        EasyMock.expect(activeObjects.find(MessageMapping.class)).andReturn(new MessageMapping[] {});

        EasyMock.replay(activeObjects, message);
        ((MessagingServiceImpl) testedObject).setConsumers(new MessageConsumer<?>[] { mockMessageConsumer });
        EasyMock.verify(activeObjects, message);

        // messaging
        EasyMock.reset(activeObjects, message);

        // first message - 1
        EasyMock.expect(message.getPayload()).andReturn("1").once();
        EasyMock.expect(message.getTags()).andReturn(new String[] {}).once();
        EasyMock.expect(activeObjects.executeInTransaction((TransactionCallback<Void>) EasyMock.anyObject())).andReturn(null).once();
        EasyMock.expect(activeObjects.find(MessageMapping.class)).andReturn(new MessageMapping[] { message });
        EasyMock.expect(activeObjects.find(MessageMapping.class)).andReturn(new MessageMapping[] {});
        
        // second message - 3
        EasyMock.expect(activeObjects.executeInTransaction((TransactionCallback<Void>) EasyMock.anyObject())).andReturn(null).once();
        EasyMock.expect(activeObjects.find(MessageMapping.class)).andReturn(new MessageMapping[] { message });
        EasyMock.expect(activeObjects.find(MessageMapping.class)).andReturn(new MessageMapping[] {});
        EasyMock.expect(message.getPayload()).andReturn("3").once();
        EasyMock.expect(message.getTags()).andReturn(new String[] {}).once();

        // third message - 5 
        EasyMock.expect(activeObjects.executeInTransaction((TransactionCallback<Void>) EasyMock.anyObject())).andReturn(null).once();
        EasyMock.expect(activeObjects.find(MessageMapping.class)).andReturn(new MessageMapping[] { message });
        EasyMock.expect(activeObjects.find(MessageMapping.class)).andReturn(new MessageMapping[] {});
        EasyMock.expect(message.getPayload()).andReturn("5").once();
        EasyMock.expect(message.getTags()).andReturn(new String[] {}).once();

        EasyMock.replay(activeObjects, message);

        testedObject.publish(MESSAGE_MOCK_KEY, 1);
        Thread.sleep(1); // sleep - it is multi-thread - we need to wait until first message is queued & proceed
        
        testedObject.publish(MESSAGE_MOCK_KEY, 3);
        Thread.sleep(1); // sleep - it is multi-thread - we need to wait until second message is queued & proceed
        
        testedObject.publish(MESSAGE_MOCK_KEY, 5);
        Thread.sleep(1); // sleep - it is multi-thread - we need to wait until third message is queued & proceed
        
        ((MessagingServiceImpl) testedObject).destroy();

        Assert.assertEquals(mockMessageConsumer.getSum(), 9);

        EasyMock.verify(activeObjects, message);

    }
}
