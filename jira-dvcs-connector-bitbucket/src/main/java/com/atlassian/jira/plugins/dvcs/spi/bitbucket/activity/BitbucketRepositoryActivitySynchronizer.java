package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommentActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.HasMessages;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.util.IssueKeyExtractor;

// TODO failure recovery + rename to stateful if will be stateful
public class BitbucketRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{

    private final BitbucketClientRemoteFactory clientFactory;
    private final RepositoryActivityDao dao;
    private final RepositoryDao repositoryDao;

    public BitbucketRepositoryActivitySynchronizer(BitbucketClientRemoteFactory clientFactory, RepositoryActivityDao dao,
            RepositoryDao repositoryDao)
    {
        this.clientFactory = clientFactory;
        this.dao = dao;
        this.repositoryDao = repositoryDao;
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

        Date lastActivitySyncDate = forRepository.getActivityLastSync();
        
        Date previousActivityDate = null;
        //
        // check whether there's some interesting issue keys in activity
        // and persist it if yes
        //
        try {
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
	            	PullRequestContext pullRequestContext = context.get(info.getPullRequest().getId());
	            	if (pullRequestContext == null)
	                {
	                    pullRequestContext = new PullRequestContext();
	                    context.put(info.getPullRequest().getId(), pullRequestContext);
	                }
	            	
	            	processActivity(info, forRepository, pullRestpoint, pullRequestContext);
	            }
	            previousActivityDate = activityDate;
	        }
	        
