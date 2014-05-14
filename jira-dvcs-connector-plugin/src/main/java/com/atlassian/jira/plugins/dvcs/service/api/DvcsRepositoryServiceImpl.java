package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;

import java.util.List;
import java.util.Set;

public class DvcsRepositoryServiceImpl implements DvcsRepositoryService
{
    private RepositoryService repositoryService;

    public DvcsRepositoryServiceImpl(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    @Override
    public List<Repository> getRepositories(boolean includeDeleted)
    {
        return repositoryService.getAllRepositories(includeDeleted);
    }

    @Override
    public List<Repository> getRepositories(String dvcsType, boolean includeDeleted)
    {
        return repositoryService.getAllRepositories(dvcsType, includeDeleted);
    }

    @Override
    public List<Repository> getRepositories(int organizationId, boolean includeDeleted)
    {
        return repositoryService.getAllByOrganization(organizationId, includeDeleted);
    }

    @Override
    public Repository getRepository(int repositoryId)
    {
        return repositoryService.get(repositoryId);
    }

    @Override
    public DvcsUser getDvcsUser(Repository repository, String author, String rawAuthor)
    {
        return repositoryService.getUser(repository, author, rawAuthor);
    }

    @Override
    public Set<String> getDvcsUserEmails(Repository repository, DvcsUser dvcsUser)
    {
        return repositoryService.getEmails(repository, dvcsUser);
    }
}
