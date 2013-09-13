package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

/**
 * Consumer of {@link OldBitbucketSynchronizeCsetMsg}-s.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class OldBitbucketSynchronizeCsetMsgConsumer implements MessageConsumer<OldBitbucketSynchronizeCsetMsg>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OldBitbucketSynchronizeCsetMsgConsumer.class);

    private static final String ID = OldBitbucketSynchronizeCsetMsgConsumer.class.getCanonicalName();

    public static final String KEY = OldBitbucketSynchronizeCsetMsg.class.getCanonicalName();

    @Resource
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;
    @Resource
    private ChangesetService changesetService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private LinkedIssueService linkedIssueService;
    @Resource
    private MessagingService messagingService;
    @Resource
    private BranchService branchService;

    @Override
    public void onReceive(int messageId, OldBitbucketSynchronizeCsetMsg payload, String [] tags)
    {
        try
        {
            Changeset fromDB = changesetService.getByNode(payload.getRepository().getId(), payload.getNode());
            if (fromDB != null)
            {
                return;
            }

            Date synchronizedAt = new Date();
            Changeset changeset = dvcsCommunicatorProvider.getCommunicator(payload.getRepository().getDvcsType()).getChangeset(
                    payload.getRepository(), payload.getNode());
            changeset.setSynchronizedAt(synchronizedAt);
            changeset.setBranch(payload.getBranch());

            Set<String> issues = linkedIssueService.getIssueKeys(changeset.getMessage());

            changesetService.create(changeset, issues);

            payload.getProgress().inProgress( //
                    payload.getProgress().getChangesetCount() + 1, //
                    payload.getProgress().getJiraCount() + issues.size(), //
                    0 //
                    );

            for (String parentChangesetNode : changeset.getParents())
            {
                messagingService.publish(getKey(), //
                        new OldBitbucketSynchronizeCsetMsg(payload.getRepository(), //
                                payload.getBranch(), //
                                parentChangesetNode, //
                                payload.getRefreshAfterSynchronizedAt(), //
                                payload.getProgress(), payload.getNewHeads() //
                        ), tags);
            }

            if (payload.getRepository().getLastCommitDate() == null
                    || payload.getRepository().getLastCommitDate().before(changeset.getDate()))
            {
                payload.getRepository().setLastCommitDate(changeset.getDate());
                repositoryService.save(payload.getRepository());
            }

            if (messagingService.getQueuedCount(getKey(), tags[0]) == 0)
            {
                payload.getProgress().finish();
            }

            messagingService.ok(this, messageId);

        } catch (Exception e)
        {
            messagingService.fail(this, messageId);
            LOGGER.error(e.getMessage(), e);

        }
    }
    
    protected void updateBranchHeads(Repository repo, List<BranchHead> newBranchHeads)
    {
        List<BranchHead> oldBranchHeads = branchService.getListOfBranchHeads(repo);
        branchService.updateBranchHeads(repo, newBranchHeads, oldBranchHeads);
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public MessageKey<OldBitbucketSynchronizeCsetMsg> getKey()
    {
        return messagingService.get(OldBitbucketSynchronizeCsetMsg.class, KEY);
    }

    @Override
    public boolean shouldDiscard(int messageId, int retryCount, OldBitbucketSynchronizeCsetMsg payload, String[] tags)
    {
        return false;
    }

    @Override
    public void beforeDiscard(int messageId, int retryCount, OldBitbucketSynchronizeCsetMsg payload, String[] tags)
    {
        
    }
    
}
