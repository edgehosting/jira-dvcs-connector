package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

/**
 * Consumer of {@link BitbucketSynchronizeChangesetMessage}-s.
 *
 * @author Stanislav Dvorscak
 */
@Component
public class BitbucketSynchronizeChangesetMessageConsumer
        implements MessageConsumer<BitbucketSynchronizeChangesetMessage>
{

    private static final String ID = BitbucketSynchronizeChangesetMessageConsumer.class.getCanonicalName();
    public static final String KEY = BitbucketSynchronizeChangesetMessage.class.getCanonicalName();

    @Resource (name = "cachingBitbucketCommunicator")
    private CachingDvcsCommunicator cachingCommunicator;
    @Resource
    private ChangesetService changesetService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private LinkedIssueService linkedIssueService;
    @Resource
    private MessagingService messagingService;

    public BitbucketSynchronizeChangesetMessageConsumer()
    {
    }

    @Override
    public void onReceive(Message<BitbucketSynchronizeChangesetMessage> message, final BitbucketSynchronizeChangesetMessage payload)
    {
        final BitbucketCommunicator communicator = (BitbucketCommunicator) cachingCommunicator.getDelegate();

        final BitbucketChangesetPage page = FlightTimeInterceptor.execute(payload.getProgress(), new FlightTimeInterceptor.Callable<BitbucketChangesetPage>()
        {
            @Override
            public BitbucketChangesetPage call()
            {
                return communicator.getNextPage(payload.getRepository(), payload.getInclude(), payload.getExclude(), payload.getPage());
            }
        });

        process(message.getTags(), payload, page);
    }

    /**
     * Loads changesets not already in the database from the page of changesets into the database
     *
     * @param messageTags message tags
     * @param page the page of changesets to be processed
     * @param payload an object that is passed around that contains information specific to the processing of all the
     * bitbucket pages
     */
    void process(String[] messageTags, BitbucketSynchronizeChangesetMessage payload, BitbucketChangesetPage page)
    {
        List<BitbucketNewChangeset> newChangesets = page.getValues();
        boolean softSync = payload.isSoftSync();
        Repository repo = payload.getRepository();

        for (BitbucketNewChangeset newChangeset : newChangesets)
        {
            Changeset fromDB = changesetService.getByNode(repo.getId(), newChangeset.getHash());
            if (fromDB != null)
            {
                continue;
            }
            assignBranch(newChangeset, payload);
            Changeset cset = ChangesetTransformer.fromBitbucketNewChangeset(repo.getId(), newChangeset);
            cset.setSynchronizedAt(new Date());
            Set<String> issues = linkedIssueService.getIssueKeys(cset.getMessage());

            MessageConsumerSupport.markChangesetForSmartCommit(repo, cset, softSync && CollectionUtils.isNotEmpty(issues));

            changesetService.create(cset, issues);

            if (repo.getLastCommitDate() == null || repo.getLastCommitDate().before(cset.getDate()))
            {
                payload.getRepository().setLastCommitDate(cset.getDate());
                repositoryService.save(payload.getRepository());
            }

            payload.getProgress().inProgress( //
                    payload.getProgress().getChangesetCount() + 1, //
                    payload.getProgress().getJiraCount() + issues.size(), //
                    0 //
            );
        }

        if (StringUtils.isNotBlank(page.getNext()))
        {
            fireNextPage(page, payload, softSync, messageTags);
        }
        else
        {
            cachingCommunicator.linkRepository(repo, changesetService.findReferencedProjects(repo.getId()));
        }

    }

    /**
     * A Magical Mystery, if you know how or why this works update this javadoc with a better explanation
     *
     * @param cset incomming Changeset
     * @param originalMessage an object that holds state specific to the synchronisation of this repository
     */
    private void assignBranch(BitbucketNewChangeset cset, BitbucketSynchronizeChangesetMessage originalMessage)
    {
        Map<String, String> changesetBranch = originalMessage.getNodesToBranches();

        String branch = changesetBranch.get(cset.getHash());
        cset.setBranch(branch);
        changesetBranch.remove(cset.getHash());
        for (BitbucketNewChangeset parent : cset.getParents())
        {
            changesetBranch.put(parent.getHash(), branch);
        }
    }

    private void fireNextPage(BitbucketChangesetPage prevPage, BitbucketSynchronizeChangesetMessage originalMessage, boolean softSync, String[] tags)
    {
        messagingService.publish(
                getAddress(), //
                new BitbucketSynchronizeChangesetMessage(originalMessage.getRepository(), //
                        originalMessage.getRefreshAfterSynchronizedAt(), //
                        originalMessage.getProgress(), //
                        originalMessage.getInclude(), originalMessage.getExclude(), prevPage, originalMessage.getNodesToBranches(), originalMessage.isSoftSync(), originalMessage.getSyncAuditId(), originalMessage.isWebHookSync()), softSync ? MessagingService.SOFTSYNC_PRIORITY : MessagingService.DEFAULT_PRIORITY, tags);
    }

    @Override
    public String getQueue()
    {
        return ID;
    }

    @Override
    public MessageAddress<BitbucketSynchronizeChangesetMessage> getAddress()
    {
        return messagingService.get(BitbucketSynchronizeChangesetMessage.class, KEY);
    }

    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }
}
