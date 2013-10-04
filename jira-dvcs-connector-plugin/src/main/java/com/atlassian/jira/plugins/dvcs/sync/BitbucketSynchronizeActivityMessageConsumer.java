package com.atlassian.jira.plugins.dvcs.sync;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestCommentActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivityEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommentActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.dao.BitbucketPullRequestDao;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;

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
    private MessagingService<BitbucketSynchronizeActivityMessage> messagingService;
    @Resource
    private BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    @Resource
    private RepositoryActivityDao dao;
    @Resource
    private RepositoryDao repositoryDao;
    @Resource
    private BitbucketPullRequestDao pullRequestDao;

    public BitbucketSynchronizeActivityMessageConsumer()
    {
        super();
    }

    @Override
    public void onReceive(Message<BitbucketSynchronizeActivityMessage> message)
    {
        BitbucketSynchronizeActivityMessage payload = message.getPayload();
        Repository repo = payload.getRepository();
        int jiraCount = payload.getProgress().getJiraCount();
        int pullRequestActivityCount = 0;
        BitbucketPullRequestBaseActivityEnvelope activityPage = null;
        PullRequestRemoteRestpoint pullRestpoint = null;
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repo).apiVersion(2).build();
            pullRestpoint = remoteClient.getPullRequestAndCommentsRemoteRestpoint();
            activityPage = pullRestpoint.getRepositoryActivityPage(payload.getPageNum(),
                    repo.getOrgName(), repo.getSlug(), repo.getActivityLastSync());
        } catch (Exception e)
        {
            LOGGER.error("Failed to process " + payload.getRepository().getName(), e);
            messagingService.fail(message, this);
            return;
        }

        List<BitbucketPullRequestActivityInfo> infos = activityPage.getValues();
        boolean isLastPage = isLastPage(infos);

        Date lastActivitySyncDate = repo.getActivityLastSync();

        for (BitbucketPullRequestActivityInfo info : infos)
        {
            try
            {
                Date activityDate = ClientUtils.extractActivityDate(info.getActivity());

                if (activityDate == null)
                {
                    Log.info("Date for the activity could not be found.");
                    continue;
                }
                if ((lastActivitySyncDate == null) || (activityDate.after(lastActivitySyncDate)))
                {
                    lastActivitySyncDate = activityDate;
                }

                int localPrId = processActivity(message, info, pullRestpoint);
                markProcessed(payload, info, localPrId);

                payload.getProgress().inPullRequestProgress(++pullRequestActivityCount,
                        jiraCount + dao.updatePullRequestIssueKeys(repo, localPrId));
            } catch (Exception e)
            {
                LOGGER.warn("Failed to process activity from date " + info.getActivity().getDate(), e);
            }
        }
        if (!isLastPage)
        {
            fireNextPage(message, activityPage.getNext());
        } else
        {
            finalizeSync(message, lastActivitySyncDate);
        }

    }

    protected void markProcessed(BitbucketSynchronizeActivityMessage payload, BitbucketPullRequestActivityInfo info, Integer prLocalId)
    {
        payload.getProcessedPullRequests().add(info.getPullRequest().getId().intValue());
        payload.getProcessedPullRequestsLocal().add(prLocalId);
    }

    private void finalizeSync(Message<BitbucketSynchronizeActivityMessage> message, Date lastActivitySyncDate)
    {
        try
        {
            Set<Integer> processedPullRequests = message.getPayload().getProcessedPullRequestsLocal();
            for (Integer localPrId : processedPullRequests)
            {
                RepositoryPullRequestUpdateActivityMapping oldestUpdateActivity = dao.getLatestOrOldestUpdateActivity(message.getPayload()
                        .getRepository(), localPrId, false);
                dao.updateActivityStatus(message.getPayload().getRepository(), oldestUpdateActivity.getID(),
                        RepositoryPullRequestUpdateActivityMapping.Status.OPENED);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to mark pull request to OPEN state: " + e.getMessage());
        }
        finally {
            message.getPayload().getProgress().finish();
            repositoryDao.setLastActivitySyncDate(message.getPayload().getRepository().getId(), lastActivitySyncDate);
            messagingService.ok(message, this);
        }
    }

    private void fireNextPage(Message<BitbucketSynchronizeActivityMessage> message, String nextUrl)
    {
        BitbucketSynchronizeActivityMessage payload = message.getPayload();

        messagingService.publish(getKey(), new BitbucketSynchronizeActivityMessage(payload.getRepository(), null, payload.isSoftSync(),
                payload.getPageNum() + 1, payload.getProcessedPullRequests(), payload.getProcessedPullRequestsLocal()), message.getTags());
    }

    private boolean isLastPage(List<BitbucketPullRequestActivityInfo> infos)
    {
        return infos.isEmpty() || infos.size() < PullRequestRemoteRestpoint.REPO_ACTIVITY_PAGESIZE;
    }

    private int processActivity(Message<BitbucketSynchronizeActivityMessage> message, BitbucketPullRequestActivityInfo info,
            PullRequestRemoteRestpoint pullRestpoint)
    {
        BitbucketSynchronizeActivityMessage payload = message.getPayload();
        Repository repo = payload.getRepository();
        int localPullRequestId = ensurePullRequestPresent(repo, pullRestpoint, info, payload);

        RepositoryActivityMapping savedActivity = dao.saveActivity(repo,
                toDaoModelActivity(info.getActivity(), repo.getId(), localPullRequestId));

        if (isUpdateActivity(info.getActivity()))
        {
            loadPullRequestCommits(repo, pullRestpoint, localPullRequestId, (BitbucketPullRequestUpdateActivity) info.getActivity(),
                    (RepositoryPullRequestUpdateActivityMapping) savedActivity, info.getPullRequest().getLinks().getCommits().getHref());

        }
        return localPullRequestId;
    }

    private int ensurePullRequestPresent(Repository repo, PullRequestRemoteRestpoint pullRestpoint, BitbucketPullRequestActivityInfo info, BitbucketSynchronizeActivityMessage payload)
    {
        BitbucketPullRequest remote = null;
        Integer remoteId = info.getPullRequest().getId().intValue();

        // have a detail within this synchronization ?
        if (!payload.getProcessedPullRequests().contains(remoteId))
        {
            try
            {
                remote = pullRestpoint.getPullRequestDetail(repo.getOrgName(), repo.getSlug(), info
                    .getPullRequest().getId() + "");
            } catch (Exception e)
            {
                Log.error("Could not retrieve pull request details", e);
                remote = new BitbucketPullRequest();
                remote.setId(info.getPullRequest().getId());
            }
        }
        RepositoryPullRequestMapping local = dao.findRequestByRemoteId(repo, remoteId);

        // don't have this pull request, let's save it
        if (local == null)
        {
            local = dao.savePullRequest(repo, toDaoModelPullRequest(remote, repo));
        }
        // maybe update
        if (hasChanged(local, remote) && remote != null)
        {
            dao.updatePullRequestInfo(local.getID(), remote.getTitle(), remote.getSource()
                    .getBranchName().getName(), remote.getDestination().getBranchName().getName(), remote.getStatus());
        }

        return local.getID();
    }

    private boolean hasChanged(RepositoryPullRequestMapping local, BitbucketPullRequest remote)
    {
        return !StringUtils.equals(local.getName(), remote.getTitle()) ||
               !StringUtils.equals(local.getLastStatus(), remote.getStatus()) ||
               !StringUtils.equals(local.getLastStatus(), RepositoryPullRequestMapping.Status.fromBbString(remote.getDescription()).name()) ||
               !StringUtils.equals(local.getDestinationBranch(), remote.getDestination().getBranchName().getName()) ||
               !StringUtils.equals(local.getSourceBranch(), remote.getSource().getBranchName().getName());
    }

    public void loadPullRequestCommits(Repository repo, PullRequestRemoteRestpoint pullRestpoint,
            int localPullRequestId, BitbucketPullRequestUpdateActivity activity, RepositoryPullRequestUpdateActivityMapping savedActivity, String commitsUrl)
    {
        try
        {
            Iterable<BitbucketPullRequestCommit> commitsIterator = pullRestpoint.getPullRequestCommits(commitsUrl);

            for (BitbucketPullRequestCommit commit : commitsIterator)
            {
                RepositoryCommitMapping localCommit = dao.getCommitByNode(repo, localPullRequestId, commit.getHash());
                if (localCommit == null)
                {
                    localCommit = saveCommit(repo, commit, null, localPullRequestId);
                    linkCommit(repo, localCommit, savedActivity);
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
            RepositoryPullRequestUpdateActivityMapping withActivity)
    {
        dao.linkCommit(domainRepository, withActivity, commitMapping);
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

    private HashMap<String, Object> getAsCommonProperties(BitbucketPullRequestBaseActivity activity, int repositoryId, Integer pullRequestId)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryPullRequestActivityMapping.LAST_UPDATED_ON, ClientUtils.extractActivityDate(activity));
        ret.put(RepositoryPullRequestActivityMapping.AUTHOR, activity.getUser().getUsername());
        ret.put(RepositoryPullRequestActivityMapping.RAW_AUTHOR, activity.getUser().getDisplayName());
        ret.put(RepositoryPullRequestActivityMapping.PULL_REQUEST_ID, pullRequestId);
        ret.put(RepositoryPullRequestActivityMapping.REPOSITORY_ID, repositoryId);
        return ret;
    }

    private Map<String, Object> toDaoModelPullRequest(BitbucketPullRequest request, Repository repository)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryPullRequestMapping.REMOTE_ID, request.getId());
        ret.put(RepositoryPullRequestMapping.NAME, request.getTitle());
        ret.put(RepositoryPullRequestMapping.URL, request.getLinks().getHtml().getHref());
        ret.put(RepositoryPullRequestMapping.TO_REPO_ID, repository.getId());

        ret.put(RepositoryPullRequestMapping.CREATED_ON, request.getCreatedOn());
        ret.put(RepositoryPullRequestMapping.DESTINATION_BRANCH, request.getDestination().getBranchName().getName());
        ret.put(RepositoryPullRequestMapping.SOURCE_BRANCH, request.getSource().getBranchName().getName());
        ret.put(RepositoryPullRequestMapping.DESTINATION_BRANCH, request.getDestination().getBranchName().getName());
        ret.put(RepositoryPullRequestMapping.SOURCE_BRANCH, repository.getId());
        // in case that fork has been deleted, the source repository is null
        if (request.getSource().getRepository() != null)
        {
            String sourceRepositoryUrl = request.getSource().getRepository().getLinks().getHtml().getHref();
            ret.put(RepositoryPullRequestMapping.SOURCE_URL, sourceRepositoryUrl);
        }

        return ret;
    }

    private Map<String, Object> toDaoModelActivity(BitbucketPullRequestBaseActivity activity, int repositoryId, Integer pullRequestId)
    {
        Map<String, Object> ret = getAsCommonProperties(activity, repositoryId, pullRequestId);

        if (activity instanceof BitbucketPullRequestCommentActivity)
        {
            BitbucketPullRequestCommentActivity commentActivity = (BitbucketPullRequestCommentActivity) activity;
            ret.put(RepositoryPullRequestActivityMapping.ENTITY_TYPE, RepositoryPullRequestCommentActivityMapping.class);
            ret.put(RepositoryPullRequestCommentActivityMapping.REMOTE_ID, new Long(commentActivity.getId()));
            if (commentActivity.getParent() != null)
            {
                ret.put(RepositoryPullRequestCommentActivityMapping.REMOTE_PARENT_ID, commentActivity.getParent().getId());
            }
            if (commentActivity.getContent() != null)
            {
                ret.put(RepositoryPullRequestCommentActivityMapping.MESSAGE, commentActivity.getContent().getRaw());
            }
            if (commentActivity.getInline() != null)
            {
                ret.put(RepositoryPullRequestCommentActivityMapping.FILE, commentActivity.getInline().getPath());
            }
        } else if (activity instanceof BitbucketPullRequestApprovalActivity)
        {
            ret.put(RepositoryPullRequestActivityMapping.ENTITY_TYPE, RepositoryPullRequestUpdateActivityMapping.class);
            ret.put(RepositoryPullRequestUpdateActivityMapping.STATUS, RepositoryPullRequestUpdateActivityMapping.Status.APPROVED);

        } else if (activity instanceof BitbucketPullRequestUpdateActivity)
        {
            ret.put(RepositoryPullRequestActivityMapping.ENTITY_TYPE, RepositoryPullRequestUpdateActivityMapping.class);
            ret.put(RepositoryPullRequestUpdateActivityMapping.STATUS, transformUpdateStatus((BitbucketPullRequestUpdateActivity) activity));
        }
        return ret;
    }

    private RepositoryPullRequestUpdateActivityMapping.Status transformUpdateStatus(BitbucketPullRequestUpdateActivity activity)
    {
        String status = activity.getStatus();
        if ("open".equals(status))
        {
            // we save all updates with status updated, first update will be updated to opened
            return RepositoryPullRequestUpdateActivityMapping.Status.UPDATED;
        }
        if ("update".equals(status))
        {
            return RepositoryPullRequestUpdateActivityMapping.Status.UPDATED;
        }
        if ("fulfilled".equals(status))
        {
            return RepositoryPullRequestUpdateActivityMapping.Status.MERGED;
        }
        if ("rejected".equals(status))
        {
            return RepositoryPullRequestUpdateActivityMapping.Status.DECLINED;
        }

        return null;
    }

    private boolean isUpdateActivity(BitbucketPullRequestBaseActivity activity)
    {
        return activity instanceof BitbucketPullRequestUpdateActivity
                && "open".equalsIgnoreCase(((BitbucketPullRequestUpdateActivity) activity).getStatus());
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public MessageKey<BitbucketSynchronizeActivityMessage> getKey()
    {
        return messagingService.get(BitbucketSynchronizeActivityMessage.class, KEY);
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
