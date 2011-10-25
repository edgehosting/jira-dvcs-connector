package com.atlassian.jira.plugins.bitbucket.api;

import java.util.List;

/**
 * Uniquely identify a synchronization operation
 */
public class SynchronizationKey
{
    private final List<Changeset> changesets;
	private final SourceControlRepository repository;

    public SynchronizationKey(SourceControlRepository repository, List<Changeset> changesets)
    {
        this.repository = repository;
        this.changesets = changesets;
    }

    public SynchronizationKey(SourceControlRepository repository)
    {
        this.repository = repository;
        changesets = null;
    }

    public SourceControlRepository getRepository()
	{
		return repository;
	}

	@Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynchronizationKey that = (SynchronizationKey) o;
        if (changesets != null ? !changesets.equals(that.changesets) : that.changesets != null) return false;
        if (!repository.equals(that.repository)) return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = repository.hashCode();
        result = 31 * result + (changesets != null ? changesets.hashCode() : 0);
        return result;
    }

    public List<Changeset> getChangesets()
    {
        return changesets;
    }

    public boolean matches(SourceControlRepository repository)
    {
        return this.repository.equals(repository);
    }
}
