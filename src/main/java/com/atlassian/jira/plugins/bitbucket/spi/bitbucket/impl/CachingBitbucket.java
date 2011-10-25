package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;

/**
 * A {@link BitbucketCommunicator} implementation that caches results for quicker subsequent lookup times
 */
public class CachingBitbucket implements BitbucketCommunicator
{
    private final BitbucketCommunicator delegate;

    private class ChangesetKey
    {
        final String id;
		private final SourceControlRepository repository;

        public ChangesetKey(SourceControlRepository repository, String id)
        {
            this.repository = repository;
            this.id = id;
        }

        @Override
		public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChangesetKey that = (ChangesetKey) o;
            if (!repository.equals(that.repository)) return false;
            if (!id.equals(that.id)) return false;
            return true;
        }

        @Override
		public int hashCode()
        {
            int result = repository.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }
    }

    private final Map<String, SourceControlUser> userMap = new MapMaker()
            .expiration(30, TimeUnit.MINUTES)
            .makeComputingMap(
                    new Function<String, SourceControlUser>()
                    {
                        public SourceControlUser apply(String key)
                        {
                            return delegate.getUser(key);
                        }
                    });

    private final Map<ChangesetKey, Changeset> changesetMap = new MapMaker()
            .expiration(30, TimeUnit.MINUTES)
            .makeComputingMap(
                    new Function<ChangesetKey, Changeset>()
                    {
                        public Changeset apply(ChangesetKey key)
                        {
                            return delegate.getChangeset(key.repository, key.id);
                        }
                    });


    public CachingBitbucket(BitbucketCommunicator delegate)
    {
        this.delegate = delegate;
    }

    public SourceControlUser getUser(String username)
    {
        try
        {
            return userMap.get(username);
        }
        catch (ComputationException e)
        {
            throw unrollException(e);
        }
    }

    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        try
        {
            return changesetMap.get(new ChangesetKey(repository, id));
        }
        catch (ComputationException e)
        {
            throw unrollException(e);
        }
    }

    public Iterable<Changeset> getChangesets(SourceControlRepository repository)
    {
        try
        {
            return delegate.getChangesets(repository);
        }
        catch (ComputationException e)
        {
            throw unrollException(e);
        }
    }

    private SourceControlException unrollException(ComputationException e)
    {
        return e.getCause() instanceof SourceControlException ? (SourceControlException) e.getCause() : new SourceControlException(e.getCause());
    }
}
