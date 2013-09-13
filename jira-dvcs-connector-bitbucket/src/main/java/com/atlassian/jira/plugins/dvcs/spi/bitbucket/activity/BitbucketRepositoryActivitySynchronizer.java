package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import org.jfree.util.Log;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestCommentActivityMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityMapping;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects.BitbucketPullRequestCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommentActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.dao.BitbucketPullRequestDao;

// TODO failure recovery + rename to stateful if will be stateful
public class BitbucketRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{

    private final BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    private final RepositoryActivityDao dao;
    private RepositoryDao repositoryDao;
    private final PullRequestContextManager pullRequestContextManager;

    public BitbucketRepositoryActivitySynchronizer(BitbucketClientBuilderFactory bitbucketClientBuilderFactory, RepositoryActivityDao dao,
                                                   RepositoryDao repositoryDao, BitbucketPullRequestDao pullRequestDao)
    {
        super();
        this.bitbucketClientBuilderFactory = bitbucketClientBuilderFactory;
        this.dao = dao;
        this.repositoryDao = repositoryDao;
        this.pullRequestContextManager = new PullRequestContextManager(pullRequestDao, dao);
    }

    @Override
    public void synchronize(Repository forRepository, Progress progress, boolean softSync)
    {
        if (!softSync)
        {
            dao.removeAll(forRepository);
            forRepository.setActivityLastSync(null);
        }

        int jiraCount = progress.getJiraCount();
        int pullRequestActivityCount = 0;
        
        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(forRepository).apiVersion(2).build();
        PullRequestRemoteRestpoint pullRestpoint = remoteClient.getPullRequestAndCommentsRemoteRestpoint();

        //
        // get activities iterator
        //
        Iterable<BitbucketPullRequestActivityInfo> activities = pullRestpoint.getRepositoryActivity(forRepository.getOrgName(),
                forRepository.getSlug(), forRepository.getActivityLastSync());

        pullRequestContextManager.clear(forRepository);

        Date lastActivitySyncDate = forRepository.getActivityLastSync();

        //
        // check whether there's some interesting issue keys in activity
        // and persist it if yes
        //
        try
        {
            for (BitbucketPullRequestActivityInfo info : activities)
            {
                if (progress.isShouldStop())
                {
                    break;
                }
                
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

                PullRequestContext pullRequestContext = pullRequestContextManager.getPullRequestContext(forRepository.getId(), info
                        .getPullRequest().getId());
                processActivity(info, forRepository, pullRestpoint, pullRequestContext);
                pullRequestContextManager.save(pullRequestContext);
                
                pullRequestActivityCount++;
                progress.inPullRequestProgress(pullRequestActivityCount, jiraCount);
            }

            for (PullRequestContext pullRequestContext : pullRequestContextManager.getPullRequestRequestContexts(forRepository.getId()))
            {
                // when soft sync, it could happen that we have no update activities and therefore no last update activity and iterator
                if (pullRequestContext.getLastUpdateActivity() != null)
                {
                    fillActivityCommits(forRepository, null, pullRequestContext);
                }

                jiraCount += dao.updatePullRequestIssueKeys(forRepository, pullRequestContext.getLocalPullRequestId());
                progress.inPullRequestProgress(pullRequestActivityCount, jiraCount);
            }
        } finally
        {
            pullRequestContextManager.clear(forRepository);
            repositoryDao.setLastActivitySyncDate(forRepository.getId(), lastActivitySyncDate);
        }
    }

    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------
    // Helpers ...
    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    private void processActivity(BitbucketPullRequestActivityInfo info, Repository forRepository, PullRequestRemoteRestpoint pullRestpoint,
            PullRequestContext pullRequestContext)
    {
        int localPullRequestId = ensurePullRequestPresent(forRepository, pullRestpoint, info, pullRequestContext);
        BitbucketPullRequestBaseActivity activity = info.getActivity();

        if (isUpdateActivity(activity))
        {
            pullRequestContext.setLastUpdateActivity((BitbucketPullRequestUpdateActivity) info.getActivity());
        } else
        {
            dao.saveActivity(forRepository, toDaoModel(info.getActivity(), forRepository.getId(), localPullRequestId));
        }
    }

    private boolean isUpdateActivity(BitbucketPullRequestBaseActivity activity)
    {
        return activity instanceof BitbucketPullRequestUpdateActivity
                && "open".equals(((BitbucketPullRequestUpdateActivity) activity).getStatus());
    }

