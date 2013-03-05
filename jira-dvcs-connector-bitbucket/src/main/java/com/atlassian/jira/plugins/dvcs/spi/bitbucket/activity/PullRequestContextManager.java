package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects.BitbucketPullRequestContextMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketActivityUser;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.dao.BitbucketPullRequestDao;

public class PullRequestContextManager
{
    private final BitbucketPullRequestDao pullRequestDao;
    private final RepositoryActivityDao dao;
    
    public PullRequestContextManager(BitbucketPullRequestDao pullRequestDao, RepositoryActivityDao dao)
    {
        this.pullRequestDao = pullRequestDao;
        this.dao = dao;
    }
    
    public PullRequestContext getPullRequestContext(int repositoryId, long pullRequestRemoteId)
    {
        BitbucketPullRequestContextMapping contextMapping = pullRequestDao.getPulRequestContextForRemoteId(repositoryId, pullRequestRemoteId);
        
        PullRequestContext pullRequestContext = transformContext(repositoryId, contextMapping);

        if (pullRequestContext == null)
        {
            pullRequestContext = new PullRequestContext(repositoryId, pullRequestRemoteId, pullRequestDao);
        }
        
        return pullRequestContext;
    }
    
    public Collection<PullRequestContext> getPullRequestRequestContexts(int repositoryId)
    {
        List<PullRequestContext> contexts = new ArrayList<PullRequestContext>();
        for (BitbucketPullRequestContextMapping contextMapping : pullRequestDao.findAllPullRequestContexts(repositoryId))
        {
            contexts.add(transformContext(repositoryId, contextMapping));
        }
        return contexts;
    }
    
    private PullRequestContext transformContext(int repositoryId, BitbucketPullRequestContextMapping context)
    {
        if (context == null)
        {
            return null;
        }
        PullRequestContext pullRequestContext = new PullRequestContext(repositoryId, context.getRemotePullRequestId(), pullRequestDao);
        
        pullRequestContext.setNextNode(context.getNextCommit());
        pullRequestContext.setCommitsUrl(context.getCommitsUrl());
        pullRequestContext.setExistingUpdateActivity(context.isSavedUpdateActivity());
        pullRequestContext.setLocalPullRequestId(context.getLocalPullRequestId());
        
        BitbucketPullRequestUpdateActivity lastUpdateActivity = new BitbucketPullRequestUpdateActivity();
        lastUpdateActivity.setDate(context.getLastActivityDate());
        lastUpdateActivity.setStatus(context.getLastActivityStatus());
        BitbucketActivityUser user = new BitbucketActivityUser();
        user.setUsername(context.getLastActivityAuthor());
        user.setDisplayName(context.getLastActivityRawAuthor());
        lastUpdateActivity.setUser(user);
        
        if (context.getLastActivityDate() != null)
        {
            pullRequestContext.setLastUpdateActivity(lastUpdateActivity);
        }
        
        return pullRequestContext;
    }
    
    public void clear(Repository repository)
    {
        pullRequestDao.clearContextForRepository(repository.getId());
    }
    
    public void save(PullRequestContext pullRequestContext)
    {
        BitbucketPullRequestContextMapping contextMapping = pullRequestDao.getPulRequestContextForRemoteId(pullRequestContext.getRepositoryId(), pullRequestContext.getRemotePullRequestId());
                
        if (contextMapping == null)
        {
            pullRequestDao.saveContext(toDaoContext(pullRequestContext));
        } else
        {
            updatePullRequestContext(contextMapping, pullRequestContext);
        }
    }
    
    private void updatePullRequestContext(BitbucketPullRequestContextMapping contextMapping, PullRequestContext pullRequestContext)
    {
        contextMapping.setCommitsUrl(pullRequestContext.getCommitsUrl());
        contextMapping.setNextCommit(pullRequestContext.getNextNode());
        contextMapping.setRemotePullRequestId(pullRequestContext.getRemotePullRequestId());
        contextMapping.setRepositoryId(pullRequestContext.getRepositoryId());
        contextMapping.setSavedUpdateActivity(pullRequestContext.isExistingUpdateActivity());
        contextMapping.setLocalPullRequestId(pullRequestContext.getLocalPullRequestId());

        if (pullRequestContext.getLastUpdateActivity() != null)
        {
            BitbucketPullRequestUpdateActivity lastUpdateActivity = pullRequestContext.getLastUpdateActivity();
            contextMapping.setLastActivityAuthor(lastUpdateActivity.getUser().getUsername());
            contextMapping.setLastActivityDate(lastUpdateActivity.getDate());
            contextMapping.setLastActivityRawAuthor(lastUpdateActivity.getUser().getDisplayName());
            contextMapping.setLastActivityStatus(lastUpdateActivity.getStatus());
        }
        contextMapping.save();
    }

