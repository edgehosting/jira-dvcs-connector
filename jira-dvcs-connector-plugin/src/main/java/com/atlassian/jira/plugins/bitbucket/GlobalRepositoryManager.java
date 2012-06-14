package com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.SynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.api.impl.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.api.rest.UrlInfo;
import com.atlassian.jira.plugins.bitbucket.api.streams.GlobalFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Aggregated Repository Manager that handles all Repository Managers based on the repository url
 */
public class GlobalRepositoryManager implements RepositoryManager
{
    private final RepositoryManager[] repositoryManagers;
    private final RepositoryPersister repositoryPersister;

    private static final Comparator<? super Changeset> CHANGESET_COMPARATOR = new Comparator<Changeset>()
    {
        @Override
        public int compare(Changeset o1, Changeset o2)
        {
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        }
    };

    public GlobalRepositoryManager(RepositoryPersister repositoryPersister, RepositoryManager... repositoryManagers)
    {
        this.repositoryPersister = repositoryPersister;
        this.repositoryManagers = repositoryManagers;
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

    @Override
    public SourceControlRepository addRepository(String repositoryType, String projectKey, String url, String adminUsername, String adminPassword, String accessToken)
    {
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            if (repositoryManager.getRepositoryType().equals(repositoryType))
            {
                return repositoryManager.addRepository(repositoryType, projectKey, url, adminUsername, adminPassword, accessToken);
            }
        }
        throw new IllegalArgumentException("No repository manager found for given repository type [" + repositoryType + "]");
    }


    @Override
    public List<SourceControlRepository> getRepositories(String projectKey)
    {
        List<SourceControlRepository> allRepositories = new ArrayList<SourceControlRepository>();
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            allRepositories.addAll(repositoryManager.getRepositories(projectKey));
        }
        return allRepositories;
    }


    @Override
    public SourceControlRepository getRepository(int id)
    {
        return getManagerByRepoId(id).getRepository(id);
    }

    @Override
    public List<Changeset> getChangesets(final String issueKey)
    {
        List<Changeset> allChangesets = new ArrayList<Changeset>();
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            allChangesets.addAll(repositoryManager.getChangesets(issueKey));
        }
        Collections.sort(allChangesets, CHANGESET_COMPARATOR);
        return new ArrayList<Changeset>(allChangesets);
    }


    @Override
    public Changeset getDetailChangeset(SourceControlRepository repository, Changeset changeset)
    {
        return getManagerByRepository(repository).getDetailChangeset(repository, changeset);
    }

    @Override
    public void removeRepository(int id)
    {
        getManagerByRepoId(id).removeRepository(id);
    }


    @Override
    public void addChangeset(SourceControlRepository repository, String issueId, Changeset changeset)
    {
        getManagerByRepository(repository).addChangeset(repository, issueId, changeset);
    }

    @Override
    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        return getManagerByRepository(repository).getUser(repository, username);
    }

    @Override
    public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progressProvider)
    {
        return getManagerByRepository(key.getRepository()).getSynchronisationOperation(key, progressProvider);
    }

    @Override
    public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset)
    {
        RepositoryManager repositoryManager = getManagerByRepositoryType(repository.getRepositoryType());
        return repositoryManager.getHtmlForChangeset(repository, changeset);
    }

    @Override
    public String getRepositoryType()
    {
        return "unknown";
    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo)
    {
        getManagerByRepository(repo).setupPostcommitHook(repo);
    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo)
    {
        getManagerByRepository(repo).removePostcommitHook(repo);
    }

    @Override
    public Set<Changeset> getLatestChangesets(int count, GlobalFilter globalFilter)
    {

        Set<Changeset> allChangesets = new TreeSet<Changeset>(Collections.reverseOrder(CHANGESET_COMPARATOR));
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            allChangesets.addAll(repositoryManager.getLatestChangesets(count, globalFilter));
        }
        return allChangesets;
    }

    @Override
    public UrlInfo getUrlInfo(String repositoryUrl, String projectKey)
    {
        for (RepositoryManager repositoryManager : repositoryManagers)
        {
            UrlInfo urlInfo = repositoryManager.getUrlInfo(repositoryUrl, projectKey);
            if (urlInfo != null)
            {
                return urlInfo;
            }
        }
        return null;
    }

    @Override
    public Changeset reloadChangeset(int repositoryId, String node, String issueId, String branch)
    {
        throw new UnsupportedOperationException("This implementation should never be called.");
    }

    @Override
    public Date getLastCommitDate(SourceControlRepository repo)
    {
        return getManagerByRepository(repo).getLastCommitDate(repo);
    }

    @Override
    public void setLastCommitDate(SourceControlRepository repo, Date date)
    {
        getManagerByRepository(repo).setLastCommitDate(repo, date);
    }

    @Override
    public void removeAllChangesets(int repositoryId) {
        getManagerByRepoId(repositoryId).removeAllChangesets(repositoryId);
    }

}