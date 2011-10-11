package com.atlassian.jira.plugins.bitbucket.api;

import java.util.List;

/**
 * Uniquely identify a synchronization operation
 */
public class SynchronizationKey
{
    private final String projectKey;
    private final String repositoryUrl;
    private final List<Changeset> changesets;

    public SynchronizationKey(String projectKey, String repositoryUri, List<Changeset> changesets)
    {
        this.projectKey = projectKey;
        this.repositoryUrl = repositoryUri;
        this.changesets = changesets;
    }

    public SynchronizationKey(String projectKey, String repositoryUrl)
    {
        this.projectKey = projectKey;
        this.repositoryUrl = repositoryUrl;
        changesets = null;
    }

    public String getProjectKey()
    {
        return projectKey;
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynchronizationKey that = (SynchronizationKey) o;
        if (changesets != null ? !changesets.equals(that.changesets) : that.changesets != null) return false;
        if (!projectKey.equals(that.projectKey)) return false;
        if (!repositoryUrl.equals(that.repositoryUrl)) return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = projectKey.hashCode();
        result = 31 * result + repositoryUrl.hashCode();
        result = 31 * result + (changesets != null ? changesets.hashCode() : 0);
        return result;
    }

    public List<Changeset> getChangesets()
    {
        return changesets;
    }

    public boolean matches(String projectKey, String repositoryUrl)
    {
        return this.projectKey.equals(projectKey) && this.repositoryUrl.equals(repositoryUrl);
    }
}
