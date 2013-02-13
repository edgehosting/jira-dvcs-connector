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
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestApprovalMapping;
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
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.HasPossibleUpdatedMessages;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.util.IssueKeyExtractor;

// TODO failure recovery + rename to stateful if will be stateful
public class BitbucketRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{

    private final BitbucketClientRemoteFactory clientFactory;
    private final RepositoryActivityDao dao;
    private final RepositoryDao repositoryDao;

    private Integer lastUpdateActivityId;
    private String lastCommitSha;
    
    public BitbucketRepositoryActivitySynchronizer(BitbucketClientRemoteFactory clientFactory, RepositoryActivityDao dao,
            RepositoryDao repositoryDao)
    {
        super();
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
        
        //
        // check whether there's some interesting issue keys in activity
        // and persist it if yes
        //
        for (BitbucketPullRequestActivityInfo info : activities)
        {
            processActivity(info, forRepository, pullRestpoint);
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
        }

        // { finally
        repositoryDao.setLastActivitySyncDate(forRepository.getId(), lastActivitySyncDate);
        commits.clear();
    }

    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------
    // Helpers ...
    // -------------------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------

    private void processActivity(BitbucketPullRequestActivityInfo info, Repository forRepository,
            PullRequestRemoteRestpoint pullRestpoint)
    {
        RepositoryPullRequestMapping localPullRequest = ensurePullRequestPresent(forRepository, pullRestpoint, info);
        Integer pullRequestId = localPullRequest.getID();
        BitbucketPullRequestBaseActivity activity = info.getActivity();
        
        RepositoryActivityPullRequestMapping mapping = dao.saveActivity(toDaoModel(info.getActivity(), forRepository, pullRequestId));
        
        if (isUpdateActivity(activity))
        {
    		String commitSha = ((BitbucketPullRequestUpdateActivity) activity).getSource().getCommit().getSha();
    		
    		if (info.getPullRequest().getCommitsDetails() != null)
    		{
        		for ( BitbucketPullRequestCommit commit : info.getPullRequest().getCommitsDetails())
        		{
        			if (commit.getSha().startsWith(lastCommitSha))
        			{
        				lastCommitSha = null;
        			}
        			
        			if (lastCommitSha==null)
        			{
        				if (commit.getSha().startsWith(commitSha))
        				{
        					lastCommitSha = commitSha;
        					break;
        				} else
        				{
        					saveCommit(mapping.getID(), commit);
        				}
        			}
        		}
    		}
    		
    		lastUpdateActivityId = mapping.getID();
    		lastCommitSha = commitSha;
        }
    }

    private boolean isUpdateActivity(BitbucketPullRequestBaseActivity activity)
    {
    	return activity instanceof BitbucketPullRequestUpdateActivity && "open".equals(((BitbucketPullRequestUpdateActivity) activity).getStatus());
    }
    
    // TODO improve performance here [***] , as this is gonna to call often 
    private RepositoryPullRequestMapping ensurePullRequestPresent(Repository forRepository,
            PullRequestRemoteRestpoint pullRestpoint, BitbucketPullRequestActivityInfo info)
    {
        RepositoryPullRequestMapping localPullRequest = dao.findRequestById(forRepository.getId(),
                info.getPullRequest().getId());

        // don't have this pull request, let's save it
        if (localPullRequest == null)
        {
            // go for pull request details [***]
            BitbucketPullRequest remotePullRequest = pullRestpoint.getPullRequestDetail(forRepository.getOrgName(),
                    forRepository.getSlug(), info.getPullRequest().getId() + "");

            info.setPullRequest(remotePullRequest);
            
            // go for commits details [***]
            fillCommits(info, pullRestpoint);
            
            //
            Set<String> issueKeys = extractIssueKeys(info);
            localPullRequest = dao.savePullRequest(toDaoModelPullRequest(remotePullRequest, issueKeys, forRepository), issueKeys);

          // already have it, let's find new issue keys
        } else if (info.getActivity() instanceof HasPossibleUpdatedMessages) {
            
            // go for pull request details [***]
            BitbucketPullRequest remotePullRequest = pullRestpoint.getPullRequestDetail(forRepository.getOrgName(),
                    forRepository.getSlug(), info.getPullRequest().getId() + "");
            
            info.setPullRequest(remotePullRequest);
            // go for commits details [***]
            fillCommits(info, pullRestpoint);
                        
            Set<String> issueKeys = extractIssueKeys(info);
            // [***]
            Set<String> existingIssueKeysMapping = dao.getExistingIssueKeysMapping(localPullRequest.getID());
            for (String possibleNewIssueKey : issueKeys)
            {
                if (existingIssueKeysMapping.contains(possibleNewIssueKey)) {
                    issueKeys.remove(possibleNewIssueKey);
                }
            }
            if (!issueKeys.isEmpty()) {
                dao.saveIssueKeysMappings(issueKeys, localPullRequest.getID());
            }
        }

        return localPullRequest;
    }

