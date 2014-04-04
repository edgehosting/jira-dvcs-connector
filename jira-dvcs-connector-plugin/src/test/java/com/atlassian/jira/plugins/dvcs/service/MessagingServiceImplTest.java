package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.cache.memory.MemoryCacheManager;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeActivityMessageConsumer;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

public class MessagingServiceImplTest
{
    @Test
    public void getReturnsDifferentInstancesGivenDifferentKeys() throws Exception
    {
        MessagingServiceImpl messagingService = new MessagingServiceImpl(new MemoryCacheManager());
        final MessageAddress<BitbucketSynchronizeActivityMessage> address = messagingService.get(BitbucketSynchronizeActivityMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        final MessageAddress<BitbucketSynchronizeActivityMessage> address2 = messagingService.get(BitbucketSynchronizeActivityMessage.class, "abc");
        assertThat(address, not(sameInstance(address2)));
    }

    @Test
    public void getReturnsSameInstanceGivenSameKey() throws Exception
    {
        MessagingServiceImpl messagingService = new MessagingServiceImpl(new MemoryCacheManager());
        final MessageAddress<BitbucketSynchronizeActivityMessage> address = messagingService.get(BitbucketSynchronizeActivityMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        final MessageAddress<BitbucketSynchronizeActivityMessage> address2 = messagingService.get(BitbucketSynchronizeActivityMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        assertThat(address, sameInstance(address2));
    }

    @Test
    public void getReturnsSameInstanceGivenSameKeyButDifferentPayloadType() throws Exception
    {
        MessagingServiceImpl messagingService = new MessagingServiceImpl(new MemoryCacheManager());
        final MessageAddress<BitbucketSynchronizeActivityMessage> address = messagingService.get(BitbucketSynchronizeActivityMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        final MessageAddress<?> address2 = messagingService.get(BitbucketSynchronizeChangesetMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        //noinspection unchecked
        assertThat(address, sameInstance((MessageAddress<BitbucketSynchronizeActivityMessage>)address2));
    }
}
