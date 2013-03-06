package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientRemoteFactory;
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

    private final BitbucketClientRemoteFactory clientFactory;
    private final RepositoryActivityDao dao;
    private final RepositoryDao repositoryDao;
    private final PullRequestContextManager pullRequestContextManager;
    
    public BitbucketRepositoryActivitySynchronizer(BitbucketClientRemoteFactory clientFactory, RepositoryActivityDao dao,
            RepositoryDao repositoryDao, BitbucketPullRequestDao pullRequestDao)
    {
        super();
        this.clientFactory = clientFactory;
        this.dao = dao;
        this.repositoryDao = repositoryDao;
        this.pullRequestContextManager = new PullRequestContextManager(pullRequestDao, dao);
    }

    @Override
    public void synchronize(Repository forRepository, boolean softSync)
    {
        if (!softSync)
        {
            dao.removeAll(forRepository);
            forRepository.setActivityLastSync(null);
        }

        BitbucketRemoteClient remoteClient = clientFactory.getForRepository(forRepository, 2);
        PullRequestRemoteRestpoint pullRestpoint = remoteClient.getPullRequestAndCommentsRemoteRestpoint();

        //
        // get activities iterator
        //
        Iterable<BitbucketPullRequestActivityInfo> activities = pullRestpoint.getRepositoryActivity(
                forRepository.getOrgName(), forRepository.getSlug(), forRepository.getActivityLastSync());

        pullRequestContextManager.clear(forRepository);
        
        Date lastActivitySyncDate = forRepository.getActivityLastSync();
        
        Date previousActivityDate = null;
        //
        // check whether there's some interesting issue keys in activity
        // and persist it if yes
        //
        try
        {
            for (BitbucketPullRequestActivityInfo info : activities)
            {
                Date activityDate = ClientUtils.extractActivityDate(info.getActivity());
                if (lastActivitySyncDate == null)
                {
                    lastActivitySyncDate = activityDate;
                } else
                {
                    if (activityDate!=null && activityDate.after(lastActivitySyncDate))
                    {
                        lastActivitySyncDate = activityDate;
                    }
                }
                
                // filtering duplicated activities in response
                //TODO implement better checking whether activity is duplicated than comparing dates
                if (!activityDate.equals(previousActivityDate))
                {
                    PullRequestContext pullRequestContext = pullRequestContextManager.getPullRequestContext(forRepository.getId(), info.getPullRequest().getId());
                    
                    processActivity(info, forRepository, pullRestpoint, pullRequestContext);
                    
                    pullRequestContextManager.save(pullRequestContext);
                }
                previousActivityDate = activityDate;
            }
            
            for ( PullRequestContext pullRequestContext : pullRequestContextManager.getPullRequestRequestContexts(forRepository.getId()) )
            {
                // when soft sync, it could happen that we have no update activities and therefore no last update activity and iterator
                if (pullRequestContext.getLastUpdateActivity() != null)
                {    
                    fillActivityCommits(null, pullRequestContext);
                }

                dao.updatePullRequestIssueKeys(pullRequestContext.getLocalPullRequestId());
            }
        } finally
        {
            pullRequestContextManager.clear(forRepository);
        }

        // { finally
        repositoryDao.setLastActivitySyncDate(forRepository.getId(), lastActivitySyncDate);
    }

    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------
    // Helpers ...
    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    private void processActivity(BitbucketPullRequestActivityInfo info, Repository forRepository,
            PullRequestRemoteRestpoint pullRestpoint, PullRequestContext pullRequestContext)
    {
        int localPullRequestId = ensurePullRequestPresent(forRepository, pullRestpoint, info, pullRequestContext);
        BitbucketPullRequestBaseActivity activity = info.getActivity();
        
        if (isUpdateActivity(activity))
        {
            pullRequestContext.setLastUpdateActivity((BitbucketPullRequestUpdateActivity)info.getActivity());
        } else
        {
            dao.saveActivity(toDaoModel(info.getActivity(), forRepository.getId(), localPullRequestId));
        }
    }

    private boolean isUpdateActivity(BitbucketPullRequestBaseActivity activity)
    {
        return activity instanceof BitbucketPullRequestUpdateActivity && "open".equals(((BitbucketPullRequestUpdateActivity) activity).getStatus());
    }
    
    // TODO improve performance here [***] , as this is gonna to call often 
    private int ensurePullRequestPresent(Repository forRepository,
            PullRequestRemoteRestpoint pullRestpoint, BitbucketPullRequestActivityInfo info, PullRequestContext pullRequestContext)
    {
        BitbucketPullRequest remotePullRequest = null;
        if (pullRequestContext.getLocalPullRequestId() == null)
        {
            // go for pull request details [***]
            remotePullRequest = pullRestpoint.getPullRequestDetail(forRepository.getOrgName(),
                    forRepository.getSlug(), info.getPullRequest().getId() + "");
            pullRequestContext.setCommitsUrl(remotePullRequest.getLinks().getCommitsHref());
        }
            
        RepositoryPullRequestMapping localPullRequest = dao.findRequestByRemoteId(forRepository.getId(), info.getPullRequest().getId());
        
        // don't have this pull request, let's save it
        if (localPullRequest == null)
        {
            localPullRequest = dao.savePullRequest(toDaoModelPullRequest(remotePullRequest, forRepository));
        }
        
        pullRequestContext.setLocalPullRequestId(localPullRequest.getID());

        if (isUpdateActivity(info.getActivity()))
        {
            // if we have first update activity in this context, go for commits
            if (pullRequestContext.getLastUpdateActivity() == null)
            {
                pullRequestContextManager.loadPullRequestCommits(pullRestpoint, localPullRequest, pullRequestContext);
            }
            fillActivityCommits((BitbucketPullRequestUpdateActivity)info.getActivity(), pullRequestContext);
        }

        return pullRequestContext.getLocalPullRequestId();
    }
        
    private Map<String, Object> toDaoModel(BitbucketPullRequestBaseActivity activity, int repositoryId, Integer pullRequestId)
    {
        Map<String, Object> ret = getAsCommonProperties(activity, repositoryId, pullRequestId);

        if (activity instanceof BitbucketPullRequestCommentActivity)
        {
            BitbucketPullRequestCommentActivity commentActivity = (BitbucketPullRequestCommentActivity) activity;
            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestCommentMapping.class);
            ret.put(RepositoryActivityPullRequestCommentMapping.REMOTE_ID,  new Long(commentActivity.getId()));
            if (commentActivity.getParent() != null)
            {
                ret.put(RepositoryActivityPullRequestCommentMapping.REMOTE_PARENT_ID, commentActivity.getParent().getId());
            }
            if (commentActivity.getContent() != null)
            {
                ret.put(RepositoryActivityPullRequestCommentMapping.MESSAGE, commentActivity.getContent().getRaw());
            }
            if (commentActivity.getInline() != null)
            {
                ret.put(RepositoryActivityPullRequestCommentMapping.FILE, commentActivity.getInline().getPath());
            }
        } else if (activity instanceof BitbucketPullRequestApprovalActivity)
        {
            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
            ret.put(RepositoryActivityPullRequestUpdateMapping.STATUS, RepositoryActivityPullRequestUpdateMapping.Status.APPROVED);

        } else if (activity instanceof BitbucketPullRequestUpdateActivity)
        {
            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
            ret.put(RepositoryActivityPullRequestUpdateMapping.STATUS, transformStatus((BitbucketPullRequestUpdateActivity) activity));
        }
        return ret;
    }

    private RepositoryActivityPullRequestUpdateMapping.Status transformStatus(BitbucketPullRequestUpdateActivity activity)
    {
        String status = activity.getStatus();
        if ("open".equals(status))
        {
            // we save all updates with status updated, first update will be updated to opened
            return RepositoryActivityPullRequestUpdateMapping.Status.UPDATED;
        }
        if ("update".equals(status))
        {
            return RepositoryActivityPullRequestUpdateMapping.Status.UPDATED;
        }
        if ("fulfilled".equals(status))
        {
            return RepositoryActivityPullRequestUpdateMapping.Status.MERGED;
        }
        if ("rejected".equals(status))
        {
            return RepositoryActivityPullRequestUpdateMapping.Status.DECLINED;
        }
        
        return null;
    }
    
    private HashMap<String, Object> getAsCommonProperties(BitbucketPullRequestBaseActivity activity, int repositoryId, Integer pullRequestId)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, ClientUtils.extractActivityDate(activity));
        ret.put(RepositoryActivityPullRequestMapping.AUTHOR, activity.getUser().getUsername());
        ret.put(RepositoryActivityPullRequestMapping.RAW_AUTHOR, activity.getUser().getDisplayName());
        ret.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, pullRequestId);
        ret.put(RepositoryActivityPullRequestMapping.REPOSITORY_ID, repositoryId);
        return ret;
    }

    private Map<String, Object> toDaoModelPullRequest(BitbucketPullRequest request, Repository repository)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryPullRequestMapping.REMOTE_ID, request.getId());
        ret.put(RepositoryPullRequestMapping.NAME, request.getTitle());
        ret.put(RepositoryPullRequestMapping.URL, repository.getOrgHostUrl() + request.getLinks().getHtmlHref());
        ret.put(RepositoryPullRequestMapping.TO_REPO_ID, repository.getId());
        // in case that fork has been deleted, the source repository is null 
        if (request.getSource().getRepository() != null)
        {
            String sourceRepositoryUrl = repository.getOrgHostUrl() + request.getSource().getRepository().getLinks().getHtmlHref();
            ret.put(RepositoryPullRequestMapping.SOURCE_URL, sourceRepositoryUrl);
        }
        
        return ret;
    }

    private void fillActivityCommits(BitbucketPullRequestUpdateActivity activity, PullRequestContext pullRequestContext)
    {
        RepositoryActivityPullRequestUpdateMapping lastUpdateActivity = null;
        if (pullRequestContext.getNextNode() != null)
        {
            if (activity != null && pullRequestContext.getNextNode().startsWith(activity.getSource().getCommit().getSha()))
            {
                return;
            } else
            {
                BitbucketPullRequestUpdateActivity lastRemoteUpdateActivity = pullRequestContext.getLastUpdateActivity();
                if (lastRemoteUpdateActivity != null)
                {
                    lastUpdateActivity = (RepositoryActivityPullRequestUpdateMapping)dao.saveActivity(toDaoModel(lastRemoteUpdateActivity, pullRequestContext.getRepositoryId(), pullRequestContext.getLocalPullRequestId()));
                }
            }
        }
        
        for (BitbucketPullRequestCommitMapping commit : pullRequestContextManager.getCommitIterator(pullRequestContext))
        {
            if (activity != null && commit.getNode().startsWith(activity.getSource().getCommit().getSha()))
            {
                // we found first commit for the update activity
                pullRequestContext.setNextNode(commit.getNode());
                break;
            }
    
            if (lastUpdateActivity != null)
            {
                saveCommit(commit, lastUpdateActivity);
            }
        }
        
        // there are no more commits, this activity must be the first
        if (lastUpdateActivity != null && pullRequestContext.getNextNode() == null && !pullRequestContext.isExistingUpdateActivity())
        {
            dao.updateActivityStatus(lastUpdateActivity.getID(), RepositoryActivityPullRequestUpdateMapping.Status.OPENED);
        }
    }
    
    private void saveCommit(BitbucketPullRequestCommitMapping commit, RepositoryActivityPullRequestUpdateMapping lastUpdateActivity)
    {
        RepositoryActivityCommitMapping commitMapping = dao.getCommit(commit.getLocalId());
        commitMapping.setActivity(lastUpdateActivity);
        commitMapping.save();
    }
}
