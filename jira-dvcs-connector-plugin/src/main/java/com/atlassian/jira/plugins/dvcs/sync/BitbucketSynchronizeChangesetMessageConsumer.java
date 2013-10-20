package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetTransformer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

/**
 * Consumer of {@link BitbucketSynchronizeChangesetMessage}-s.
 *
 * @author Stanislav Dvorscak
 *
 */
public class BitbucketSynchronizeChangesetMessageConsumer implements MessageConsumer<BitbucketSynchronizeChangesetMessage>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BitbucketSynchronizeChangesetMessageConsumer.class);
    private static final String ID = BitbucketSynchronizeChangesetMessageConsumer.class.getCanonicalName();
    public static final String KEY = BitbucketSynchronizeChangesetMessage.class.getCanonicalName();

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

    public BitbucketSynchronizeChangesetMessageConsumer()
    {
    }

    @Override
    public void onReceive(Message<BitbucketSynchronizeChangesetMessage> message)
    {
        CachingDvcsCommunicator cachingCommunicator = (CachingDvcsCommunicator) dvcsCommunicatorProvider
                .getCommunicator(BitbucketCommunicator.BITBUCKET);
        BitbucketCommunicator communicator = (BitbucketCommunicator) cachingCommunicator.getDelegate();

        Repository repo = message.getPayload().getRepository();
        int pageNum = message.getPayload().getPage();

        BitbucketChangesetPage page = communicator.getChangesetsForPage(pageNum, repo, createInclude(message.getPayload()), message
                .getPayload().getExclude());
        process(message, page);
        //
    }

    private List<String> createInclude(BitbucketSynchronizeChangesetMessage payload)
    {
        List<BranchHead> newHeads = payload.getNewHeads();
        List<String> newHeadsNodes = extractBranchHeads(newHeads);
        if (newHeadsNodes != null && payload.getExclude() != null)
        {
            newHeadsNodes.removeAll(payload.getExclude());
        }
        return newHeadsNodes;
    }

    private void process(Message<BitbucketSynchronizeChangesetMessage> message, BitbucketChangesetPage page)
    {
        List<BitbucketNewChangeset> csets = page.getValues();
        boolean errorOnPage = false;
        BitbucketSynchronizeChangesetMessage payload = message.getPayload();
        boolean softSync = payload.isSoftSync();

        for (BitbucketNewChangeset ncset : csets)
        {
            try
            {
                Repository repo = payload.getRepository();
                Changeset fromDB = changesetService.getByNode(repo.getId(), ncset.getHash());
                if (fromDB != null)
                {
                    continue;
                }
                assignBranch(ncset, payload);
                Changeset cset = ChangesetTransformer.fromBitbucketNewChangeset(repo.getId(), ncset);
                cset = changesetService.getDetailChangesetFromDvcs(repo, cset);
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
            } catch (Exception e)
            {
                errorOnPage = true;
                ((DefaultProgress) payload.getProgress()).setError("Error during sync. See server logs.");
                LOGGER.error(e.getMessage(), e);
            }
        }

        if (!errorOnPage)
        {
            if (StringUtils.isNotBlank(page.getNext()))
            {
                fireNextPage(page, payload, message.getTags());
            }

            messagingService.ok(this, message);
        } else
        {
            messagingService.fail(this, message);
        }
    }

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

    private void fireNextPage(BitbucketChangesetPage prevPage, BitbucketSynchronizeChangesetMessage originalMessage, String[] tags)
    {
        messagingService.publish(
                getAddress(), //
                new BitbucketSynchronizeChangesetMessage(originalMessage.getRepository(), //
                        originalMessage.getRefreshAfterSynchronizedAt(), //
                        originalMessage.getProgress(), //
                        originalMessage.getNewHeads(), originalMessage.getExclude(), prevPage.getPage() + 1, originalMessage
                                .getNodesToBranches(), originalMessage.isSoftSync(), originalMessage.getSyncAuditId()), tags);
    }

    private List<String> extractBranchHeads(List<BranchHead> branchHeads)
    {
        if (branchHeads == null)
        {
            return null;
        }
        List<String> result = new ArrayList<String>();
        for (BranchHead branchHead : branchHeads)
        {
            result.add(branchHead.getHead());
        }
        return result;
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
        return 1;
    }

    @Override
    public boolean shouldDiscard(int messageId, int retryCount, BitbucketSynchronizeChangesetMessage payload, String[] tags)
    {
        return retryCount >= 3;
    }

    @Override
    public void afterDiscard(int messageId, int retryCount, BitbucketSynchronizeChangesetMessage payload, String[] tags)
    {

    }

}