    private Set<String> extractIssueKeys(BitbucketPullRequestActivityInfo info)
    {
        Set<String> ret = new HashSet<String>();
        Iterable<String> messages = info.getMessages();

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

    private Map<String, Object> toDaoModel(BitbucketPullRequestBaseActivity activity, Repository forRepository, Integer pullRequestId)
    {
        Map<String, Object> ret = getAsCommonProperties(activity, forRepository, pullRequestId);

        if (activity instanceof BitbucketPullRequestCommentActivity)
        {
            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestCommentMapping.class);
            BitbucketPullRequestCommentActivity commentActivity = (BitbucketPullRequestCommentActivity) activity;
            if (commentActivity.getContent() != null)
            {
            	ret.put(RepositoryActivityPullRequestCommentMapping.MESSAGE, commentActivity.getContent().getRaw());
            }
            
        } else if (activity instanceof BitbucketPullRequestApprovalActivity)
        {
            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestApprovalMapping.class);

        } else if (activity instanceof BitbucketPullRequestUpdateActivity)
        {
            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
            ret.put(RepositoryActivityPullRequestUpdateMapping.STATUS, ((BitbucketPullRequestUpdateActivity) activity).getStatus());
        }
        return ret;
    }

    private HashMap<String, Object> getAsCommonProperties(BitbucketPullRequestBaseActivity activity, Repository forRepository, Integer pullRequestId)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, ClientUtils.extractActivityDate(activity));
        ret.put(RepositoryActivityPullRequestMapping.AUTHOR, activity.getUser().getUsername());
        ret.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, pullRequestId);
        ret.put(RepositoryActivityPullRequestMapping.REPOSITORY_ID, forRepository.getId());
        return ret;
    }

    private Map<String, Object> toDaoModelPullRequest(BitbucketPullRequest request, Set<String> issueKeys, Repository forRepo)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryPullRequestMapping.LOCAL_ID, request.getId());
        ret.put(RepositoryPullRequestMapping.NAME, request.getTitle());
        ret.put(RepositoryPullRequestMapping.URL, request.getLinks().getHtmlHref());
        ret.put(RepositoryPullRequestMapping.FOUND_ISSUE_KEY, !issueKeys.isEmpty());
        ret.put(RepositoryPullRequestMapping.TO_REPO_ID, forRepo.getId());
        
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
        ret.put(RepositoryActivityCommitMapping.COMMIT_URL, commit.getHref());
        
        return ret;
    }
    
    private Map<Integer, Iterable<BitbucketPullRequestCommit>> commits = new HashMap<Integer, Iterable<BitbucketPullRequestCommit>>();
	private BitbucketPullRequestCommit lastCommit;
    
    private void fillCommits(BitbucketPullRequestActivityInfo activityInfo, PullRequestRemoteRestpoint pullRestpoint)
    {
    	if (lastCommit != null)
    	{
    		System.out.println("First Commit:");
    		saveCommit(lastUpdateActivityId, lastCommit);
    		lastCommit = null;
    	}
        Iterable<BitbucketPullRequestCommit> commitsIterator = commits.get(activityInfo.getPullRequest().getId());
        if (commitsIterator == null)
        {
    		commitsIterator = pullRestpoint.getPullRequestCommits(activityInfo.getPullRequest().getLinks().getCommitsHref());
    		commits.put(activityInfo.getPullRequest().getId(),commitsIterator);
        }
        List<BitbucketPullRequestCommit> prCommits = new ArrayList<BitbucketPullRequestCommit>();
        if ( isUpdateActivity(activityInfo.getActivity()))
        {
	        for (BitbucketPullRequestCommit bitbucketPullRequestCommit : commitsIterator)
	        {
	        	if ( bitbucketPullRequestCommit.getSha().startsWith(((BitbucketPullRequestUpdateActivity)activityInfo.getActivity()).getSource().getCommit().getSha()))
	        	{
	        		if (lastUpdateActivityId!=null)
	        		{
	        			lastCommit = bitbucketPullRequestCommit;
	        		}
	        		break;
	        	}
	        	saveCommit(lastUpdateActivityId, bitbucketPullRequestCommit);
//	            prCommits.add(bitbucketPullRequestCommit);
	        }
        }
        if (!prCommits.isEmpty())
        {
        	activityInfo.getPullRequest().setCommitsDetails(prCommits);
        }
    }
    
    private void saveCommit(Integer activityId, BitbucketPullRequestCommit commit)
    {
    	System.out.println("Saving commit '"+commit.getSha()+"' for " + activityId);
    	dao.saveCommit(toDaoModelCommit(commit,activityId));
    }
}
