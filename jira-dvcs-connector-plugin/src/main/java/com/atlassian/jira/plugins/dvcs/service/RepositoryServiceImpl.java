package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.ApplicationProperties;

public class RepositoryServiceImpl implements RepositoryService
{
    private DvcsCommunicatorProvider communicatorProvider;
    private RepositoryDao repositoryDao;
    private Synchronizer synchronizer;
    private ChangesetService changesetService;
    private ApplicationProperties applicationProperties;

    public RepositoryServiceImpl()
    {
    }

    public void setCommunicatorProvider(DvcsCommunicatorProvider communicatorProvider)
    {
        this.communicatorProvider = communicatorProvider;
    }

    public void setRepositoryDao(RepositoryDao repositoryDao)
    {
        this.repositoryDao = repositoryDao;
    }

    public void setSynchronizer(Synchronizer synchronizer)
    {
        this.synchronizer = synchronizer;
    }

    public void setChangesetService(ChangesetService changesetService)
    {
        this.changesetService = changesetService;
    }

    public void setApplicationProperties(ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public List<Repository> getAllByOrganization(int organizationId, boolean alsoDeleted)
    {
        return repositoryDao.getAllByOrganization(organizationId, alsoDeleted);
    }

    @Override
    public Repository get(int repositoryId)
    {
        return repositoryDao.get(repositoryId);
    }

    @Override
    public Repository save(Repository repository)
    {
        return repositoryDao.save(repository);
    }

    @Override
    public void syncRepositoryList(Organization organization)
    {
        List<Repository> storedRepositories = repositoryDao.getAllByOrganization(organization.getId(), true);

        DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());
        final List<Repository> remoteRepositories = communicator.getRepositories(organization);
        for (Repository remoteRepository : remoteRepositories)
        {
            // find repository by SLUG in storedRepositories
            Repository storedRepository = null;
            for (int i=0; i<storedRepositories.size(); i++)
            {
                storedRepository = storedRepositories.get(i);
                if (storedRepository.getSlug().equals(remoteRepository.getSlug()))
                {
                    // remove existed from list. will stay there only those which we have to delete
                    storedRepositories.remove(i);
                    break;
                } else
                {
                    storedRepository = null;
                }
            }

            if (storedRepository != null)
            {
                // we have it. try to update NAME
                storedRepository.setName(remoteRepository.getName());
                storedRepository.setDeleted(false); // it could be deleted before and now will be revived
                repositoryDao.save(storedRepository);
            } else
            {
                // save brand new
                remoteRepository.setOrganizationId(organization.getId());
                remoteRepository.setDvcsType(organization.getDvcsType());
                remoteRepository.setLinked(organization.isAutolinkNewRepos());
                remoteRepository.setCredential(organization.getCredential());

                // need for install post commit hook
                remoteRepository.setOrgHostUrl(organization.getHostUrl());
                remoteRepository.setOrgName(organization.getName());

                final Repository savedRepository = repositoryDao.save(remoteRepository);

                // if linked install post commit hook
                if (savedRepository.isLinked()) {
                    setupPostcommitHook(savedRepository);
                }
            }
        }

        // set as DELETED all stored repositories which are not on remote dvcs system
        for (Repository storedRepository : storedRepositories)
        {
            storedRepository.setDeleted(true);
            repositoryDao.save(storedRepository);
        }

        // start asynchronous changesets synchronization for all repositories in organization
        syncAllInOrganization(organization.getId());
    }


    @Override
    public void sync(int repositoryId, boolean softSync)
    {
        final Repository repository = get(repositoryId);
        doSync(repository, softSync);
    }

    @Override
    public void syncAllInOrganization(int organizationId)
    {
        final List<Repository> repositories = getAllByOrganization(organizationId, false);
        for (Repository repository : repositories)
        {
            doSync(repository, false);
        }
    }

    private void doSync(Repository repository, boolean softSync)
    {
        if (repository.isLinked()) {
            synchronizer.synchronize(repository, softSync);
        }
    }

    @Override
	public List<Repository> getAllActiveRepositories()
	{
    	return repositoryDao.getAll(false);
	}

	@Override
	public void enableAutolinkCommits(int repoId, boolean linked) {
        final Repository repository = repositoryDao.get(repoId);
        if (repository != null)
        {
            repository.setLinked(linked);

            setupPostcommitHook(repository);
            
            repositoryDao.save(repository);
        }
    }

    private void setupPostcommitHook(Repository repository)
    {
        final DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());
        final String postCommitUrl = getPostCommitUrl(repository);
        if (repository.isLinked())
        {
            communicator.setupPostcommitHook(repository, postCommitUrl);
        } else {
            communicator.removePostcommitHook(repository, postCommitUrl);
        }
    }

    private String getPostCommitUrl(Repository repo)
    {
        return applicationProperties.getBaseUrl() + "/rest/bitbucket/1.0/repository/" + repo.getId() + "/sync";
    }


    @Override
    public void removeAllInOrganization(int organizationId)
    {
        final List<Repository> repositories = repositoryDao.getAllByOrganization(organizationId, true);
        for (Repository repository : repositories)
        {
            changesetService.removeAllInRepository(repository.getId());
            repositoryDao.remove(repository.getId());
        }
    }


}
