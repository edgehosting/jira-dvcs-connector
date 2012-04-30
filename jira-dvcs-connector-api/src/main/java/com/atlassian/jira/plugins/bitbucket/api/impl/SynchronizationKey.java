package com.atlassian.jira.plugins.bitbucket.api.impl;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Uniquely identify a synchronization operation
 */
public class SynchronizationKey
{
	private final SourceControlRepository repository;
	private final boolean softSync; // only sync commits since last sync date

    public SynchronizationKey(SourceControlRepository repository)
    {
        this.repository = repository;
        this.softSync = false;
    }

    public SynchronizationKey(SourceControlRepository repository, boolean softSync)
    {
        this.repository = repository;
        this.softSync = softSync;
    }

    public SourceControlRepository getRepository()
	{
		return repository;
	}
    public boolean isSoftSync()
    {
        return softSync;
    }

    public boolean matches(SourceControlRepository repository)
    {
        return this.repository.equals(repository);
    }
	@Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynchronizationKey that = (SynchronizationKey) o;
        if (softSync != that.softSync) return false;
        if (!repository.equals(that.repository)) return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(softSync).append(repository).hashCode();
    }


}
