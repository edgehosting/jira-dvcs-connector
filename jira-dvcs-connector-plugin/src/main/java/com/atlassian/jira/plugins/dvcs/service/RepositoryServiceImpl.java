package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.google.common.collect.Maps;

/**
 * The Class RepositoryServiceImpl.
 */
public class RepositoryServiceImpl implements RepositoryService
{
    
    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

	/** The communicator provider. */
	private final DvcsCommunicatorProvider communicatorProvider;
	
	/** The repository dao. */
	private final RepositoryDao repositoryDao;
	
	/** The synchronizer. */
	private final Synchronizer synchronizer;
	
	/** The changeset service. */
	private final ChangesetService changesetService;
	
	/** The application properties. */
	private final ApplicationProperties applicationProperties;

	/**
	 * The Constructor.
	 *
	 * @param communicatorProvider the communicator provider
	 * @param repositoryDao the repository dao
	 * @param synchronizer the synchronizer
	 * @param changesetService the changeset service
	 * @param applicationProperties the application properties
	 */
	public RepositoryServiceImpl(DvcsCommunicatorProvider communicatorProvider, RepositoryDao repositoryDao, Synchronizer synchronizer,
        ChangesetService changesetService, ApplicationProperties applicationProperties)
    {
        this.communicatorProvider = communicatorProvider;
        this.repositoryDao = repositoryDao;
        this.synchronizer = synchronizer;
        this.changesetService = changesetService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public List<Repository> getAllByOrganization(int organizationId)
	{
		return repositoryDao.getAllByOrganization(organizationId, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Repository get(int repositoryId)
	{
		return repositoryDao.get(repositoryId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Repository save(Repository repository)
	{
		return repositoryDao.save(repository);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void syncRepositoryList(Organization organization)
	{
		log.debug("Synchronising list of repositories");
		// get repositories from the dvcs hosting server
		DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());
		List<Repository> remoteRepositories = communicator.getRepositories(organization);
		// get local repositories
		List<Repository> storedRepositories = repositoryDao.getAllByOrganization(organization.getId(), true);

		// BBC-231 somehow we ended up with duplicated repositories on QA-EACJ
		removeDuplicateRepositories(organization, storedRepositories);
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
	 * Removes duplicated repositories.
	 * 
	 * @param organization
	 * @param storedRepositories
	 */
	private void removeDuplicateRepositories(Organization organization, List<Repository> storedRepositories)
    {
	    Set<String> existingRepositories = new HashSet<String>();
	    for (Repository repository : storedRepositories)
        {
            String slug = repository.getSlug();
            if (existingRepositories.contains(slug))
            {
                log.warn("Repository " + organization.getName() + "/" + slug + " is duplicated. Will be deleted.");
                remove(repository);
            } else
            {
                existingRepositories.add(slug);
            }
        }
    }

    /**
	 * Adds the new repositories.
	 *
	 * @param storedRepositories the stored repositories
	 * @param remoteRepositories the remote repositories
	 * @param organization the organization
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
			repository.setSmartcommitsEnabled(organization.isSmartcommitsOnNewRepos());

			// need for installing post commit hook
			repository.setOrgHostUrl(organization.getHostUrl());
			repository.setOrgName(organization.getName());

			Repository savedRepository = repositoryDao.save(repository);
			log.debug("Adding new repository with name " + savedRepository.getName());

			// if linked install post commit hook
			if (savedRepository.isLinked())
			{
                try
                {
                    addOrRemovePostcommitHook(savedRepository);
                }
                catch (SourceControlException e)
                {
                    log.warn("Adding postcommit hook for repository "
                            + savedRepository.getRepositoryUrl() + " failed: " + e.getMessage());
                    // if the user didn't have rights to add post commit hook, just unlink the repository
                    savedRepository.setLinked(false);
                    repositoryDao.save(savedRepository);
                }
			}
        }
    }

	/**
	 * Removes the deleted repositories.
	 *
	 * @param storedRepositories the stored repositories
	 * @param remoteRepositories the remote repositories
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
	 * - undelete existing deleted
	 * - updates names.
	 *
	 * @param storedRepositories the stored repositories
	 * @param remoteRepositories the remote repositories
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
                log.debug("Updating repository [{}]", localRepo);
				repositoryDao.save(localRepo);
			}
        }
    }

	/**
	 * Converts collection of repository objects into map where key is
	 * repository slug and value is repository object.
	 *
	 * @param repositories the repositories
	 * @return the map< string, repository>
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sync(int repositoryId, boolean softSync)
	{
		Repository repository = get(repositoryId);
		doSync(repository, softSync);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void syncAllInOrganization(int organizationId)
	{
		List<Repository> repositories = getAllByOrganization(organizationId);
		for (Repository repository : repositories)
		{
			doSync(repository, true);
		}
	}

	/**
	 * Do sync.
	 *
	 * @param repository the repository
	 * @param softSync the soft sync
	 */
	private void doSync(Repository repository, boolean softSync)
	{
		if (repository.isLinked())
		{
            DefaultSynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(repository, this, changesetService, softSync);
			synchronizer.synchronize(repository, synchronisationOperation);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Repository> getAllRepositories()
	{
		return repositoryDao.getAll(false);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsLinkedRepositories()
    {
        List<Repository> repositories = repositoryDao.getAll(false);
        for (Repository repository : repositories)
        {
            if (repository.isLinked())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void enableRepository(int repoId, boolean linked)
	{
		Repository repository = repositoryDao.get(repoId);
		if (repository != null)
		{
		    if (!linked)
		    {
		        synchronizer.stopSynchronization(repository);
		    }

			repository.setLinked(linked);
			addOrRemovePostcommitHook(repository);

            log.debug("Enable repository [{}]", repository);
			repositoryDao.save(repository);
		}
	}
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void enableRepositorySmartcommits(int repoId, boolean enabled)
	{
		Repository repository = repositoryDao.get(repoId);
		if (repository != null)
		{
		    if (!enabled)
		    {
		       // TODO - does syncer need to know that ? - synchronizer.disableSmartcommits();
		    }

			repository.setSmartcommitsEnabled(enabled);

            log.debug("Enable repository smartcommits [{}]", repository);
			repositoryDao.save(repository);
		}
	}

	/**
	 * Adds the or remove postcommit hook.
	 *
	 * @param repository the repository
	 */
	private void addOrRemovePostcommitHook(Repository repository)
	{
		DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());
		String postCommitUrl = getPostCommitUrl(repository);

		if (repository.isLinked())
		{
			communicator.setupPostcommitHook(repository, postCommitUrl);
		} else
		{
			communicator.removePostcommitHook(repository, postCommitUrl);
		}
	}

	/**
	 * Gets the post commit url.
	 *
	 * @param repo the repo
	 * @return the post commit url
	 */
	private String getPostCommitUrl(Repository repo)
	{
		return applicationProperties.getBaseUrl() + "/rest/bitbucket/1.0/repository/" + repo.getId() + "/sync";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeAllInOrganization(int organizationId)
	{
		List<Repository> repositories = repositoryDao.getAllByOrganization(organizationId, true);
		for (Repository repository : repositories)
		{
			remove(repository);
		}
	}

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * Removes the postcommit hook.
	 *
	 * @param repository the repository
	 */
	private void removePostcommitHook(Repository repository)
	{
		try
		{
            DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());
            String postCommitUrl = getPostCommitUrl(repository);
            communicator.removePostcommitHook(repository, postCommitUrl);
		} catch (Exception e)
		{
            log.warn("Failed to uninstall postcommit hook for repository id = " + repository.getId()
                            + ", slug = " + repository.getSlug(), e);
		}
	}

}
