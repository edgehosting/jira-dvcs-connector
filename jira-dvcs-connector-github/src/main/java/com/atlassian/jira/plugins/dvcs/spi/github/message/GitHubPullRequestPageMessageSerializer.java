package com.atlassian.jira.plugins.dvcs.spi.github.message;

import com.atlassian.jira.plugins.dvcs.service.message.AbstractMessagePayloadSerializer;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

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
        json.put("processedPullRequests", payload.getProcessedPullRequests());
    }

    @Override
    protected GitHubPullRequestPageMessage deserializeInternal(final JSONObject json, final int version)
            throws Exception
    {
        Set<Long> processedPullRequests = asSet(json.optJSONArray("processedPullRequests"));
        return new GitHubPullRequestPageMessage(null, 0, false, null, json.getInt("page"), json.getInt("pagelen"), processedPullRequests, false);
    }

    @Override
    public Class<GitHubPullRequestPageMessage> getPayloadType()
    {
        return GitHubPullRequestPageMessage.class;
    }

    protected Set<Long> asSet(JSONArray optJSONArray)
    {
        if (optJSONArray == null)
        {
            return null;
        }

        Set<Long> ret = new HashSet<Long>();

        for (int i = 0; i < optJSONArray.length(); i++)
        {

            ret.add(optJSONArray.optLong(i));
        }
        return ret;
    }
}
