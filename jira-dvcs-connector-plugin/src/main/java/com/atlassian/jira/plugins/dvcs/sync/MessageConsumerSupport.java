package com.atlassian.jira.plugins.dvcs.sync;

import java.util.Date;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsChangesetsProcessor;

public abstract class MessageConsumerSupport<P extends HasProgress> implements MessageConsumer<P>
{

    private final static Logger LOGGER = LoggerFactory.getLogger(MessageConsumerSupport.class);

    @Resource
    protected DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Resource
    protected ChangesetService changesetService;

    @Resource
    protected RepositoryService repositoryService;

    @Resource
    protected LinkedIssueService linkedIssueService;

    @Resource
    protected MessagingService<P> messagingService;

    @Resource
    protected BranchService branchService;

    @Resource
    SmartcommitsChangesetsProcessor smartCommitsProcessor;

    @Override
    public void onReceive(int messageId, P payload, String [] tags)
    {
        try
        {
            Repository repo = getRepository(payload);
            String node = getNode(payload);
            String branch = getBranch(payload);
            boolean softSync = getSoftSync(payload);

            Changeset fromDB = changesetService.getByNode(repo.getId(), node);
            if (fromDB != null)
            {
                return;
            }

            Date synchronizedAt = new Date();
            Changeset changeset = dvcsCommunicatorProvider.getCommunicator(repo.getDvcsType()).getChangeset(
                    repo, node);
            changeset.setSynchronizedAt(synchronizedAt);
            changeset.setBranch(branch);

            Set<String> issues = linkedIssueService.getIssueKeys(changeset.getMessage());
            markChangesetForSmartCommit(repo, changeset, softSync && CollectionUtils.isNotEmpty(issues));

            changesetService.create(changeset, issues);

            payload.getProgress().inProgress( //
                    payload.getProgress().getChangesetCount() + 1, //
                    payload.getProgress().getJiraCount() + issues.size(), //
                    0 //
                    );

            for (String parentChangesetNode : changeset.getParents())
            {
                messagingService.publish(getKey(),
                        createNextMessage(payload, parentChangesetNode), tags);
            }

            if (repo.getLastCommitDate() == null
                    || repo.getLastCommitDate().before(changeset.getDate()))
            {
                repo.setLastCommitDate(changeset.getDate());
                repositoryService.save(repo);
            }

            messagingService.ok(this, messageId);

        } catch (Exception e)
        {
            messagingService.fail(this, messageId);
            LOGGER.error(e.getMessage(), e);
        } finally
        {
            tryEndProgress(payload, tags);
        }

    }

    protected void tryEndProgress(P payload, String[] tags)
    {
        if (messagingService.getQueuedCount(getKey(), tags[0]) == 0)
        {
            smartCommitsProcessor.startProcess(payload.getProgress(), getRepository(payload), changesetService);
            payload.getProgress().finish();
        }
    }

    @Override
    public void afterDiscard(int messageId, int retryCount, P payload, String[] tags)
    {
        tryEndProgress(payload, tags);
    }

    static void markChangesetForSmartCommit(Repository repo, Changeset changesetForSave, boolean mark)
    {
        if (repo.isSmartcommitsEnabled())
        {
            LOGGER.debug("Marking changeset node = {} to be processed by smart commits", changesetForSave.getNode());
            changesetForSave.setSmartcommitAvaliable(mark);
        } else {
            LOGGER.debug("Changeset node = {}. Repository not enabled for smartcommits.", changesetForSave.getNode());
        }
    }



    protected abstract Repository getRepository(P payload);
    protected abstract String getBranch(P payload);
    protected abstract String getNode(P payload);
    protected abstract boolean getSoftSync(P payload);
    protected abstract P createNextMessage(P payload, String parentChangesetNode);

}
