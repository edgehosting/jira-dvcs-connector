package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

import java.util.*;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public class DefaultBitbucket implements Bitbucket
{
    private final BitbucketConnection bitbucketConnection;

    public DefaultBitbucket(BitbucketConnection bitbucketConnection)
    {
        this.bitbucketConnection = bitbucketConnection;
    }

    public BitbucketUser getUser(String username)
    {
        try
        {
            return DefaultBitbucketUser.parse(new JSONObject(bitbucketConnection.getUser(username)));
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse json object", e);
        }
    }

    public BitbucketRepository getRepository(BitbucketAuthentication auth, String owner, String slug)
    {
        try
        {
            return BitbucketRepositoryFactory.parse(new JSONObject(bitbucketConnection.getRepository(auth, owner, slug)));
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse json object", e);
        }
    }

    public BitbucketChangeset getChangeset(BitbucketAuthentication auth, String owner, String slug, String id)
    {
        try
        {
            return BitbucketChangesetFactory.parse(owner, slug, new JSONObject(bitbucketConnection.getChangeset(auth, owner, slug, id)));
        }
        catch (JSONException e)
        {
            throw new BitbucketException("could not parse json result", e);
        }
    }

    public Iterable<BitbucketChangeset> getChangesets(final BitbucketAuthentication auth, final String owner, final String slug)
    {
        return new Iterable<BitbucketChangeset>()
        {
            public Iterator<BitbucketChangeset> iterator()
            {
                return new BitbucketChangesetIterator(bitbucketConnection, auth, owner, slug);
            }
        };
    }

}
