package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.plugins.dvcs.sync.impl.DefaultSynchronisationOperation;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Maps;

public class RepositoryServiceImpl implements RepositoryService
{
    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

	private final DvcsCommunicatorProvider communicatorProvider;
	private final RepositoryDao repositoryDao;
	private final Synchronizer synchronizer;
	private final ChangesetService changesetService;
	private final ApplicationProperties applicationProperties;

	public RepositoryServiceImpl(DvcsCommunicatorProvider communicatorProvider, RepositoryDao repositoryDao, Synchronizer synchronizer,
        ChangesetService changesetService, ApplicationProperties applicationProperties)
    {
        this.communicatorProvider = communicatorProvider;
        this.repositoryDao = repositoryDao;
        this.synchronizer = synchronizer;
        this.changesetService = changesetService;
        this.applicationProperties = applicationProperties;
    }

    @Override
	public List<Repository> getAllByOrganization(int organizationId)
	{
		return repositoryDao.getAllByOrganization(organizationId, false);
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
		log.debug("Synchronising list of repositories");
		// get repositories from the dvcs hosting server
		DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());
		List<Repository> remoteRepositories = communicator.getRepositories(organization);
		// get local repositories
		List<Repository> storedRepositories = repositoryDao.getAllByOrganization(organization.getId(), true);

		// update names of existing repositories in case their names changed
		updateExistingRepositories(storedRepositories, remoteRepositories);
		// repositories that are no longer on hosting server will be marked as deleted
		removeDeletedRepositories(storedRepositories, remoteRepositories);
		// new repositories will be added to the database
		addNewRepositories(storedRepositories, remoteRepositories, organization);

		// start asynchronous changesets synchronization for all linked repositories in organization
		syncAllInOrganization(organization.getId());
	}

	/**
	 * @param storedRepositories
	 * @param remoteRepositories
	 * @param organization
	 */
	private void addNewRepositories(List<Repository> storedRepositories, List<Repository> remoteRepositories, Organization organization)
    {
		Map<String, Repository> remoteRepos = makeRepositoryMap(remoteRepositories);
		// remove existing
		for (Repository localRepo : storedRepositories)
		{
			remoteRepos.remove(localRepo.getSlug());
		}

		for (Repository repository : remoteRepos.values())
        {
			// save brand new
			repository.setOrganizationId(organization.getId());
			repository.setDvcsType(organization.getDvcsType());
			repository.setLinked(organization.isAutolinkNewRepos());
			repository.setCredential(organization.getCredential());

			// need for installing post commit hook
			repository.setOrgHostUrl(organization.getHostUrl());
			repository.setOrgName(organization.getName());

			final Repository savedRepository = repositoryDao.save(repository);
			log.debug("Adding new repository " + savedRepository);

			// if linked install post commit hook
			if (savedRepository.isLinked())
			{
                try
                {
                    addOrRemovePostcommitHook(savedRepository);
                }
                catch (SourceControlException e)
                {
                    if (e.getCause() instanceof ResponseException)
                    {
                        // if the user didn't have rights to add post commit hook, just unlink the repository
                        savedRepository.setLinked(false);
                        repositoryDao.save(savedRepository);
                    }
                }
			}
        }
    }

	/**
	 * @param storedRepositories
	 * @param remoteRepositories
	 */
	private void removeDeletedRepositories(List<Repository> storedRepositories, List<Repository> remoteRepositories)
    {
		Map<String, Repository> remoteRepos = makeRepositoryMap(remoteRepositories);
		for (Repository localRepo : storedRepositories)
		{
			Repository remotRepo = remoteRepos.get(localRepo.getSlug());
			// does the remote repo exists?
			if (remotRepo==null)
			{
				log.debug("Deleting repository "+ localRepo);
				localRepo.setDeleted(true);
				repositoryDao.save(localRepo);
			}
		}
    }

	/**
	 * Updates existing repositories
	 *  - undelete existing deleted
	 *  - updates names
	 *
	 * @param storedRepositories
	 * @param remoteRepositories
	 */
	private void updateExistingRepositories(List<Repository> storedRepositories, List<Repository> remoteRepositories)
    {
		Map<String, Repository> remoteRepos = makeRepositoryMap(remoteRepositories);
		for (Repository localRepo : storedRepositories)
        {
			Repository remoteRepo = remoteRepos.get(localRepo.getSlug());
			if (remoteRepo != null)
			{
				// set the name and save
				localRepo.setName(remoteRepo.getName());
				localRepo.setDeleted(false); // it could be deleted before and
											 // now will be revived
				repositoryDao.save(localRepo);
			}
        }
    }

	/**
	 * Converts collection of repository objects into map where key is
	 * repository slug and value is repository object
	 *
	 * @param repositories
	 * @return
	 */
	private Map<String, Repository> makeRepositoryMap(Collection<Repository> repositories)
    {
	    Map<String, Repository> map = Maps.newHashMap();
		for (Repository repository : repositories)
        {
	        map.put(repository.getSlug(), repository);
        }
		return map;
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
		final List<Repository> repositories = getAllByOrganization(organizationId);
		for (Repository repository : repositories)
		{
			doSync(repository, true);
		}
	}

	private void doSync(Repository repository, boolean softSync)
	{
		if (repository.isLinked())
		{
            DefaultSynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(repository, this, changesetService, softSync);
			synchronizer.synchronize(repository, synchronisationOperation);
		}
	}

	@Override
	public List<Repository> getAllRepositories()
	{
		return repositoryDao.getAll(false);
	}

    @Override
    public boolean existsLinkedRepositories()
    {
        final List<Repository> repositories = repositoryDao.getAll(false);
        for (Repository repository : repositories)
        {
            if (repository.isLinked())
            {
                return true;
            }
        }
        return false;
    }

    @Override
	public void enableRepository(int repoId, boolean linked)
	{
		final Repository repository = repositoryDao.get(repoId);
		if (repository != null)
		{
		    if (!linked)
		    {
		        synchronizer.stopSynchronization(repository);
		    }

			repository.setLinked(linked);

			addOrRemovePostcommitHook(repository);

			repositoryDao.save(repository);
		}
	}

	private void addOrRemovePostcommitHook(Repository repository)
	{
		final DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());
		final String postCommitUrl = getPostCommitUrl(repository);

		if (repository.isLinked())
		{
			communicator.setupPostcommitHook(repository, postCommitUrl);
		} else
		{
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
			remove(repository);
		}
	}

	@Override
	public void remove(Repository repository)
	{
	    synchronizer.stopSynchronization(repository);
		// try remove postcommit hook
		if (repository.isLinked())
		{
			removePostcommitHook(repository);
		}
		// remove all changesets from DB that references this repository
		changesetService.removeAllInRepository(repository.getId());
		// delete repository record itself
		repositoryDao.remove(repository.getId());
	}

	private void removePostcommitHook(Repository repository)
	{
		try
		{
			final DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());
			final String postCommitUrl = getPostCommitUrl(repository);


			communicator.removePostcommitHook(repository, postCommitUrl);
		} catch (Exception e)
		{
			log.warn("Failed to uninstall postcommit hook for repository id = " + repository.getId() + ", slug = "
					+ repository.getSlug(), e);
		}
	}

}
