package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Authentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketUser;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

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

    public Changeset getChangeset(String repositoryId, Authentication auth, String id)
    {
        try
        {
        	RepositoryUri uri = RepositoryUri.parse(repositoryId);
            return BitbucketChangesetFactory.parse(uri.getRepositoryUrl(), new JSONObject(bitbucketConnection.getChangeset(auth, uri.getOwner(), uri.getSlug(), id)));
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
