package com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Aggregated Repository Manager that handles all Repository Managers based on the repository url
 */
public class GlobalRepositoryManager implements RepositoryManager
{
    public RepositoryManager[] repositoryManagers;
    private final RepositoryPersister repositoryPersister;

    public GlobalRepositoryManager(RepositoryPersister repositoryPersister, RepositoryManager... repositoryManagers)
    {
        this.repositoryPersister = repositoryPersister;
        this.repositoryManagers = repositoryManagers;
    }

    private RepositoryManager getManagerForUrl(String url)
    {
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            if (repositoryManager.canHandleUrl(url))
            {
                return repositoryManager;
            }
        }
        throw new IllegalArgumentException("No repository manager found for given url [" + url + "]");
    }

    private RepositoryManager getManagerByRepoId(int id)
    {
        ProjectMapping repository = repositoryPersister.getRepository(id);
        if (repository == null)
            throw new IllegalArgumentException("No repository with id = '" + id + "' found");

        RepositoryManager repositoryManager = getManagerByRepositoryType(repository.getRepositoryType());
        if (repositoryManager == null)
        {
            throw new IllegalArgumentException("No repository manager found for given id = '" + id + "'");
        }

        return repositoryManager;
    }

    private RepositoryManager getManagerByRepository(SourceControlRepository repository)
    {
        return getManagerByRepositoryType(repository.getRepositoryType());
    }

    private RepositoryManager getManagerByRepositoryType(String repositoryType)
    {
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            if (repositoryManager.getRepositoryType().equals(repositoryType))
            {
                return repositoryManager;
            }
        }
        return null;
    }


    public boolean canHandleUrl(String url)
    {
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            if (repositoryManager.canHandleUrl(url))
            {
                return true;
            }
        }
        return false;
    }

    public SourceControlRepository addRepository(String projectKey, String url, String username, String password, String adminUsername, String adminPassword)
    {
        return getManagerForUrl(url).addRepository(projectKey, url, username, password, adminUsername, adminPassword);
    }


    public List<SourceControlRepository> getRepositories(String projectKey)
    {
        List<SourceControlRepository> allRepositories = new ArrayList<SourceControlRepository>();
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            allRepositories.addAll(repositoryManager.getRepositories(projectKey));
        }
        return allRepositories;
    }


    public SourceControlRepository getRepository(int id)
    {
        return getManagerByRepoId(id).getRepository(id);
    }

    public List<Changeset> getChangesets(final String issueKey)
    {
        List<Changeset> allChangesets = new ArrayList<Changeset>();
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            allChangesets.addAll(repositoryManager.getChangesets(issueKey));
        }
        return allChangesets;
    }

    public void removeRepository(int id)
    {
        getManagerByRepoId(id).removeRepository(id);
    }


    public void addChangeset(SourceControlRepository repository, String issueId, Changeset changeset)
    {
        getManagerByRepository(repository).addChangeset(repository, issueId, changeset);
    }

    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        return getManagerByRepository(repository).getUser(repository, username);
    }

    public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progressProvider)
    {
        return getManagerByRepository(key.getRepository()).getSynchronisationOperation(key, progressProvider);
    }

    public List<Changeset> parsePayload(SourceControlRepository repository, String payload)
    {
        return getManagerByRepository(repository).parsePayload(repository, payload);
    }

    public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset)
    {
        RepositoryManager repositoryManager = getManagerByRepositoryType(repository.getRepositoryType());
        return repositoryManager.getHtmlForChangeset(repository, changeset);
    }

    public String getRepositoryType()
    {
        return "unknown";
    }

    public void setupPostcommitHook(SourceControlRepository repo)
    {
        getManagerByRepository(repo).setupPostcommitHook(repo);
    }

    public void removePostcommitHook(SourceControlRepository repo)
    {
        getManagerByRepository(repo).removePostcommitHook(repo);
    }

    @Override
    public List<IssueMapping> getLastChangesetMappings(int count, Set<String> inProjects, Set<String> notInProjects)
    {
        return repositoryPersister.getLastChangesetMappings(count, inProjects, notInProjects);
    }

    @Override
    public Changeset getChangeset(String node)
    {
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            Changeset changeset = repositoryManager.getChangeset(node);
            if (changeset != null)
            {
                return changeset;
            }
        }
        return null;
    }
}
