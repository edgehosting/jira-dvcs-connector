package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Authentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketException;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketUser;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;

/**
 * A {@link Bitbucket} implementation that caches results for quicker subsequent lookup times
 */
public class CachingBitbucket implements Bitbucket
{
    private final Bitbucket delegate;

    private class ChangesetKey
    {
        final Authentication auth;
        final String id;
		private final String repositoryUrl;

        public ChangesetKey(String repositoryUrl, Authentication auth, String id)
        {
            this.repositoryUrl = repositoryUrl;
			this.auth = auth;
            this.id = id;
        }

        @Override
		public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChangesetKey that = (ChangesetKey) o;
            if (!auth.equals(that.auth)) return false;
            if (!id.equals(that.id)) return false;
            if (!repositoryUrl.equals(that.repositoryUrl)) return false;
            return true;
        }

        @Override
		public int hashCode()
        {
            int result = auth.hashCode();
            result = 31 * result + repositoryUrl.hashCode();
            result = 31 * result + id.hashCode();
            return result;
        }
    }

    private class RepositoryKey
    {
        final Authentication auth;
        final String owner;
        final String slug;

        public RepositoryKey(Authentication auth, String owner, String slug)
        {
            this.auth = auth;
            this.owner = owner;
            this.slug = slug;
        }

        @Override
		public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RepositoryKey that = (RepositoryKey) o;
            if (!auth.equals(that.auth)) return false;
            if (!owner.equals(that.owner)) return false;
            if (!slug.equals(that.slug)) return false;
            return true;
        }

        @Override
		public int hashCode()
        {
            int result = auth.hashCode();
            result = 31 * result + owner.hashCode();
            result = 31 * result + slug.hashCode();
            return result;
        }
    }

    private final Map<String, BitbucketUser> userMap = new MapMaker()
            .expiration(30, TimeUnit.MINUTES)
            .makeComputingMap(
                    new Function<String, BitbucketUser>()
                    {
                        public BitbucketUser apply(String key)
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
                            return delegate.getChangeset(key.repositoryUrl, key.auth, key.id);
                        }
                    });


    public CachingBitbucket(Bitbucket delegate)
    {
        this.delegate = delegate;
    }

    public BitbucketUser getUser(String username)
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

    public Changeset getChangeset(String repositoryUrl, Authentication auth, String id)
    {
        try
        {
            return changesetMap.get(new ChangesetKey(repositoryUrl, auth, id));
        }
        catch (ComputationException e)
        {
            throw unrollException(e);
        }
    }

    public Iterable<Changeset> getChangesets(Authentication auth, String owner, String slug)
    {
        try
        {
            return delegate.getChangesets(auth, owner, slug);
        }
        catch (ComputationException e)
        {
            throw unrollException(e);
        }
    }

    private BitbucketException unrollException(ComputationException e)
    {
        return e.getCause() instanceof BitbucketException ? (BitbucketException) e.getCause() : new BitbucketException(e.getCause());
    }
}
