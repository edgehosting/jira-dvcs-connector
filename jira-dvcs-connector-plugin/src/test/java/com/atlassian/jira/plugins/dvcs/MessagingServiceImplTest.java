package com.atlassian.jira.plugins.dvcs;

import net.java.ao.DBParam;
import net.java.ao.Query;

import org.easymock.Capture;
import org.easymock.classextension.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageConsumerMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageTagMapping;
import com.atlassian.jira.plugins.dvcs.service.MessageRouterImpl;
import com.atlassian.jira.plugins.dvcs.service.MessagingServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageRouter;
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
        public void onReceive(int messageId, Integer payload)
        {
            sum += payload != null ? payload : 0;
            testedObject.ok(this, messageId);
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
     * Mocked {@link MessageConsumerMapping} entity.
     */
    private MessageConsumerMapping messageConsumer;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void before() throws Exception
    {
        activeObjects = EasyMock.createStrictMock(ActiveObjects.class);
        message = EasyMock.createStrictMock(MessageMapping.class);
        messageConsumer = EasyMock.createStrictMock(MessageConsumerMapping.class);

        // expects count invocation - workaround to check that active objects is available
        EasyMock.expect(activeObjects.<Integer> count(MessageMapping.class)).andReturn(0).once();
        EasyMock.expect(
                activeObjects.<MessageMapping, Integer> find((Class<MessageMapping>) EasyMock.anyObject(), (Query) EasyMock.anyObject()))
                .andReturn(new MessageMapping[] {}).once();

        EasyMock.replay(activeObjects);
        MessageRouter messageRouter = new MessageRouterImpl(activeObjects, new MessageConsumer<?>[] { mockMessageConsumer },
                new MessagePayloadSerializer<?>[] { new MockMessagePayloadSerializer() });
        MessagingServiceImpl messagingServiceImpl = new MessagingServiceImpl(messageRouter);
        testedObject = messagingServiceImpl;
        Thread.sleep(1000);
        EasyMock.verify(activeObjects);
        EasyMock.reset(activeObjects);
    }

    /**
     * Unit test of {@link MessagingServiceImpl#publish(MessageKey, Object)}.
     * 
     * @throws Exception
     */
    public void testPublish() throws Exception
    {
        // first message - 1
        Capture<TransactionCallback<Void>> createMessageCallback = new Capture<TransactionCallback<Void>>();

        createMessageInDB(createMessageCallback);
        returnAndProcessMessage(1);
        createMessageInDB(createMessageCallback);
        returnAndProcessMessage(2);
        createMessageInDB(createMessageCallback);
        returnAndProcessMessage(5);

        EasyMock.replay(activeObjects, message, messageConsumer);

        testedObject.publish(MESSAGE_MOCK_KEY, 1);
        createMessageCallback.getValues().get(0).doInTransaction();
        Thread.sleep(1000); // sleep - it is multi-thread - we need to wait until first message is queued & proceed

        testedObject.publish(MESSAGE_MOCK_KEY, 2);
        createMessageCallback.getValues().get(0).doInTransaction();
        Thread.sleep(1000); // sleep - it is multi-thread - we need to wait until second message is queued & proceed

        testedObject.publish(MESSAGE_MOCK_KEY, 5);
        createMessageCallback.getValues().get(0).doInTransaction();
        Thread.sleep(1000); // sleep - it is multi-thread - we need to wait until third message is queued & proceed

        Assert.assertEquals(mockMessageConsumer.getSum(), 8);

        EasyMock.verify(activeObjects, message, messageConsumer);

    }

    /**
     * Creates message in DB.
     * 
     * @param createMessageCallback
     */
    @SuppressWarnings("unchecked")
    private void createMessageInDB(Capture<TransactionCallback<Void>> createMessageCallback)
    {
        EasyMock.expect(activeObjects.executeInTransaction((TransactionCallback<Void>) EasyMock.capture(createMessageCallback))).andReturn(
                null);
        EasyMock.expect(
                activeObjects.<MessageMapping, Integer> create((Class<MessageMapping>) EasyMock.anyObject(),
                        (DBParam) EasyMock.anyObject(), (DBParam) EasyMock.anyObject())).andReturn(message);
        EasyMock.expect(message.getID()).andReturn(1);
        EasyMock.expect(activeObjects.executeInTransaction((TransactionCallback<Void>) EasyMock.anyObject())).andReturn(null);
    }

    /**
     * Returns created message and process it.
     * 
     * @param messagePayload
     */
    @SuppressWarnings("unchecked")
    private void returnAndProcessMessage(Integer messagePayload)
    {
        EasyMock.expect(
                activeObjects.<MessageMapping, Integer> find((Class<MessageMapping>) EasyMock.anyObject(), (Query) EasyMock.anyObject()))
                .andReturn(new MessageMapping[] { message });
        EasyMock.expect(message.getPayload()).andReturn(messagePayload + "");
        EasyMock.expect(message.getTags()).andReturn(new MessageTagMapping[] {});
        EasyMock.expect(message.getConsumers()).andReturn(new MessageConsumerMapping[] { messageConsumer });
        EasyMock.expect(messageConsumer.getConsumer()).andReturn("messageConsumer");
        EasyMock.expect(message.getID()).andReturn(1);

        // next message
        EasyMock.expect(
                activeObjects.<MessageMapping, Integer> find((Class<MessageMapping>) EasyMock.anyObject(), (Query) EasyMock.anyObject()))
                .andReturn(new MessageMapping[] {});

        // mark as ok
        EasyMock.expect(activeObjects.executeInTransaction((TransactionCallback<Void>) EasyMock.anyObject())).andReturn(null);
    }

}
