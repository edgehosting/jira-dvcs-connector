package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.util.json.JSONObject;

/**
 * {@link GitHubPullRequestSynchronizeMessage} message serializer.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestSynchronizeMessageSerializer extends AbstractMessagePayloadSerializer<GitHubPullRequestSynchronizeMessage>
{

    /**
     * Constructor.
     * 
     * @param repositoryService
     *            injected {@link RepositoryService} dependency
     * @param synchronizer
     *            injected {@link Synchronizer} dependency
     */
    public GitHubPullRequestSynchronizeMessageSerializer(RepositoryService repositoryService, Synchronizer synchronizer)
    {
        super(repositoryService, synchronizer);
    }

    @Override
    public Class<GitHubPullRequestSynchronizeMessage> getPayloadType()
    {
        return GitHubPullRequestSynchronizeMessage.class;
    }

    @Override
    protected void serializeInternal(JSONObject json, GitHubPullRequestSynchronizeMessage payload) throws Exception
    {
        json.put("pullRequestNumber", payload.getPullRequestNumber());
    }

    @Override
    protected GitHubPullRequestSynchronizeMessage deserializeInternal(JSONObject json, final int version) throws Exception
    {
        return new GitHubPullRequestSynchronizeMessage(null, 0, false, null, json.getInt("pullRequestNumber"));
    }

}
