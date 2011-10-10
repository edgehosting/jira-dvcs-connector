package com.atlassian.jira.plugins.bitbucket.api;

import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;

import java.util.List;

/**
 * Uniquely identify a synchronization operation
 */
public class SynchronizationKey
{
    private final String projectKey;
    private final RepositoryUri repositoryUri;
    private final List<Changeset> changesets;

    public SynchronizationKey(String projectKey, RepositoryUri repositoryUri, List<Changeset> changesets)
    {
        this.projectKey = projectKey;
        this.repositoryUri = repositoryUri;
        this.changesets = changesets;
    }

    public SynchronizationKey(String projectKey, RepositoryUri repositoryUri)
    {
        this.projectKey = projectKey;
        this.repositoryUri = repositoryUri;
        changesets = null;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public RepositoryUri getRepositoryUri()
    {
        return repositoryUri;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynchronizationKey that = (SynchronizationKey) o;
        if (changesets != null ? !changesets.equals(that.changesets) : that.changesets != null) return false;
        if (!projectKey.equals(that.projectKey)) return false;
        if (!repositoryUri.equals(that.repositoryUri)) return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = projectKey.hashCode();
        result = 31 * result + repositoryUri.hashCode();
        result = 31 * result + (changesets != null ? changesets.hashCode() : 0);
        return result;
    }

    public List<Changeset> getChangesets()
    {
        return changesets;
    }

    public boolean matches(String projectKey, RepositoryUri repositoryUri)
    {
        return this.projectKey.equals(projectKey) && this.repositoryUri.equals(repositoryUri);
    }
}
