package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketChangeset;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Factory for {@link BitbucketChangeset} implementations
 */
public class BitbucketChangesetFactory
{
    public static BitbucketChangeset parse(String owner, String slug, JSONObject json)
    {
        try
        {
            return new DefaultBitbucketChangeset(
                    owner, slug,
                    json.getString("node"),
                    json.getString("raw_author"),
                    json.getString("author"),
                    json.getString("timestamp"),
                    json.getString("raw_node"),
                    json.getString("branch"),
                    json.getString("message"),
                    json.getInt("revision")
            );
        }
        catch (JSONException e)
        {
            throw new BitbucketException("invalid json object", e);
        }
    }

    public static BitbucketChangeset load(Bitbucket bitbucket, BitbucketAuthentication auth,
                                          String owner, String slug, String node)
    {
        return bitbucket.getChangeset(auth, owner, slug, node);
    }
}
