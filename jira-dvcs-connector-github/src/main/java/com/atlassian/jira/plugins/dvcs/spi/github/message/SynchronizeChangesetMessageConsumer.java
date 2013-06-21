package com.atlassian.jira.plugins.dvcs.spi.github.message;

import java.util.Date;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

/**
 * Consumer of {@link SynchronizeChangesetMessage}-s.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class SynchronizeChangesetMessageConsumer implements MessageConsumer<SynchronizeChangesetMessage>
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizeChangesetMessageConsumer.class);

    /**
     * @see #getId()
     */
    private static final String ID = SynchronizeChangesetMessageConsumer.class.getCanonicalName();

    /**
     * @see #getKey()
     */
    public static final String KEY = SynchronizeChangesetMessage.class.getCanonicalName();

    /**
     * @see #setDvcsCommunicatorProvider(DvcsCommunicatorProvider)
     */
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    /**
     * @see #setChangesetService(ChangesetService)
     */
    private ChangesetService changesetService;

    /**
     * @see #setRepositoryService(RepositoryService)
     */
    private RepositoryService repositoryService;

    /**
     * @see #setLinkedIssueService(LinkedIssueService)
     */
    private LinkedIssueService linkedIssueService;

    /**
     * @see #setMessagingService(MessagingService)
     */
    private MessagingService messagingService;

    /**
     * @param dvcsCommunicatorProvider
     *            injected {@link DvcsCommunicatorProvider} dependency
     */
    public void setDvcsCommunicatorProvider(DvcsCommunicatorProvider dvcsCommunicatorProvider)
    {
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
    }

    /**
     * @param changesetService
     *            injected {@link ChangesetService} dependency
     */
    public void setChangesetService(ChangesetService changesetService)
    {
        this.changesetService = changesetService;
    }

    /**
     * @param repositoryService
     *            injected {@link RepositoryService} dependency
     */
    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    /**
     * @param linkedIssueService
     *            injected {@link LinkedIssueService} dependency
     */
    public void setLinkedIssueService(LinkedIssueService linkedIssueService)
    {
        this.linkedIssueService = linkedIssueService;
    }

    /**
     * @param messagingService
     *            injected {@link MessagingService} dependency
     */
    public void setMessagingService(MessagingService messagingService)
    {
        this.messagingService = messagingService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(int messageId, SynchronizeChangesetMessage payload)
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
                        new SynchronizeChangesetMessage(payload.getRepository(), //
                                payload.getBranch(), //
                                parentChangesetNode, //
                                payload.getRefreshAfterSynchronizedAt(), //
                                payload.getProgress(), //
                                payload.getSynchronizationTag() //
                        ), payload.getSynchronizationTag());
            }

            if (payload.getRepository().getLastCommitDate() == null
                    || payload.getRepository().getLastCommitDate().before(changeset.getDate()))
            {
                payload.getRepository().setLastCommitDate(changeset.getDate());
                repositoryService.save(payload.getRepository());
            }

            if (messagingService.getQueuedCount(getKey(), payload.getSynchronizationTag()) == 0)
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId()
    {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey<SynchronizeChangesetMessage> getKey()
    {
        return messagingService.get(SynchronizeChangesetMessage.class, KEY);
    }

}
