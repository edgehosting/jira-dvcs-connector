package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.google.common.base.Function;
import com.google.common.collect.ComputationException;
import com.google.common.collect.MapMaker;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A {@link com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator} implementation that caches results for quicker subsequent lookup times
 */
public class CachingCommunicator implements CachingDvcsCommunicator
{
    private final DvcsCommunicator delegate;

    private class UserKey
    {
        Repository repository;
        String username;

        public UserKey(Repository repository, String username)
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
            return new EqualsBuilder().append(repository.getOrgHostUrl(), that.repository.getOrgHostUrl()).append(username, that.username).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 37).append(repository.getOrgHostUrl()).append(username).toHashCode();
        }

    }

    private final Map<UserKey, DvcsUser> userMap = new MapMaker().expiration(30, TimeUnit.MINUTES).makeComputingMap(
        new Function<UserKey, DvcsUser>()
        {
            @Override
            public DvcsUser apply(UserKey key)
            {
                return delegate.getUser(key.repository, key.username);
            }
        });

    public CachingCommunicator(DvcsCommunicator delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public DvcsUser getUser(Repository repository, String username)
    {
        try
        {
            return userMap.get(new UserKey(repository, username));
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
    public String getUserUrl(Repository repository, Changeset changeset)
    {
        return delegate.getUserUrl(repository, changeset);
    }

    @Override
    public boolean isOauthConfigured()
    {
        return delegate.isOauthConfigured();
    }

    @Override
    public List<Group> getGroupsForOrganization(Organization organization)
    {
        return delegate.getGroupsForOrganization(organization);
    }

    @Override
    public boolean supportsInvitation(Organization organization)
    {
        return delegate.supportsInvitation(organization);
    }

    @Override
    public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
    {
        delegate.inviteUser(organization, groupSlugs, userEmail);
    }

    @Override
    public String getDvcsType()
    {
        return delegate.getDvcsType();
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        return delegate.getAccountInfo(hostUrl, accountName);
    }

    @Override
    public List<Repository> getRepositories(Organization organization)
    {
        return delegate.getRepositories(organization);
    }

    @Override
    public Changeset getDetailChangeset(Repository repository, Changeset changeset)
    {
        return delegate.getDetailChangeset(repository, changeset);
    }

    @Override
    public Iterable<Changeset> getChangesets(Repository repository, Date lastCommitDate)
    {
        return delegate.getChangesets(repository, lastCommitDate);
    }

    @Override
    public void setupPostcommitHook(Repository repository, String postCommitUrl)
    {
        delegate.setupPostcommitHook(repository, postCommitUrl);
    }

    @Override
    public void removePostcommitHook(Repository repository, String postCommitUrl)
    {
        delegate.removePostcommitHook(repository, postCommitUrl);
    }

    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        return delegate.getCommitUrl(repository, changeset);
    }

    @Override
    public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index)
    {
        return delegate.getFileCommitUrl(repository, changeset, file, index);
    }
}
