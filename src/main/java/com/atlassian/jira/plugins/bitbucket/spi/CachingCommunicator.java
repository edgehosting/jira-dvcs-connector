package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;

/**
 * A {@link com.atlassian.jira.plugins.bitbucket.spi.Communicator} implementation that caches results for quicker subsequent lookup times
 */
public class CachingCommunicator implements Communicator
{
    private final Communicator delegate;

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

    private class UserKey
    {
        SourceControlRepository repository;
        String username;

        public UserKey(SourceControlRepository repository, String username)
        {
            this.repository = repository;
            this.username = username;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) return false;
            if (this == obj) return true;
            if (this.getClass() != obj.getClass()) return false;
            UserKey that = (UserKey) obj;
            return new EqualsBuilder().append(repository, that.repository).append(username, that.username).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 37).append(repository).append(username).toHashCode();
        }

    }

    private final Map<UserKey, SourceControlUser> userMap = new MapMaker().expiration(30, TimeUnit.MINUTES).makeComputingMap(
        new Function<UserKey, SourceControlUser>()
        {
            @Override
            public SourceControlUser apply(UserKey key)
            {
                return delegate.getUser(key.repository, key.username);
            }
        });

    private final Map<ChangesetKey, Changeset> changesetMap = new MapMaker().expiration(30, TimeUnit.MINUTES).makeComputingMap(
        new Function<ChangesetKey, Changeset>()
        {
            @Override
            public Changeset apply(ChangesetKey key)
            {
                return delegate.getChangeset(key.repository, key.id);
            }
        });

    public CachingCommunicator(Communicator delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        try
        {
            return userMap.get(new UserKey(repository, username));
        } catch (ComputationException e)
        {
            throw unrollException(e);
        }
    }

    @Override
    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        try
        {
            return changesetMap.get(new ChangesetKey(repository, id));
        } catch (ComputationException e)
        {
            throw unrollException(e);
        }
    }

    private SourceControlException unrollException(ComputationException e)
    {
        return e.getCause() instanceof SourceControlException ? (SourceControlException) e.getCause() : new SourceControlException(e
            .getCause());
    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        delegate.setupPostcommitHook(repo, postCommitUrl);
    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        delegate.removePostcommitHook(repo, postCommitUrl);
    }

    @Override
    public Iterable<Changeset> getChangesets(SourceControlRepository repository)
    {
        return delegate.getChangesets(repository);
    }

    @Override
    public UrlInfo getUrlInfo(RepositoryUri repositoryUri)
    {
        return delegate.getUrlInfo(repositoryUri);
    }

    @Override
    public void validateRepositoryAccess(String repositoryType, String projectKey, RepositoryUri repositoryUri, String username,
        String password, String adminUsername, String adminPassword, String accessToken) throws SourceControlException
    {
        delegate.validateRepositoryAccess(repositoryType, projectKey, repositoryUri, username, password, adminUsername, adminPassword, accessToken);
    }
}
