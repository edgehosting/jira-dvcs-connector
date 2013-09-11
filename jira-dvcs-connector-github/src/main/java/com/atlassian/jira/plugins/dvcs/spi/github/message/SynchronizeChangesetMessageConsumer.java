package com.atlassian.jira.plugins.dvcs.spi.github.message;

import java.util.Date;
import java.util.Set;

import javax.annotation.Resource;

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
     * Injected {@link DvcsCommunicatorProvider} dependency.
     */
    @Resource
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    /**
     * Injected {@link ChangesetService} dependency.
     */
    @Resource
    private ChangesetService changesetService;

    /**
     * Injected {@link RepositoryService} dependency.
     */
    @Resource
    private RepositoryService repositoryService;

    /**
     * Injected {@link LinkedIssueService} dependency.
     */
    @Resource
    private LinkedIssueService linkedIssueService;

    /**
     * Injected {@link MessagingService} dependency.
     */
    @Resource
    private MessagingService messagingService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(int messageId, SynchronizeChangesetMessage payload, String [] tags)
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
