package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestReviewerMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.Reviewer;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestReviewer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

/**
 * Consumer of {@link BitbucketSynchronizeActivityMessage}-s.
 *
 * @author Stanislav Dvorscak
 *
 */
public class BitbucketSynchronizeActivityMessageConsumer implements MessageConsumer<BitbucketSynchronizeActivityMessage>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BitbucketSynchronizeActivityMessageConsumer.class);
    private static final String ID = BitbucketSynchronizeActivityMessageConsumer.class.getCanonicalName();
    public static final String KEY = BitbucketSynchronizeActivityMessage.class.getCanonicalName();

    @Resource
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;
    @Resource
    private ChangesetService changesetService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private MessagingService messagingService;
    @Resource
    private BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    @Resource
    private RepositoryActivityDao dao;
    @Resource
    private RepositoryDao repositoryDao;

    public BitbucketSynchronizeActivityMessageConsumer()
    {
        super();
    }

    @Override
    public void onReceive(Message<BitbucketSynchronizeActivityMessage> message, BitbucketSynchronizeActivityMessage payload)
    {
        Repository repo = payload.getRepository();
        int jiraCount = payload.getProgress().getJiraCount();

        BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> activityPage = null;
        PullRequestRemoteRestpoint pullRestpoint = null;

        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repo).apiVersion(2).build();
        pullRestpoint = remoteClient.getPullRequestAndCommentsRemoteRestpoint();
        activityPage = pullRestpoint.getRepositoryActivityPage(payload.getPageNum(), repo.getOrgName(), repo.getSlug(),
                repo.getActivityLastSync());

        List<BitbucketPullRequestActivityInfo> infos = activityPage.getValues();
        boolean isLastPage = isLastPage(infos);

        Date lastSync = payload.getLastSyncDate();

        for (BitbucketPullRequestActivityInfo info : infos)
        {
            Date activityDate = ClientUtils.extractActivityDate(info.getActivity());

            if (activityDate == null)
            {
                Log.info("Date for the activity could not be found.");
                continue;
            }
            if ((lastSync == null) || (activityDate.after(lastSync)))
            {
                lastSync = activityDate;
                repositoryDao.setLastActivitySyncDate(repo.getId(), lastSync);
            }

            int localPrId = processActivity(payload, info, pullRestpoint);
            markProcessed(payload, info, localPrId);

            payload.getProgress().inPullRequestProgress(processedSize(payload),
                    jiraCount + dao.updatePullRequestIssueKeys(repo, localPrId));
        }
        if (!isLastPage)
        {
            fireNextPage(message, payload, activityPage.getNext(), lastSync);
        }

    }

    protected int processedSize(BitbucketSynchronizeActivityMessage payload)
    {
        return payload.getProcessedPullRequests() == null ? 0 : payload.getProcessedPullRequests().size();
    }

    protected void markProcessed(BitbucketSynchronizeActivityMessage payload, BitbucketPullRequestActivityInfo info, Integer prLocalId)
    {
        payload.getProcessedPullRequests().add(info.getPullRequest().getId().intValue());
    }

    private void fireNextPage(Message<BitbucketSynchronizeActivityMessage> message, BitbucketSynchronizeActivityMessage payload, String nextUrl, Date lastSync)
    {
        messagingService.publish(getAddress(), new BitbucketSynchronizeActivityMessage(payload.getRepository(), null, payload.isSoftSync(),
                payload.getPageNum() + 1, payload.getProcessedPullRequests(), payload.getProcessedPullRequestsLocal(), lastSync, payload.getSyncAuditId()), getPriority(payload), message.getTags());
    }

    private int getPriority(BitbucketSynchronizeActivityMessage payload)
    {
        if (payload == null)
        {
            return MessagingService.DEFAULT_PRIORITY;
        }
        return payload.isSoftSync() ? MessagingService.SOFTSYNC_PRIORITY: MessagingService.DEFAULT_PRIORITY;
    }

    private boolean isLastPage(List<BitbucketPullRequestActivityInfo> infos)
    {
        return infos.isEmpty() || infos.size() < PullRequestRemoteRestpoint.REPO_ACTIVITY_PAGESIZE;
    }

    private int processActivity(BitbucketSynchronizeActivityMessage payload, BitbucketPullRequestActivityInfo info,
            PullRequestRemoteRestpoint pullRestpoint)
    {
        Repository repo = payload.getRepository();

        RepositoryPullRequestMapping localPullRequest = ensurePullRequestPresent(repo, pullRestpoint, info, payload);

        if (isUpdateActivity(info.getActivity()) && !payload.getProcessedPullRequestsLocal().contains(localPullRequest.getID()))
        {
            loadPullRequestCommits(repo, pullRestpoint, localPullRequest.getID(), (BitbucketPullRequestUpdateActivity) info.getActivity(),
                     localPullRequest, info.getPullRequest());
            payload.getProcessedPullRequestsLocal().add(localPullRequest.getID());
        }
        return localPullRequest.getID();
    }

    private RepositoryPullRequestMapping ensurePullRequestPresent(Repository repo, PullRequestRemoteRestpoint pullRestpoint, BitbucketPullRequestActivityInfo info, BitbucketSynchronizeActivityMessage payload)
    {
        BitbucketPullRequest remote = null;
        Map<String, Reviewer> reviewers = null;
        Integer remoteId = info.getPullRequest().getId().intValue();

        // have a detail within this synchronization ?
        if (!payload.getProcessedPullRequests().contains(remoteId))
        {
            BitbucketLink pullRequestLink = info.getPullRequest().getLinks().getSelf();
            if (pullRequestLink != null && !StringUtils.isBlank(pullRequestLink.getHref()))
            {
                remote = pullRestpoint.getPullRequestDetail(pullRequestLink.getHref());
            } else
            {
                // if there is no pull request link fall back to the generated pull request url
                remote = pullRestpoint.getPullRequestDetail(repo.getOrgName(), repo.getSlug(), info
                        .getPullRequest().getId() + "");
            }
            reviewers = loadPulRequestReviewers(pullRestpoint, remote);
        }
        RepositoryPullRequestMapping local = dao.findRequestByRemoteId(repo, remoteId);

        // don't have this pull request, let's save it
        if (local == null)
        {
            local = dao.savePullRequest(repo, toDaoModelPullRequest(remote, repo));
        }
        // maybe update
        if (remote != null && hasChanged(local, remote))
        {
            dao.updatePullRequestInfo(local.getID(), remote.getTitle(), remote.getSource()
                    .getBranch().getName(), remote.getDestination().getBranch().getName(),
                    RepositoryPullRequestMapping.Status.fromBbString(remote.getStatus()),
                    remote.getUpdatedOn(), remote.getSource().getRepository().getFullName());
        }

        if (reviewers != null)
        {
            updatePulRequestReviewers(local.getID(), repo.getId(), reviewers);
        }

        return local;
    }

    private void updatePulRequestReviewers(final int pullRequestId, final int repositoryId, final Map<String,Reviewer> reviewerIndex)
    {
        PullRequestReviewerMapping[] oldReviewers = dao.getReviewers(pullRequestId);
        for (PullRequestReviewerMapping reviewerMapping : oldReviewers)
        {
            Reviewer reviewer = reviewerIndex.remove(reviewerMapping.getUsername());
            if (reviewer == null)
            {
                dao.removeReviewer(reviewerMapping);
            } else
            {
                if (reviewer.isApproved() != reviewerMapping.isApproved())
                {
                    // update approval
                    reviewerMapping.setApproved(reviewer.isApproved());
                    dao.saveReviewer(reviewerMapping);
                }
            }
        }

        for (String username : reviewerIndex.keySet())
        {
            Reviewer reviewer = reviewerIndex.get(username);
            dao.createReviewer(pullRequestId, repositoryId, reviewer);
        }
    }

    private Map<String, Reviewer> loadPulRequestReviewers(final PullRequestRemoteRestpoint pullRestpoint, final BitbucketPullRequest remote)
    {
        BitbucketLink reviewersLink = remote.getLinks().getReviewers();
        BitbucketLink approvalsLink = remote.getLinks().getApprovals();

        Map<String, Reviewer> reviewersIndex = new LinkedHashMap<String, Reviewer>();

        if (reviewersLink != null)
        {
            Iterable<BitbucketPullRequestReviewer> bitbucketReviewers = pullRestpoint.getPullRequestReviewers(reviewersLink.getHref());
            for (BitbucketPullRequestReviewer bitbucketReviewer : bitbucketReviewers)
            {
                Reviewer reviewer = new Reviewer(bitbucketReviewer.getUser().getUsername(), false);
                reviewersIndex.put(bitbucketReviewer.getUser().getUsername(), reviewer);
            }
        }

        if (approvalsLink != null)
        {
            Iterable<BitbucketPullRequestApprovalActivity> bitbucketApprovals = pullRestpoint.getPullRequestApprovals(approvalsLink.getHref());
            for (BitbucketPullRequestApprovalActivity bitbucketApproval : bitbucketApprovals)
            {
                Reviewer reviewer = reviewersIndex.get(bitbucketApproval.getUser().getUsername());
                if (reviewer == null)
                {
                    reviewersIndex.put(bitbucketApproval.getUser().getUsername(), new Reviewer(bitbucketApproval.getUser().getUsername(), true));
                } else
                {
                    reviewer.setApproved(true);
                }
            }
        }

        return reviewersIndex;
    }

    private boolean hasChanged(RepositoryPullRequestMapping local, BitbucketPullRequest remote)
    {
        return !remote.getUpdatedOn().equals(local.getUpdatedOn());
    }

    private void loadPullRequestCommits(Repository repo, PullRequestRemoteRestpoint pullRestpoint,
            int localPullRequestId, BitbucketPullRequestUpdateActivity activity, RepositoryPullRequestMapping savedPullRequest, BitbucketPullRequest remotePullRequest)
    {
        try
        {
            BitbucketLink commitsLink = remotePullRequest.getLinks().getCommits();
            Iterable<BitbucketPullRequestCommit> commitsIterator = null;
            if (commitsLink != null && !StringUtils.isBlank(commitsLink.getHref()))
            {
                commitsIterator = pullRestpoint.getPullRequestCommits(commitsLink.getHref());
            } else
            {
                // if there is no commits link, fall back to use generated commits url
                commitsIterator = pullRestpoint.getPullRequestCommits(repo.getOrgName(), repo.getSlug(), remotePullRequest.getId() + "");
            }

            for (BitbucketPullRequestCommit commit : commitsIterator)
            {
                RepositoryCommitMapping localCommit = dao.getCommitByNode(repo, localPullRequestId, commit.getHash());
                if (localCommit == null)
                {
                    localCommit = saveCommit(repo, commit, null, localPullRequestId);
                    linkCommit(repo, localCommit, savedPullRequest);
                } else {
                    break;
                }
            }
        } catch(BitbucketRequestException e)
        {
            LOGGER.warn("Could not get commits for pull request", e);
        }
    }

    private void linkCommit(Repository domainRepository, RepositoryCommitMapping commitMapping,
            RepositoryPullRequestMapping request)
    {
        dao.linkCommit(domainRepository, request, commitMapping);
        commitMapping.save();
    }


    private RepositoryCommitMapping saveCommit(Repository domainRepository, BitbucketPullRequestCommit commit, String nextNode, int localPullRequestId)
    {
        if (commit != null)
        {
            return dao.saveCommit(domainRepository, toDaoModelCommit(commit));
        }
        return null;
    }

    private Map<String, Object> toDaoModelCommit(BitbucketPullRequestCommit commit)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        if (commit.getAuthor().getUser() != null)
        {
            ret.put(RepositoryCommitMapping.AUTHOR, commit.getAuthor().getUser().getUsername());
        } else
        {
            ret.put(RepositoryCommitMapping.AUTHOR, commit.getAuthor().getRaw().replaceAll("<[^>]*>", "").trim());
        }
        ret.put(RepositoryCommitMapping.RAW_AUTHOR, commit.getAuthor().getRaw());
        ret.put(RepositoryCommitMapping.MESSAGE, commit.getMessage());
        ret.put(RepositoryCommitMapping.NODE, commit.getHash());
        ret.put(RepositoryCommitMapping.DATE, commit.getDate());

        return ret;
    }


    private Map<String, Object> toDaoModelPullRequest(BitbucketPullRequest request, Repository repository)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryPullRequestMapping.REMOTE_ID, request.getId());
        ret.put(RepositoryPullRequestMapping.NAME, request.getTitle());
        ret.put(RepositoryPullRequestMapping.URL, request.getLinks().getHtml().getHref());
        ret.put(RepositoryPullRequestMapping.TO_REPO_ID, repository.getId());

        ret.put(RepositoryPullRequestMapping.AUTHOR, request.getUser().getUsername());
        ret.put(RepositoryPullRequestMapping.CREATED_ON, request.getCreatedOn());
        ret.put(RepositoryPullRequestMapping.UPDATED_ON, request.getUpdatedOn());
        ret.put(RepositoryPullRequestMapping.DESTINATION_BRANCH, request.getDestination().getBranch().getName());
        ret.put(RepositoryPullRequestMapping.SOURCE_BRANCH, request.getSource().getBranch().getName());
        ret.put(RepositoryPullRequestMapping.LAST_STATUS, RepositoryPullRequestMapping.Status.fromBbString(request.getStatus()).name());
        // in case that fork has been deleted, the source repository is null
        if (request.getSource().getRepository() != null)
        {
            ret.put(RepositoryPullRequestMapping.SOURCE_REPO, request.getSource().getRepository().getFullName());
        }

        return ret;
    }

    private boolean isUpdateActivity(BitbucketPullRequestBaseActivity activity)
    {
        return activity instanceof BitbucketPullRequestUpdateActivity
                && "open".equalsIgnoreCase(((BitbucketPullRequestUpdateActivity) activity).getStatus());
    }

    @Override
    public String getQueue()
    {
        return ID;
    }

    @Override
    public MessageAddress<BitbucketSynchronizeActivityMessage> getAddress()
    {
        return messagingService.get(BitbucketSynchronizeActivityMessage.class, KEY);
    }

    @Override
    public int getParallelThreads()
    {
        return 1;
    }

    @Override
    public boolean shouldDiscard(int messageId, int retryCount, BitbucketSynchronizeActivityMessage payload, String[] tags)
    {
        return retryCount >= 3;
    }

    @Override
    public void afterDiscard(int messageId, int retryCount, BitbucketSynchronizeActivityMessage payload, String[] tags)
    {

    }

}
