package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.util.json.JSONObject;

/**
 * {@link GitHubPullRequestPageMessage} message serializer.
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 *
 */
public class GitHubPullRequestPageMessageSerializer extends AbstractMessagePayloadSerializer<GitHubPullRequestPageMessage>
{
    @Override
    protected void serializeInternal(final JSONObject json, final GitHubPullRequestPageMessage payload) throws Exception
    {
        json.put("page", payload.getPage());
        json.put("pagelen", payload.getPagelen());
    }

    @Override
    protected GitHubPullRequestPageMessage deserializeInternal(final JSONObject json, final int version)
            throws Exception
    {
        return new GitHubPullRequestPageMessage(null, 0, false, null, json.getInt("page"), json.getInt("pagelen"));
    }

    @Override
    public Class<GitHubPullRequestPageMessage> getPayloadType()
    {
        return GitHubPullRequestPageMessage.class;
    }
}
