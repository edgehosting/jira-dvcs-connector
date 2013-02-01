package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestLikeMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientRemoteFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommentActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestLikeActivity;
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
        Iterable<BitbucketPullRequestActivityInfo> activites = pullRestpoint.getRepositoryActivity(
                forRepository.getOrgName(), forRepository.getSlug(), forRepository.getActivityLastSync());

        //
        // check whether there's some interesting issue keys in activity
        // and persist it if yes
        //
        for (BitbucketPullRequestActivityInfo info : activites)
        {
            processActivity(info, forRepository, pullRestpoint);
        }

        // { finally
        repositoryDao.setLastActivitySyncDate(forRepository.getId(), new Date() /* ? TODO last success saved activity date ? */);
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

        dao.saveActivity(toDaoModel(info.getActivity(), pullRequestId));
    }

    // TODO improve performance here [***] , as this is gonna to call often 
    private RepositoryPullRequestMapping ensurePullRequestPresent(Repository forRepository,
            PullRequestRemoteRestpoint pullRestpoint, BitbucketPullRequestActivityInfo info)
    {
        RepositoryPullRequestMapping localPullRequest = dao.findRequestById(info.getPullRequest().getId(),
                forRepository.getSlug());

        // don't have this pull request, let's save it
        if (localPullRequest == null)
        {
            // go for pull request details [***]
            BitbucketPullRequest remotePullRequest = pullRestpoint.getPullRequestDetail(forRepository.getOrgName(),
                    forRepository.getSlug(), info.getPullRequest().getId() + "");

            // go for commits details [***]
            fillCommits(info, pullRestpoint);
            info.setPullRequest(remotePullRequest);
            //
            Set<String> issueKeys = extractIssueKeys(info);
            localPullRequest = dao.savePullRequest(toDaoModelPullRequest(remotePullRequest, issueKeys, forRepository), issueKeys);

          // already have it, let's find new issue keys
        } else if (info.getActivity() instanceof HasPossibleUpdatedMessages) {
            
            // go for pull request details [***]
            BitbucketPullRequest remotePullRequest = pullRestpoint.getPullRequestDetail(forRepository.getOrgName(),
                    forRepository.getSlug(), info.getPullRequest().getId() + "");
            // go for commits details [***]
            fillCommits(info, pullRestpoint);
            info.setPullRequest(remotePullRequest);
            
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

    private Map<String, Object> toDaoModel(BitbucketPullRequestBaseActivity activity, Integer pullRequestId)
    {
        Map<String, Object> ret = getAsCommonProperties(activity, pullRequestId);

        if (activity instanceof BitbucketPullRequestCommentActivity)
        {

            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestCommentMapping.class);

        } else if (activity instanceof BitbucketPullRequestLikeActivity)
        {

            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestLikeMapping.class);

        } else if (activity instanceof BitbucketPullRequestUpdateActivity)
        {

            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, RepositoryActivityPullRequestUpdateMapping.class);
            ret.put(RepositoryActivityPullRequestUpdateMapping.STATUS, ((BitbucketPullRequestUpdateActivity) activity).getStatus());

        }
        return ret;
    }

    private HashMap<String, Object> getAsCommonProperties(BitbucketPullRequestBaseActivity activity,
            Integer pullRequestId)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, extractDate(activity));
        ret.put(RepositoryActivityPullRequestMapping.INITIATOR_USERNAME, activity.getUser().getUsername());
        ret.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, pullRequestId);
        ret.put(RepositoryActivityPullRequestMapping.REPO_SLUG, activity.getRepository().getSlug());
        return ret;
    }

    private Date extractDate(BitbucketPullRequestBaseActivity activity)
    {
        Date date = activity.getUpdatedOn();
        
        // fallbacks - order depends
        if (date == null) {
            date = activity.getDate();
        }
        if (date == null) {
            date = activity.getCreatedOn();
        }
        
        return date;
    }

    private Map<String, Object> toDaoModelPullRequest(BitbucketPullRequest request, Set<String> issueKeys, Repository forRepo)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryPullRequestMapping.LOCAL_ID, request.getId());
        ret.put(RepositoryPullRequestMapping.PULL_REQUEST_NAME, request.getTitle());
        ret.put(RepositoryPullRequestMapping.FOUND_ISSUE_KEY, !issueKeys.isEmpty());
        ret.put(RepositoryPullRequestMapping.TO_REPO_SLUG, forRepo.getSlug());
        return ret;
    }

    private void fillCommits(BitbucketPullRequestActivityInfo activityInfo, PullRequestRemoteRestpoint pullRestpoint)
    {
        Iterable<BitbucketPullRequestCommit> commitsIterator = pullRestpoint.getPullRequestCommits(activityInfo
                .getPullRequest().getCommits().getHref());
        List<BitbucketPullRequestCommit> prCommits = new ArrayList<BitbucketPullRequestCommit>();
        for (BitbucketPullRequestCommit bitbucketPullRequestCommit : commitsIterator)
        {
            prCommits.add(bitbucketPullRequestCommit);
        }
        activityInfo.getPullRequest().setCommitsDetails(prCommits);
    }

}
