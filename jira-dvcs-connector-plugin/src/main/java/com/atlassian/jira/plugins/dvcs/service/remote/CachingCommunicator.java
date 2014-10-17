package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheException;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetailsEnvelope;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * A {@link com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator} implementation that caches results for
 * quicker subsequent lookup times
 */
@Component
public class CachingCommunicator implements CachingDvcsCommunicator
{
    @VisibleForTesting
    static class UserKey implements Serializable
    {
        private final Repository repository;
        private final String username;

        public UserKey(Repository repository, String username)
        {
            this.repository = repository;
            this.username = username;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
            { return false; }
            if (this == obj)
            { return true; }
            if (this.getClass() != obj.getClass())
            { return false; }
            UserKey that = (UserKey) obj;
            return new EqualsBuilder().append(repository.getOrgHostUrl(), that.repository.getOrgHostUrl()).append(username, that.username)
                    .isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 37).append(repository.getOrgHostUrl()).append(username).toHashCode();
        }

    }

    @VisibleForTesting
    static class OrganisationKey implements Serializable
    {
        private final Organization organization;

        public OrganisationKey(Organization organization)
        {
            this.organization = organization;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null)
            { return false; }
            if (this == obj)
            { return true; }
            if (this.getClass() != obj.getClass())
            { return false; }
            OrganisationKey that = (OrganisationKey) obj;
            return new EqualsBuilder()
                    .append(organization.getHostUrl(), that.organization.getHostUrl())
                    .append(organization.getName(), that.organization.getName())
                    .isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder(17, 37).append(organization.getHostUrl()).append(organization.getName()).toHashCode();
        }
    }

    private static final CacheSettings CACHE_SETTINGS = new CacheSettingsBuilder().expireAfterWrite(30, MINUTES).build();

    private final Cache<UserKey, DvcsUser> usersCache;
    private final Cache<OrganisationKey, List<Group>> groupsCache;
    private DvcsCommunicator delegate;

    @Autowired
    public CachingCommunicator(@ComponentImport final CacheManager cacheManager)
    {
        // self-loading caches returned from getCache are always clean/empty
        usersCache = cacheManager.getCache(getClass().getName() + ".usersCache", new UserLoader(), CACHE_SETTINGS);
        groupsCache = cacheManager.getCache(getClass().getName() + ".groupsCache", new GroupLoader(), CACHE_SETTINGS);
    }

    public void setDelegate(DvcsCommunicator delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public DvcsUser getUser(Repository repository, String username)
    {
        try
        {
            return usersCache.get(new UserKey(repository, username));
        }
        catch (CacheException e)
        {
            throw unrollException(e);
        }
    }

    private SourceControlException unrollException(final Throwable e)
    {
        return e.getCause() instanceof SourceControlException ?
                (SourceControlException) e.getCause() : new SourceControlException(e.getCause());
    }

    @Override
    public List<Group> getGroupsForOrganization(Organization organization)
    {
        try
        {
            return groupsCache.get(new OrganisationKey(organization));
        }
        catch (CacheException e)
        {
            throw unrollException(e);
        }
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
    public String getBranchUrl(final Repository repository, final Branch branch)
    {
        return delegate.getBranchUrl(repository, branch);
    }

    @Override
    public String getCreatePullRequestUrl(final Repository repository, final String sourceSlug, final String sourceBranch, final String destinationSlug, final String destinationBranch, final String eventSource)
    {
        return delegate.getCreatePullRequestUrl(repository, sourceSlug, sourceBranch, destinationSlug, destinationBranch, eventSource);
    }

    @Override
    public void startSynchronisation(final Repository repo, final EnumSet<SynchronizationFlag> flags, final int auditId)
    {
        delegate.startSynchronisation(repo, flags, auditId);
    }

    @Override
    public boolean isSyncDisabled(final Repository repo, final EnumSet<SynchronizationFlag> flags)
    {
        return delegate.isSyncDisabled(repo, flags);
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
    public List<Repository> getRepositories(Organization organization, List<Repository> storedRepositories)
    {
        return delegate.getRepositories(organization, storedRepositories);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Branch> getBranches(Repository repository)
    {
        return delegate.getBranches(repository);
    }

    @Override
    public Changeset getChangeset(Repository repository, String node)
    {
        return delegate.getChangeset(repository, node);
    }

    @Override
    public ChangesetFileDetailsEnvelope getFileDetails(Repository repository, Changeset changeset)
    {
        return delegate.getFileDetails(repository, changeset);
    }

    @Override
    public void ensureHookPresent(Repository repository, String postCommitUrl)
    {
        delegate.ensureHookPresent(repository, postCommitUrl);
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

    @Override
    public void linkRepository(Repository repository, Set<String> withProjectkeys)
    {
        delegate.linkRepository(repository, withProjectkeys);
    }

    @Override
    public void linkRepositoryIncremental(Repository repository, Set<String> withPossibleNewProjectkeys)
    {
        delegate.linkRepositoryIncremental(repository, withPossibleNewProjectkeys);
    }

    @Override
    public DvcsUser getTokenOwner(Organization organization)
    {
        return delegate.getTokenOwner(organization);
    }

    @Override
    public DvcsCommunicator getDelegate()
    {
        return delegate;
    }

    private class UserLoader implements CacheLoader<UserKey, DvcsUser>
    {
        @Override
        public DvcsUser load(@Nonnull final UserKey key)
        {
            return delegate.getUser(key.repository, key.username);
        }
    }

    private class GroupLoader implements CacheLoader<OrganisationKey, List<Group>>
    {
        @Override
        public List<Group> load(@Nonnull final OrganisationKey key)
        {
            return delegate.getGroupsForOrganization(key.organization);
        }
    }
}
