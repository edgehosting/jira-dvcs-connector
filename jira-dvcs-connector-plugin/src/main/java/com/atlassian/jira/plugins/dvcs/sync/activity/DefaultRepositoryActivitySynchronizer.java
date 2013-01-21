package com.atlassian.jira.plugins.dvcs.sync.activity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientRemoteFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommentActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestLikeActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.util.IssueKeyExtractor;

public class DefaultRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{

    private final BitbucketClientRemoteFactory clientFactory;
    private final RepositoryActivityDao dao;

    public DefaultRepositoryActivitySynchronizer(BitbucketClientRemoteFactory clientFactory, RepositoryActivityDao dao)
    {
        super();
        this.clientFactory = clientFactory;
        this.dao = dao;
    }

    @Override
    public void synchronize(Repository forRepository)
    {
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
            Set<String> issueKeysFromActivity = extractIssueKeys(info.getActivity());
            if (!issueKeysFromActivity.isEmpty())
            {
                saveActivity(info, issueKeysFromActivity, forRepository, pullRestpoint);
            } else
            {
                // yami yami activity
            }
        }
    }

    private void saveActivity(BitbucketPullRequestActivityInfo info, Set<String> issueKeysFromActivity,
            Repository forRepository, PullRequestRemoteRestpoint pullRestpoint)
    {
        Integer pullRequestId = ensurePullRequestPresent(forRepository, pullRestpoint, info);
        for (String issueKey : issueKeysFromActivity)
        {
            dao.saveActivity(toDaoModel(info.getActivity(), issueKey, pullRequestId));
        }
    }

    private Integer ensurePullRequestPresent(Repository forRepository, PullRequestRemoteRestpoint pullRestpoint,
            BitbucketPullRequestActivityInfo info)
    {
        RepositoryPullRequestMapping pullRequest = dao.findRequestById(info.getPullRequest().getId(),
                forRepository.getSlug());

        if (pullRequest == null)
        {
            pullRequest = dao.savePullRequest(toDaoModelPullRequest(null));
        }

        return pullRequest.getID();
    }

    private Set<String> extractIssueKeys(BitbucketPullRequestBaseActivity activity)
    {
        Set<String> ret = new HashSet<String>();
        Iterable<String> messages = activity.getMessages();

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

    private Map<String, Object> toDaoModel(BitbucketPullRequestBaseActivity activity, String issueKey,
            Integer pullRequestId)
    {
        Map<String, Object> ret = getAsCommonProperties(activity, issueKey, pullRequestId);

        if (activity instanceof BitbucketPullRequestCommentActivity)
        {

            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, BitbucketPullRequestCommentActivity.class);

        } else if (activity instanceof BitbucketPullRequestLikeActivity)
        {

            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, BitbucketPullRequestLikeActivity.class);

        } else if (activity instanceof BitbucketPullRequestUpdateActivity)
        {

            ret.put(RepositoryActivityPullRequestMapping.ENTITY_TYPE, BitbucketPullRequestUpdateActivity.class);

        }
        return ret;
    }

    private HashMap<String, Object> getAsCommonProperties(BitbucketPullRequestBaseActivity activity, String issueKey,
            Integer pullRequestId)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryActivityPullRequestMapping.ISSUE_KEY, issueKey.toUpperCase());
        ret.put(RepositoryActivityPullRequestMapping.LAST_UPDATED_ON, activity.getUpdatedOn());
        ret.put(RepositoryActivityPullRequestMapping.INITIATOR_USERNAME, activity.getUser().getUsername());
        ret.put(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID, pullRequestId);
        ret.put(RepositoryActivityPullRequestMapping.REPO_SLUG, activity.getRepository().getSlug());
        return ret;
    }

    private Map<String, Object> toDaoModelPullRequest(BitbucketPullRequest request)
    {
        HashMap<String, Object> ret = new HashMap<String, Object>();
        ret.put(RepositoryPullRequestMapping.LOCAL_ID, request.getId());
        ret.put(RepositoryPullRequestMapping.PULL_REQUEST_NAME, request.getTitle());
        return ret;
    }

}
