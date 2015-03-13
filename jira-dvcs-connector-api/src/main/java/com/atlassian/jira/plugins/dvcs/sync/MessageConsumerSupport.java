package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Set;
import javax.annotation.Resource;

public abstract class MessageConsumerSupport<P extends HasProgress> implements MessageConsumer<P>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerSupport.class);

    @Resource
    protected DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Resource
    protected ChangesetService changesetService;

    @Resource
    protected RepositoryService repositoryService;

    @Resource
    protected LinkedIssueService linkedIssueService;

    @Resource
    protected MessagingService messagingService;

    @Resource
    protected BranchService branchService;

    @Override
    public final void onReceive(final Message<P> message, final P payload)
    {
        String[] tags = message.getTags();

        Repository repo = getRepository(payload);
        String node = getNode(payload);
        String branch = getBranch(payload);
        boolean softSync = getSoftSync(payload);
        int priority = softSync? MessagingService.SOFTSYNC_PRIORITY : MessagingService.DEFAULT_PRIORITY;

        if (changesetService.getByNode(repo.getId(), node) == null)
        {
            LOGGER.trace("Fetching changeset '{}'", node);
            Date synchronizedAt = new Date();
            Changeset changeset = dvcsCommunicatorProvider.getCommunicator(repo.getDvcsType()).getChangeset(repo, node);
            changeset.setSynchronizedAt(synchronizedAt);
            changeset.setBranch(branch);
            if (!node.equals(changeset.getNode()))
            {
                throw new SourceControlException(String.format("Error retrieving branch '%s'. Communicator for '%s' returned '%s' instead of '%s'", branch, repo.getDvcsType(), changeset.getNode(), node));
            }

            LOGGER.info("message={}", changeset.getMessage());
            
            Set<String> issues = linkedIssueService.getIssueKeys(changeset.getMessage());
            markChangesetForSmartCommit(repo, changeset, softSync && CollectionUtils.isNotEmpty(issues));

            changesetService.create(changeset, issues);

            payload.getProgress().inProgress( //
                    payload.getProgress().getChangesetCount() + 1, //
                    payload.getProgress().getJiraCount() + issues.size(), //
                    0 //
                    );

            if (changeset.getParents().isEmpty())
            {
                LOGGER.debug("Changeset {} has no parents, stopping traversal for {}", node, branch);
            }
            else
            {
                for (String parentChangesetNode : changeset.getParents())
                {
                    if (changesetService.getByNode(repo.getId(), parentChangesetNode) == null) {
                        messagingService.publish(getAddress(), createNextMessage(payload, parentChangesetNode), priority, tags);
                    }
                }
            }

            if (repo.getLastCommitDate() == null || repo.getLastCommitDate().before(changeset.getDate()))
            {
                repo.setLastCommitDate(changeset.getDate());
                repositoryService.save(repo);
            }
        }
        else
        {
            LOGGER.debug("Changeset {} already exists, stopping traversal for {}", node, branch);
        }
    }

    static void markChangesetForSmartCommit(Repository repo, Changeset changesetForSave, boolean mark)
    {
        if (repo.isSmartcommitsEnabled())
        {
            LOGGER.debug("Marking changeset node = {} to be processed by smart commits with value = " + mark, changesetForSave.getNode());
            changesetForSave.setSmartcommitAvaliable(mark);
        } else
        {
            LOGGER.debug("Changeset node = {}. Repository not enabled for smartcommits.", changesetForSave.getNode());
        }
    }

    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }

    protected abstract Repository getRepository(P payload);

    protected abstract String getBranch(P payload);

    protected abstract String getNode(P payload);

    protected abstract boolean getSoftSync(P payload);

    protected abstract P createNextMessage(P payload, String parentChangesetNode);
}
