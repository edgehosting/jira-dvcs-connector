package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.util.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * {@link GitHubPullRequestSynchronizeMessage} message serializer.
 *
 * @author Stanislav Dvorscak
 */
@Component
public class GitHubPullRequestSynchronizeMessageSerializer
        extends AbstractMessagePayloadSerializer<GitHubPullRequestSynchronizeMessage>
{

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
    protected GitHubPullRequestSynchronizeMessage deserializeInternal(JSONObject json, final int version)
            throws Exception
    {
        return new GitHubPullRequestSynchronizeMessage(null, 0, false, null, json.getInt("pullRequestNumber"), false);
    }

}
