package com.atlassian.jira.plugins.dvcs;

import net.java.ao.DBParam;
import net.java.ao.Query;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.ActiveObjectsMock;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMappingMock;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.service.MessageRouterImpl;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumerMock;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKeyMock;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializerMock;
import com.atlassian.jira.plugins.dvcs.service.message.MessageRouter;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * {@link MessageRouter} related tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class MessageRouterImplTest
{

    /**
     * Target entity for purposes of testing.
     */
    private MessageRouter testedObject;

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    @Mock
    private ActiveObjects activeObjects;

    /**
     * Expected data by test => data for mocks.
     * 
     * @author Stanislav Dvorscak
     * 
     * @param <P>
     *            type of payload
     */
    private static final class ExpectedData<P extends HasProgress>
    {

        final P payloadValue;

        final String consumerId;
        final String keyId;
        final int messageId;

        final MessageKey<P> key;
        final MessagePayloadSerializer<P> serializer;
        final MessageConsumer<P> consumer;
        final MessageMapping message;

        public ExpectedData(int id, Class<P> payloadType, P payloadValue)
        {
            this.consumerId = "msg_consumer_id_" + id;
            this.keyId = "msg_key_id_" + id;
            this.messageId = id;

            this.payloadValue = payloadValue;

            this.key = MessageKeyMock.newMessageKey(this.keyId, payloadType);
            this.serializer = MessagePayloadSerializerMock.newMessagePayloadSerializer(payloadType, this.payloadValue);
            this.consumer = MessageConsumerMock.newMessageConsumer(this.consumerId, this.key);
            this.message = MessageMappingMock.newMessageMapping(this.messageId);
        }

    }

    /**
     * Prepares tests environment.
     */
    @BeforeMethod
    public void before()
    {
        MockitoAnnotations.initMocks(this);

        // dummy callback call for transaction wrappers
        Mockito.doAnswer(new Answer<Object>()
        {

            @SuppressWarnings("unchecked")
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return ((TransactionCallback<Object>) invocation.getArguments()[0]).doInTransaction();
            }

        }).when(activeObjects).executeInTransaction(Mockito.<TransactionCallback<Object>> any());

        // start point of test => there are no messages for processing
        Mockito.doReturn(new MessageMapping[] {}).when(activeObjects).find(Mockito.eq(MessageMapping.class), Mockito.<Query> any());

    }

    /**
     * Unit test of {@link MessageRouter#publish(MessageKey, Object, String...)}.
     */
    @Test
    public void testPublish() throws Exception
    {
        abstract class BaseHasProgress implements HasProgress {
            public Progress getProgress()
            {
                return null;
            }
        }
        class PayloadType1 extends BaseHasProgress {}
        class PayloadType2 extends BaseHasProgress {}

        final ExpectedData<PayloadType1> expectedDataForConsumer1 = new ExpectedData<PayloadType1>(1, PayloadType1.class,
                new PayloadType1());
        final ExpectedData<PayloadType2> expectedDataForConsumer2 = new ExpectedData<PayloadType2>(2, PayloadType2.class,
                new PayloadType2());

        // object for testing
        testedObject = new MessageRouterImpl(activeObjects, //
                new MessageConsumer[] { expectedDataForConsumer1.consumer, expectedDataForConsumer2.consumer }, //
                new MessagePayloadSerializer[] { expectedDataForConsumer1.serializer, expectedDataForConsumer2.serializer } //
        );

        recordBehaviorForMessage(expectedDataForConsumer1.keyId, expectedDataForConsumer1.message);
        recordBehaviorForMessage(expectedDataForConsumer2.keyId, expectedDataForConsumer2.message);

        // message publishing & consuming
        testedObject.publish(expectedDataForConsumer1.key, expectedDataForConsumer1.payloadValue);
        testedObject.publish(expectedDataForConsumer2.key, expectedDataForConsumer2.payloadValue);
        Thread.sleep(100);

        // message verification
        Mockito.verify(expectedDataForConsumer1.consumer, Mockito.times(1)).onReceive(expectedDataForConsumer1.messageId,
                expectedDataForConsumer1.payloadValue, null);
        Mockito.verify(expectedDataForConsumer2.consumer, Mockito.times(1)).onReceive(expectedDataForConsumer2.messageId,
                expectedDataForConsumer2.payloadValue, null);
    }

    /**
     * Records behavior for single published message.
     * 
     * @param keyId
     *            key identity of message
     * @param message
     *            for publishing
     */
    private void recordBehaviorForMessage(final String keyId, MessageMapping message)
    {
        // Message is created and stored into the database in first step
        Mockito.doReturn(message).when(activeObjects)
                .create(Mockito.eq(MessageMapping.class), Mockito.eq(new DBParam(MessageMapping.KEY, keyId)), Mockito.<DBParam> any());

        // In second step the same message will be received from database
        Mockito.doReturn(new MessageMapping[] { message }).doReturn(new MessageMapping[] {}).when(activeObjects)
                .find(Mockito.eq(MessageMapping.class), ActiveObjectsMock.queryContainsWhereParameter(keyId));
    }

}
