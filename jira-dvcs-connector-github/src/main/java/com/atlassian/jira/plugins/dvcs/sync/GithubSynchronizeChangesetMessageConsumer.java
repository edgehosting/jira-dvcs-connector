package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;
import org.springframework.stereotype.Component;

@Component
public class GithubSynchronizeChangesetMessageConsumer extends MessageConsumerSupport<SynchronizeChangesetMessage>
{

    private static final String QUEUE = GithubSynchronizeChangesetMessageConsumer.class.getCanonicalName();
    public static final String ADDRESS = SynchronizeChangesetMessage.class.getCanonicalName();

    public String getQueue()
    {
        return QUEUE;
    }

    @Override
    public MessageAddress<SynchronizeChangesetMessage> getAddress()
    {
        return messagingService.get(SynchronizeChangesetMessage.class, ADDRESS);
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
                payload.getRefreshAfterSynchronizedAt(), payload.getProgress(), payload.isSoftSync(), payload.getSyncAuditId(), payload.isWebHookSync());
    }

    @Override
    protected boolean getSoftSync(SynchronizeChangesetMessage payload)
    {
        return payload.isSoftSync();
    }
}
