package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A {@link com.atlassian.jira.plugins.bitbucket.spi.Communicator} implementation that caches results for quicker subsequent lookup times
 */
public class CachingCommunicator implements Communicator
{
    private final Communicator delegate;

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
        return delegate.getChangeset(repository, id);
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
    public UrlInfo getUrlInfo(RepositoryUri repositoryUri, String projectKey)
    {
        return delegate.getUrlInfo(repositoryUri, projectKey);
    }

    @Override
    public String getRepositoryName(String repositoryType, String projectKey, RepositoryUri repositoryUri,
        String adminUsername, String adminPassword, String accessToken) throws SourceControlException
    {
        return delegate.getRepositoryName(repositoryType, projectKey, repositoryUri, adminUsername, adminPassword, accessToken);
    }
}
