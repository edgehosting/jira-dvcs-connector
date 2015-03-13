package com.atlassian.jira.plugins.dvcs.spi.gitlab.message;

import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.util.json.JSONObject;

/**
 * {@link GitHubPullRequestSynchronizeMessage} message serializer.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitlabPullRequestSynchronizeMessageSerializer extends AbstractMessagePayloadSerializer<GitlabPullRequestSynchronizeMessage>
{

    @Override
    public Class<GitlabPullRequestSynchronizeMessage> getPayloadType()
    {
        return GitlabPullRequestSynchronizeMessage.class;
    }

    @Override
    protected void serializeInternal(JSONObject json, GitlabPullRequestSynchronizeMessage payload) throws Exception
    {
        json.put("pullRequestNumber", payload.getPullRequestNumber());
    }

    @Override
    protected GitlabPullRequestSynchronizeMessage deserializeInternal(JSONObject json, final int version) throws Exception
    {
        return new GitlabPullRequestSynchronizeMessage(null, 0, false, null, json.getInt("pullRequestNumber"), false);
    }

}
