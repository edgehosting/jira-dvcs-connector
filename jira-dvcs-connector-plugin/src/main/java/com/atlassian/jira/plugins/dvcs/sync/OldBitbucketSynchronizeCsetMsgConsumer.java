package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync.OldBitbucketSynchronizeCsetMsg;

import java.util.List;

public class OldBitbucketSynchronizeCsetMsgConsumer extends MessageConsumerSupport<OldBitbucketSynchronizeCsetMsg>
{
    private static final String ID = OldBitbucketSynchronizeCsetMsgConsumer.class.getCanonicalName();
    public static final String KEY = OldBitbucketSynchronizeCsetMsg.class.getCanonicalName();

    protected void updateBranchHeads(Repository repo, List<BranchHead> newBranchHeads)
    {
        List<BranchHead> oldBranchHeads = branchService.getListOfBranchHeads(repo);
        branchService.updateBranchHeads(repo, newBranchHeads, oldBranchHeads);
    }

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
                payload.getProgress(), payload.getNewHeads() //
                , payload.isSoftSync(), payload.getSyncAuditId()
        );
    }

    @Override
    protected boolean getSoftSync(OldBitbucketSynchronizeCsetMsg payload)
    {
        return payload.isSoftSync();
    }

    @Override
    public boolean shouldDiscard(int messageId, int retryCount, OldBitbucketSynchronizeCsetMsg payload, String[] tags)
    {
        return retryCount >= 3;
    }
}