    // TODO improve performance here [***] , as this is gonna to call often
    private int ensurePullRequestPresent(Repository forRepository, PullRequestRemoteRestpoint pullRestpoint,
            BitbucketPullRequestActivityInfo info, PullRequestContext pullRequestContext)
    {
        BitbucketPullRequest remotePullRequest = null;
        if (pullRequestContext.getLocalPullRequestId() == null)
        {
            // go for pull request details [***]
            try
            {
                remotePullRequest = pullRestpoint.getPullRequestDetail(forRepository.getOrgName(), forRepository.getSlug(), info
                    .getPullRequest().getId() + "");
                pullRequestContext.setCommitsUrl(remotePullRequest.getLinks().getCommits().getHref());
            } catch (Exception e)
            {
                Log.error("Could not retrieve pull request details", e);
                remotePullRequest = new BitbucketPullRequest();
                remotePullRequest.setId(info.getPullRequest().getId());
            }
        }

        RepositoryPullRequestMapping localPullRequest = dao.findRequestByRemoteId(forRepository, info.getPullRequest().getId());

        // don't have this pull request, let's save it
        if (localPullRequest == null)
        {
            localPullRequest = dao.savePullRequest(forRepository, toDaoModelPullRequest(remotePullRequest, forRepository));
        }

        pullRequestContext.setLocalPullRequestId(localPullRequest.getID());

        if (isUpdateActivity(info.getActivity()))
        {
            // if we have first update activity in this context, go for commits
            if (pullRequestContext.getLastUpdateActivity() == null)
            {
                pullRequestContextManager.loadPullRequestCommits(forRepository, pullRestpoint, localPullRequest, pullRequestContext);
            }
            fillActivityCommits(forRepository, (BitbucketPullRequestUpdateActivity) info.getActivity(), pullRequestContext);
        }

        return pullRequestContext.getLocalPullRequestId();
    }

    private Map<String, Object> toDaoModel(BitbucketPullRequestBaseActivity activity, int repositoryId, Integer pullRequestId)
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
        ret.put(RepositoryPullRequestMapping.URL, repository.getOrgHostUrl() + request.getLinks().getHtml().getHref());
        ret.put(RepositoryPullRequestMapping.TO_REPO_ID, repository.getId());
        // in case that fork has been deleted, the source repository is null
        if (request.getSource().getRepository() != null)
        {
            String sourceRepositoryUrl = repository.getOrgHostUrl() + request.getSource().getRepository().getLinks().getHtml().getHref();
            ret.put(RepositoryPullRequestMapping.SOURCE_URL, sourceRepositoryUrl);
        }

        return ret;
    }

    private void fillActivityCommits(Repository domainRepository, BitbucketPullRequestUpdateActivity activity,
            PullRequestContext pullRequestContext)
    {
        RepositoryPullRequestUpdateActivityMapping lastUpdateActivity = null;
        if (pullRequestContext.getNextNode() != null)
        {
            if (checkSourceCommitIsPresent(activity) && pullRequestContext.getNextNode().startsWith(activity.getSource().getCommit().getSha()))
            {
                return;
            } else
            {
                BitbucketPullRequestUpdateActivity lastRemoteUpdateActivity = pullRequestContext.getLastUpdateActivity();
                if (lastRemoteUpdateActivity != null)
                {
                    lastUpdateActivity = (RepositoryPullRequestUpdateActivityMapping) dao.saveActivity(
                            domainRepository,
                            toDaoModel(lastRemoteUpdateActivity, pullRequestContext.getRepositoryId(),
                                    pullRequestContext.getLocalPullRequestId()));
                }
            }
        }

        for (BitbucketPullRequestCommitMapping commit : pullRequestContextManager.getCommitIterator(pullRequestContext))
        {
            if (checkSourceCommitIsPresent(activity) && commit.getNode().startsWith(activity.getSource().getCommit().getSha()))
            {
                // we found first commit for the update activity
                pullRequestContext.setNextNode(commit.getNode());
                break;
            }

            if (lastUpdateActivity != null)
            {
                saveCommit(domainRepository, commit, lastUpdateActivity);
            }
        }

        // there are no more commits, this activity must be the first
        if (lastUpdateActivity != null && pullRequestContext.getNextNode() == null && !pullRequestContext.isExistingUpdateActivity())
        {
            dao.updateActivityStatus(domainRepository, lastUpdateActivity.getID(), RepositoryPullRequestUpdateActivityMapping.Status.OPENED);
        }
    }

    private boolean checkSourceCommitIsPresent(BitbucketPullRequestUpdateActivity activity)
    {
        return activity != null && activity.getSource() != null && activity.getSource().getCommit() != null;
    }

    private void saveCommit(Repository domainRepository, BitbucketPullRequestCommitMapping commit,
            RepositoryPullRequestUpdateActivityMapping lastUpdateActivity)
    {
        RepositoryCommitMapping commitMapping = dao.getCommit(domainRepository, commit.getLocalId());
        dao.linkCommit(domainRepository, lastUpdateActivity, commitMapping);
        commitMapping.save();
    }
}
