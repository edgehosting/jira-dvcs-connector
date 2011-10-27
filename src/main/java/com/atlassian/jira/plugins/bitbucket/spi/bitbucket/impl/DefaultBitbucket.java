package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketConnection;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public class DefaultBitbucket implements BitbucketCommunicator
{
    private final Logger logger = LoggerFactory.getLogger(DefaultBitbucket.class);
    private final BitbucketConnection bitbucketConnection;

    public DefaultBitbucket(BitbucketConnection bitbucketConnection)
    {
        this.bitbucketConnection = bitbucketConnection;
    }

    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        try
        {
            return BitbucketUserFactory.parse(new JSONObject(bitbucketConnection.getUser(repository, username)));
        }
        catch (com.atlassian.jira.plugins.bitbucket.api.SourceControlException e)
        {
            logger.debug("could not load user [ "+username+" ]");
            return SourceControlUser.UNKNOWN_USER;
        }
        catch (JSONException e)
        {
            logger.debug("could not load user [ "+username+" ]");
            return SourceControlUser.UNKNOWN_USER;
        }
    }

    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        try
        {
			return BitbucketChangesetFactory.parse(repository.getId(), new JSONObject(bitbucketConnection.getChangeset(repository, id)));
        }
        catch (JSONException e)
        {
            throw new SourceControlException("could not parse json result", e);
        }
    }

    public Iterable<Changeset> getChangesets(final SourceControlRepository repository)
    {
        return new Iterable<Changeset>()
        {
            public Iterator<Changeset> iterator()
            {
                return new BitbucketChangesetIterator(bitbucketConnection, repository);
            }
        };
    }
    
}
