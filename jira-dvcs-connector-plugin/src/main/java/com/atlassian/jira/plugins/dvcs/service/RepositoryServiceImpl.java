package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;

import java.util.ArrayList;
import java.util.List;

public class RepositoryServiceImpl implements RepositoryService
{
    private DvcsCommunicatorProvider communicatorProvider;
    private RepositoryDao repositoryDao;
    private Synchronizer synchronizer;

    public RepositoryServiceImpl(DvcsCommunicatorProvider communicatorProvider, RepositoryDao repositoryDao, Synchronizer synchronizer)
    {
        this.communicatorProvider = communicatorProvider;
        this.repositoryDao = repositoryDao;
        this.synchronizer = synchronizer;
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
                // save brand new or updated repository
                remoteRepository.setOrganizationId(organization.getId());
                remoteRepository.setDvcsType(organization.getDvcsType());
                remoteRepository.setLinked(true);
                remoteRepository.setCredential(organization.getCredential());
                repositoryDao.save(remoteRepository);
            }
        }

        // set as DELETED all stored repositories which are not on remote dvcs system
        for (Repository storedRepository : storedRepositories)
        {
            storedRepository.setDeleted(true);
            repositoryDao.save(storedRepository);
        }


    }



    @Override
    public void sync(int repositoryId, boolean softSync)
    {
        final Repository repository = get(repositoryId);
        synchronizer.synchronize(repository, softSync);
    }

    @Override
    public void syncAllInOrganization(int organizationId)
    {
        final List<Repository> repositories = getAllByOrganization(organizationId, false);
        for (Repository repository : repositories)
        {
            synchronizer.synchronize(repository, false);
        }
    }

    @Override
	public List<Repository> getAllActiveRepositories()
	{
		// TODO Auto-generated method stub
		return new ArrayList<Repository>();
	}
    
}