	        for ( Long pullRequestRemoteId : context.keySet() )
	        {
	        	PullRequestContext pullRequestContext = context.get(pullRequestRemoteId);
	            // when soft sync, it could happen that we have no update activities and therefore no last update activity and iterator
	            if (pullRequestContext.getLastUpdateActivityId() != null)
	            {	
		        	fillCommits(null, pullRequestContext);
		        	// there are no more commits, this activity must be the first
		        	if (!pullRequestContext.getCommitIterator().iterator().hasNext())
		        	{
		        		dao.updateActivityStatus(pullRequestContext.getLastUpdateActivityId(), RepositoryActivityPullRequestUpdateMapping.Status.OPENED);
		        		
		        	}
	            }
	        	// Extracting gathered 
	        	Set<String> issueKeys = extractIssueKeysFromCommits(pullRequestContext.getPullRequesCommitIds());
	    		updateIssueKeysMapping(pullRequestContext.getLocalPullRequestId(), issueKeys);
	        }
        } finally
        {
        	context.clear();
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
        
        updateIssueKeysMapping(localPullRequestId, extractIssueKeys(activity));
        
        RepositoryActivityPullRequestMapping mapping = dao.saveActivity(toDaoModel(info.getActivity(), forRepository, localPullRequestId));
        
        if (isUpdateActivity(activity))
        {
            if (pullRequestContext.getFirstCommit() != null)
            {
                pullRequestContext.setLastUpdateActivityId(mapping.getID());
            }
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
        // go for pull request details [***]
        BitbucketPullRequest remotePullRequest = pullRestpoint.getPullRequestDetail(forRepository.getOrgName(),
                forRepository.getSlug(), info.getPullRequest().getId() + "");

        RepositoryPullRequestMapping localPullRequest = dao.findRequestByRemoteId(forRepository.getId(),
                info.getPullRequest().getId());
        
        // extract keys from pull request
        Set<String> issueKeys = extractIssueKeys(remotePullRequest);
        
        // don't have this pull request, let's save it
        if (localPullRequest == null)
        {
            localPullRequest = dao.savePullRequest(toDaoModelPullRequest(remotePullRequest, forRepository));

          // already have it, let's find new issue keys
        } else
        {
        	// [***]
        	updateIssueKeysMapping(localPullRequest.getID(), issueKeys);
        }
        
        pullRequestContext.setLocalPullRequestId(localPullRequest.getID());
        
        // go for commits details [***]
        fillCommits(info, pullRestpoint, pullRequestContext);

        return pullRequestContext.getLocalPullRequestId();
    }

    private void updateIssueKeysMapping(int localPullRequestId, Set<String> issueKeys)
    {
    	Set<String> existingIssueKeysMapping = dao.getExistingIssueKeysMapping(localPullRequestId);

    	issueKeys.removeAll(existingIssueKeysMapping);
    	
        if (!issueKeys.isEmpty())
        {
            // FIXME - was removed - currently it is handled automatically
            // dao.saveIssueKeysMappings(issueKeys, localPullRequestId);
        }
    }
    
    private Set<String> extractIssueKeys(HasMessages messageProvider)
    {
        Set<String> ret = new HashSet<String>();
        Iterable<String> messages = messageProvider.getMessages();

        for (String message : messages)
        {
            Set<String> issueKeysFromMessage = IssueKeyExtractor.extractIssueKeys(message);
            if (!issueKeysFromMessage.isEmpty())
            {
                ret.addAll(issueKeysFromMessage);
            }
        }
        return ret;
    }

    private Set<String> extractIssueKeysFromCommits(List<Integer> pullRequesCommitIds)
    {
		Set<String> issueKeys = new HashSet<String>();
		if (pullRequesCommitIds != null)
		{
			for (Integer commitId : pullRequesCommitIds)
			{
				RepositoryActivityCommitMapping commit = dao.getCommit(commitId);
				if (commit != null)
				{
					issueKeys.addAll(IssueKeyExtractor.extractIssueKeys(commit.getMessage()));
				}
				
			}
		}
		return issueKeys;
	}
    
    private Map<String, Object> toDaoModel(BitbucketPullRequestBaseActivity activity, Repository forRepository, Integer pullRequestId)
    {
        Map<String, Object> ret = getAsCommonProperties(activity, forRepository, pullRequestId);

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
    
    private HashMap<String, Object> getAsCommonProperties(BitbucketPullRequestBaseActivity activity, Repository forRepository, Integer pullRequestId)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, ClientUtils.extractActivityDate(activity));
        ret.put(RepositoryActivityPullRequestMapping.AUTHOR, activity.getUser().getUsername());
        ret.put(RepositoryActivityPullRequestMapping.RAW_AUTHOR, activity.getUser().getDisplayName());
        ret.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, pullRequestId);
        ret.put(RepositoryActivityPullRequestMapping.REPOSITORY_ID, forRepository.getId());
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

    private Map<String,Object> toDaoModelCommit(BitbucketPullRequestCommit commit, int activityId)
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
    
    private class PullRequestContext
    {
        private Iterable<BitbucketPullRequestCommit> commitIterator;
        private BitbucketPullRequestCommit firstCommit;
        private Integer lastUpdateActivityId;
        private List<Integer> pullRequesCommitIds;
        private Integer localPullRequestId;
        
        public Iterable<BitbucketPullRequestCommit> getCommitIterator()
        {
            return commitIterator;
        }
        
        public void setCommitIterator(
                Iterable<BitbucketPullRequestCommit> commitIterator)
        {
            this.commitIterator = commitIterator;
        }
        
        public BitbucketPullRequestCommit getFirstCommit()
        {
            return firstCommit;
        }
        
        public void setFirstCommit(BitbucketPullRequestCommit firstCommit)
        {
            this.firstCommit = firstCommit;
        }

        public Integer getLastUpdateActivityId() {
            return lastUpdateActivityId;
        }

        public void setLastUpdateActivityId(Integer lastUpdateActivityId)
        {
            this.lastUpdateActivityId = lastUpdateActivityId;
        }

		public List<Integer> getPullRequesCommitIds()
		{
			if (pullRequesCommitIds == null)
			{
				pullRequesCommitIds = new ArrayList<Integer>();
			}
			return pullRequesCommitIds;
		}

		public void addPullRequesCommitId(Integer pullRequesCommitId)
		{
			getPullRequesCommitIds().add(pullRequesCommitId);
		}

		public Integer getLocalPullRequestId() {
			return localPullRequestId;
		}

		public void setLocalPullRequestId(Integer localPullRequestId) {
			this.localPullRequestId = localPullRequestId;
		}
    }
   
    private final Map<Long, PullRequestContext> context = new HashMap<Long, PullRequestContext>();
    
    private void fillCommits(BitbucketPullRequestUpdateActivity activity, PullRequestContext pullRequestContext)
    {
    	BitbucketPullRequestCommit firstCommit = pullRequestContext.getFirstCommit();
        
        // check for duplicate activity
        if (firstCommit != null)
        {
	        if (activity != null && firstCommit.getSha().startsWith(activity.getSource().getCommit().getSha()))
	        {
	            return;
	        } else
	        {
	        	pullRequestContext.addPullRequesCommitId(
	        			saveCommit(pullRequestContext.getLastUpdateActivityId(), firstCommit)
	        			.getID());
                pullRequestContext.setFirstCommit(null);
	        }
        }
    	
        Iterable<BitbucketPullRequestCommit> commitsIterator = pullRequestContext.getCommitIterator();
        // when soft sync, it could happen that we have no update activities and therefore no commit iterator
        if (commitsIterator != null)
        {
	        for (BitbucketPullRequestCommit bitbucketPullRequestCommit : commitsIterator)
	        {
	        	// checking whether commit is already assigned to previously synchronized activity and stop in this case
	        	RepositoryActivityCommitMapping localCommit = dao.getCommitByNode(pullRequestContext.getLocalPullRequestId(), bitbucketPullRequestCommit.getSha());
	            if (localCommit != null)
	            {
	            	break;
	            }
	        	if (activity != null && bitbucketPullRequestCommit.getSha().startsWith(activity.getSource().getCommit().getSha()))
	            {
	            	pullRequestContext.setFirstCommit(bitbucketPullRequestCommit);
	                break;
	            }
	
	            if (pullRequestContext.getLastUpdateActivityId() != null)
	            {
	            	pullRequestContext.addPullRequesCommitId(
	            			saveCommit(pullRequestContext.getLastUpdateActivityId(), bitbucketPullRequestCommit)
	            			.getID());
	            }
	        }
        }
    }
    
    private void fillCommits(BitbucketPullRequestActivityInfo activityInfo, PullRequestRemoteRestpoint pullRestpoint, PullRequestContext pullRequestContext)
    {
        if ( !isUpdateActivity(activityInfo.getActivity()))
        {
            return;
        }

        Iterable<BitbucketPullRequestCommit> commitsIterator = pullRequestContext.getCommitIterator();
        if (commitsIterator == null)
        {
            commitsIterator = pullRestpoint.getPullRequestCommits(activityInfo.getPullRequest().getLinks().getCommitsHref());
            pullRequestContext.setCommitIterator(commitsIterator);
        }
        
        fillCommits((BitbucketPullRequestUpdateActivity)activityInfo.getActivity(), pullRequestContext);

    }
    
    private RepositoryActivityCommitMapping saveCommit(Integer activityId, BitbucketPullRequestCommit commit)
    {
        return dao.saveCommit(toDaoModelCommit(commit,activityId));
    }
}
