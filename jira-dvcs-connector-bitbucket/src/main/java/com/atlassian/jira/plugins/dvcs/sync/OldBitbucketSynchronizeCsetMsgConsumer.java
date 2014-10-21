package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync.OldBitbucketSynchronizeCsetMsg;
import org.springframework.stereotype.Component;

@Component
public class OldBitbucketSynchronizeCsetMsgConsumer extends MessageConsumerSupport<OldBitbucketSynchronizeCsetMsg>
{
    private static final String ID = OldBitbucketSynchronizeCsetMsgConsumer.class.getCanonicalName();
    public static final String KEY = OldBitbucketSynchronizeCsetMsg.class.getCanonicalName();

    @Override
    public String getQueue()
    {
        return ID;
    }

    @Override
    public MessageAddress<OldBitbucketSynchronizeCsetMsg> getAddress()
    {
        return messagingService.get(OldBitbucketSynchronizeCsetMsg.class, KEY);
    }

    @Override
    protected Repository getRepository(OldBitbucketSynchronizeCsetMsg payload)
    {
        return payload.getRepository();
    }
    @Override
    protected String getBranch(OldBitbucketSynchronizeCsetMsg payload)
    {
        return payload.getBranch();
    }

    @Override
    protected String getNode(OldBitbucketSynchronizeCsetMsg payload)
    {
        return payload.getNode();
    }

    @Override
    protected OldBitbucketSynchronizeCsetMsg createNextMessage(OldBitbucketSynchronizeCsetMsg payload, String parentChangesetNode)
    {
       return new OldBitbucketSynchronizeCsetMsg(payload.getRepository(), //
               payload.getBranch(), //
               parentChangesetNode, //
               payload.getRefreshAfterSynchronizedAt(), //
               payload.getProgress(),
               payload.isSoftSync(), payload.getSyncAuditId(),
               payload.isWebHookSync());
    }

    @Override
    protected boolean getSoftSync(OldBitbucketSynchronizeCsetMsg payload)
    {
        return payload.isSoftSync();
    }
}
