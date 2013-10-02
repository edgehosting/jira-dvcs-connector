package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;

public class GithubSynchronizeChangesetMessageConsumer extends MessageConsumerSupport<SynchronizeChangesetMessage>
{

    private static final String ID = GithubSynchronizeChangesetMessageConsumer.class.getCanonicalName();
    public static final String KEY = SynchronizeChangesetMessage.class.getCanonicalName();

    public String getId()
    {
        return ID;
    }

    @Override
    public MessageKey<SynchronizeChangesetMessage> getKey()
    {
        return messagingService.get(SynchronizeChangesetMessage.class, KEY);
    }

    @Override
    protected Repository getRepository(SynchronizeChangesetMessage payload)
    {
        return payload.getRepository();
    }

    @Override
    protected String getBranch(SynchronizeChangesetMessage payload)
    {
        return payload.getBranch();
    }

    @Override
    protected String getNode(SynchronizeChangesetMessage payload)
    {
        return payload.getNode();
    }

    @Override
    protected SynchronizeChangesetMessage createNextMessage(SynchronizeChangesetMessage payload, String parentChangesetNode)
    {
        return new SynchronizeChangesetMessage(payload.getRepository(), payload.getBranch(), parentChangesetNode,
                payload.getRefreshAfterSynchronizedAt(), payload.getProgress(), payload.isSoftSync());
    }

    @Override
    protected boolean getSoftSync(SynchronizeChangesetMessage payload)
    {
        return payload.isSoftSync();
    }

    @Override
    public boolean shouldDiscard(int messageId, int retryCount, SynchronizeChangesetMessage payload, String[] tags)
    {
        return retryCount >= 3;
    }

}
