package com.atlassian.jira.plugins.dvcs.spi.bitbucket.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
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
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetTransformer;

/**
 * Consumer of {@link BitbucketSynchronizeChangesetMessage}-s.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class BitbucketSynchronizeChangesetConsumer implements MessageConsumer<BitbucketSynchronizeChangesetMessage>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BitbucketSynchronizeChangesetConsumer.class);
    private static final String ID = BitbucketSynchronizeChangesetConsumer.class.getCanonicalName();
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

    public BitbucketSynchronizeChangesetConsumer()
    {
    }

    @Override
    public void onReceive(int messageId, BitbucketSynchronizeChangesetMessage payload, String [] tags)
    {
        CachingDvcsCommunicator cachingCommunicator = (CachingDvcsCommunicator) dvcsCommunicatorProvider
                .getCommunicator(BitbucketCommunicator.BITBUCKET);
        BitbucketCommunicator communicator = (BitbucketCommunicator) cachingCommunicator.getDelegate();

        Repository repo = payload.getRepository();
        int pageNum = payload.getPage();
        
        BitbucketChangesetPage page = communicator.getChangesetsForPage(pageNum, repo, createInclude(payload), payload.getExclude());
        process(messageId, page, payload, tags);
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

    private void process(int messageId, BitbucketChangesetPage page, BitbucketSynchronizeChangesetMessage originalMessage, String[] tags)
    {
        List<BitbucketNewChangeset> csets = page.getValues();
        boolean errorOnPage = false;
        for (BitbucketNewChangeset ncset : csets)
        {
            try
            {
                Repository repo = originalMessage.getRepository();
                Changeset fromDB = changesetService.getByNode(repo.getId(), ncset.getHash());
                if (fromDB != null)
                {
                    continue;
                }
                assignBranch(ncset, originalMessage);
                Changeset cset = ChangesetTransformer.fromBitbucketNewChangeset(repo.getId(), ncset);
                cset = changesetService.getDetailChangesetFromDvcs(repo, cset);
                cset.setSynchronizedAt(new Date());
                Set<String> issues = linkedIssueService.getIssueKeys(cset.getMessage());

                changesetService.create(cset, issues);

                if (repo.getLastCommitDate() == null || repo.getLastCommitDate().before(cset.getDate()))
                {
                    originalMessage.getRepository().setLastCommitDate(cset.getDate());
                    repositoryService.save(originalMessage.getRepository());
                }

                originalMessage.getProgress().inProgress( //
                        originalMessage.getProgress().getChangesetCount() + 1, //
                        originalMessage.getProgress().getJiraCount() + issues.size(), //
                        0 //
                        );
            } catch (Exception e)
            {
                errorOnPage = true;
                LOGGER.error(e.getMessage(), e);
            }
        }

        if (!errorOnPage && StringUtils.isNotBlank(page.getNext()))
        {
            fireNextPage(page, originalMessage, tags);
            
        } else if (errorOnPage)
        {
            messagingService.fail(this, messageId);
        }
        
        if (!errorOnPage)
        {
            if (messagingService.getQueuedCount(getKey(), tags[0]) == 0)
            {
                originalMessage.getProgress().finish();
                if (page.getPage() == 1)
                {
                    updateBranchHeads(originalMessage.getRepository(), originalMessage.getNewHeads());
                }
            }

            messagingService.ok(this, messageId);
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

    protected void updateBranchHeads(Repository repo, List<BranchHead> newBranchHeads)
    {
        List<BranchHead> oldBranchHeads = branchService.getListOfBranchHeads(repo);
        branchService.updateBranchHeads(repo, newBranchHeads, oldBranchHeads);
    }

    private void fireNextPage(BitbucketChangesetPage prevPage, BitbucketSynchronizeChangesetMessage originalMessage, String [] tags)
    {
        messagingService.publish(getKey(), //
                new BitbucketSynchronizeChangesetMessage(originalMessage.getRepository(), //
                        originalMessage.getRefreshAfterSynchronizedAt(), //
                        originalMessage.getProgress(), //
                        originalMessage.getNewHeads(), originalMessage.getExclude(), prevPage.getPage() + 1,
                        originalMessage.getNodesToBranches()
                ), tags);

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
    public String getId()
    {
        return ID;
    }

    @Override
    public MessageKey<BitbucketSynchronizeChangesetMessage> getKey()
    {
        return messagingService.get(BitbucketSynchronizeChangesetMessage.class, KEY);
    }

    @Override
    public boolean shouldDiscard(int messageId, int retryCount, BitbucketSynchronizeChangesetMessage payload, String[] tags)
    {
        return false;
    }

    @Override
    public void beforeDiscard(int messageId, int retryCount, BitbucketSynchronizeChangesetMessage payload, String[] tags)
    {
        
    }
    
}
