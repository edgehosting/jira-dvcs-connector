package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public class DefaultBitbucket implements Bitbucket
{
    private final Logger logger = LoggerFactory.getLogger(DefaultBitbucket.class);
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
        catch (BitbucketException e)
        {
            logger.debug("could not load user [ "+username+" ]");
            return BitbucketUser.UNKNOWN_USER;
        }
        catch (JSONException e)
        {
            logger.debug("could not load user [ "+username+" ]");
            return BitbucketUser.UNKNOWN_USER;
        }
    }

    public BitbucketRepository getRepository(Authentication auth, String owner, String slug)
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

    public Changeset getChangeset(Authentication auth, String owner, String slug, String id)
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

    public Iterable<Changeset> getChangesets(final Authentication auth, final String owner, final String slug)
    {
        return new Iterable<Changeset>()
        {
            public Iterator<Changeset> iterator()
            {
                return new BitbucketChangesetIterator(bitbucketConnection, auth, owner, slug);
            }
        };
    }
    
}
