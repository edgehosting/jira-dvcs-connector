package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.*;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Bitbucket} implementation that caches results for quicker subsequent lookup times
 */
public class CachingBitbucket implements Bitbucket
{
    private final Bitbucket delegate;

    private class ChangesetKey
    {
        final Authentication auth;
        final String owner;
        final String slug;
        final String id;

        public ChangesetKey(Authentication auth, String owner, String slug, String id)
        {
            this.auth = auth;
            this.owner = owner;
            this.slug = slug;
            this.id = id;
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChangesetKey that = (ChangesetKey) o;
            if (!auth.equals(that.auth)) return false;
            if (!id.equals(that.id)) return false;
            if (!owner.equals(that.owner)) return false;
            if (!slug.equals(that.slug)) return false;
            return true;
        }

        public int hashCode()
        {
            int result = auth.hashCode();
            result = 31 * result + owner.hashCode();
            result = 31 * result + slug.hashCode();
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

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChangesetKey that = (ChangesetKey) o;
            if (!auth.equals(that.auth)) return false;
            if (!owner.equals(that.owner)) return false;
            if (!slug.equals(that.slug)) return false;
            return true;
        }

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
                            return delegate.getChangeset(key.auth, key.owner, key.slug, key.id);
                        }
                    });

    private final Map<RepositoryKey, BitbucketRepository> repositoryMap = new MapMaker()
            .expiration(1, TimeUnit.HOURS)
            .makeComputingMap(
                    new Function<RepositoryKey, BitbucketRepository>()
                    {
                        public BitbucketRepository apply(RepositoryKey key)
                        {
                            return delegate.getRepository(key.auth, key.owner, key.slug);
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

    public BitbucketRepository getRepository(Authentication auth, String owner, String slug)
    {
        try
        {
            return repositoryMap.get(new RepositoryKey(auth, owner, slug));
        }
        catch (ComputationException e)
        {
            throw unrollException(e);
        }
    }

    public Changeset getChangeset(Authentication auth, String owner, String slug, String id)
    {
        try
        {
            return changesetMap.get(new ChangesetKey(auth, owner, slug, id));
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
