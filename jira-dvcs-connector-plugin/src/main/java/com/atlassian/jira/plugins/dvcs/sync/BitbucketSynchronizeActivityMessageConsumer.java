package com.atlassian.jira.plugins.dvcs.sync;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects.BitbucketPullRequestCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity.PullRequestContext;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity.PullRequestContextManager;
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
    public void onReceive(int messageId, BitbucketSynchronizeActivityMessage payload, String[] tags)
    {
        try
        {
            Repository repo = payload.getRepository();
            if (!payload.isSoftSync())
            {
                dao.removeAll(repo);
                repo.setActivityLastSync(null);
            }

            int jiraCount = payload.getProgress().getJiraCount();
            int pullRequestActivityCount = 0;

            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repo).apiVersion(2).build();
            PullRequestRemoteRestpoint pullRestpoint = remoteClient.getPullRequestAndCommentsRemoteRestpoint();
            BitbucketPullRequestBaseActivityEnvelope activityPage = pullRestpoint.getRepositoryActivityPage(payload.getPageUrl(),
                    repo.getOrgName(), repo.getSlug(), repo.getActivityLastSync());

            List<BitbucketPullRequestActivityInfo> infos = activityPage.getValues();
            Date lastActivitySyncDate = repo.getActivityLastSync();

            for (BitbucketPullRequestActivityInfo info : infos)
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

                payload.getProgress().inPullRequestProgress(++pullRequestActivityCount, jiraCount);
            }
        } catch (Exception e)
        {
            LOGGER.error("Failed to process " + payload.getPageUrl(), e);
            messagingService.fail(this, messageId);
        }

    }

    private void processActivity(BitbucketPullRequestActivityInfo info, Repository repo, PullRequestRemoteRestpoint pullRestpoint)
    {
        int localPullRequestId = ensurePullRequestPresent(repo, pullRestpoint, info);

        RepositoryActivityMapping savedActivity = dao.saveActivity(repo,
                toDaoModelActivity(info.getActivity(), repo.getId(), localPullRequestId));

        if (isUpdateActivity(info.getActivity()))
        {
            loadPullRequestCommits(repo, pullRestpoint, localPullRequestId, (BitbucketPullRequestUpdateActivity) info.getActivity(),
                    (RepositoryPullRequestUpdateActivityMapping) savedActivity, info.getPullRequest().getCommits().getHref());

        }
    }

    private int ensurePullRequestPresent(Repository repo, PullRequestRemoteRestpoint pullRestpoint, BitbucketPullRequestActivityInfo info)
    {
        BitbucketPullRequest remotePullRequest = null;
        Long remoteId = info.getPullRequest().getId();
        RepositoryPullRequestMapping storedRequest = dao.findRequestByRemoteId(repo, remoteId);

        if (storedRequest == null)
        {
            try
            {
                remotePullRequest = pullRestpoint.getPullRequestDetail(repo.getOrgName(), repo.getSlug(), info
                    .getPullRequest().getId() + "");
            } catch (Exception e)
            {
                Log.error("Could not retrieve pull request details", e);
                remotePullRequest = new BitbucketPullRequest();
                remotePullRequest.setId(info.getPullRequest().getId());
            }
        }

        RepositoryPullRequestMapping localPullRequest = dao.findRequestByRemoteId(repo, info.getPullRequest().getId());

        // don't have this pull request, let's save it
        if (localPullRequest == null)
        {
            localPullRequest = dao.savePullRequest(repo, toDaoModelPullRequest(remotePullRequest, repo));
        }

        return localPullRequest.getID();
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
            ret.put(RepositoryPullRequestUpdateActivityMapping.STATUS, transformStatus((BitbucketPullRequestUpdateActivity) activity));
        }
        return ret;
    }

    private RepositoryPullRequestUpdateActivityMapping.Status transformStatus(BitbucketPullRequestUpdateActivity activity)
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