    private Map<String, Object> toDaoContext(PullRequestContext pullRequestContext)
    {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put(BitbucketPullRequestContextMapping.COMMITS_URL, pullRequestContext.getCommitsUrl());
        ret.put(BitbucketPullRequestContextMapping.LOCAL_PULL_REQUEST_ID, pullRequestContext.getLocalPullRequestId());
        ret.put(BitbucketPullRequestContextMapping.NEXT_COMMIT, pullRequestContext.getNextNode());
        ret.put(BitbucketPullRequestContextMapping.REMOTE_PULL_REQUEST_ID, pullRequestContext.getRemotePullRequestId());
        ret.put(BitbucketPullRequestContextMapping.REPOSITORY_ID, pullRequestContext.getRepositoryId());
        ret.put(BitbucketPullRequestContextMapping.SAVED_UPDATE_ACTIVITY, pullRequestContext.isExistingUpdateActivity());
        
        BitbucketPullRequestUpdateActivity lastUpdateActivity = pullRequestContext.getLastUpdateActivity();
        
        if (lastUpdateActivity != null)
        {
            ret.put(BitbucketPullRequestContextMapping.LAST_UPDATE_ACTIVITY_AUTHOR, lastUpdateActivity.getUser().getUsername());
            ret.put(BitbucketPullRequestContextMapping.LAST_UPDATE_ACTIVITY_DATE, ClientUtils.extractActivityDate(lastUpdateActivity));
            ret.put(BitbucketPullRequestContextMapping.LAST_UPDATE_ACTIVITY_RAW_AUTHOR, lastUpdateActivity.getUser().getDisplayName());
            ret.put(BitbucketPullRequestContextMapping.LAST_UPDATE_ACTIVITY_STATUS, lastUpdateActivity.getStatus());
        }
        
        return ret;
    }

    public void loadPullRequestCommits(PullRequestRemoteRestpoint pullRestpoint, RepositoryPullRequestMapping localPullRequest, PullRequestContext pullRequestContext)
    {
        Iterable<BitbucketPullRequestCommit> commitsIterator = pullRestpoint.getPullRequestCommits(pullRequestContext.getCommitsUrl());
        BitbucketPullRequestCommit lastCommit = null;
        pullRequestContext.setExistingUpdateActivity(false);
        for (BitbucketPullRequestCommit commit : commitsIterator)
        {
            // checking whether commit is already assigned to previously synchronized activity and stop in this case
            RepositoryActivityCommitMapping localCommit = dao.getCommitByNode(localPullRequest.getID(), commit.getSha());
            if (localCommit != null)
            {
                pullRequestContext.setExistingUpdateActivity(true);
                break;
            }
            
            if (lastCommit == null)
            {
                pullRequestContext.setNextNode(commit.getSha());
            } else
            {
               saveCommit(lastCommit, commit.getSha(), localPullRequest.getID());
            }
            lastCommit = commit;
        }

        saveCommit(lastCommit, null, localPullRequest.getID());
    }

    private void saveCommit(BitbucketPullRequestCommit commit, String nextNode, int localPullRequestId)
    {
        if (commit != null)
        {
            RepositoryActivityCommitMapping commitMapping = dao.saveCommit(toDaoModelCommit(commit, null));
            pullRequestDao.saveCommit(commitMapping.getID(), commit.getSha(), nextNode, localPullRequestId);
        }
    }
    
    private Map<String,Object> toDaoModelCommit(BitbucketPullRequestCommit commit, Integer activityId)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryActivityCommitMapping.ACTIVITY_ID , activityId);
        ret.put(RepositoryActivityCommitMapping.AUTHOR, commit.getAuthor().getUser().getUsername());
        ret.put(RepositoryActivityCommitMapping.RAW_AUTHOR, commit.getAuthor().getRaw());
        ret.put(RepositoryActivityCommitMapping.MESSAGE, commit.getMessage());
        ret.put(RepositoryActivityCommitMapping.NODE, commit.getSha());
        ret.put(RepositoryActivityCommitMapping.DATE, commit.getDate());
        
        return ret;
    }
}
