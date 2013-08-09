package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser.UnknownUser;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryRegistration;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.BranchTip;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.plugins.dvcs.sync.impl.DefaultSynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.collect.Maps;

/**
 * The Class RepositoryServiceImpl.
 */
public class RepositoryServiceImpl implements RepositoryService, DisposableBean
{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    /**
     * Only single {@link #removeOrphanRepositories()} can running at same time.
     */
    private final Object removeOrphanRepositoriesLock = new Object();

    /**
     * @see #removeOrphanRepositoriesAsync(List)
     */
    private final ExecutorService removeOrphanRepositoriesExecutor = new ThreadPoolExecutor(//
            0, 1, // no remaining threads and at most single thread
            0, TimeUnit.MILLISECONDS, // destroys thread immediately, when is not used
            new LinkedBlockingQueue<Runnable>(), ThreadFactories.namedThreadFactory("DVCSConnectoRemoveRepositoriesExecutorThread"));

    /** The communicator provider. */
    @Resource
    private DvcsCommunicatorProvider communicatorProvider;

    /** The repository dao. */
    @Resource
    private RepositoryDao repositoryDao;

    @Resource
    private MessagingService messagingService;

    /** The synchronizer. */
    @Resource
    private Synchronizer synchronizer;

    /** The changeset service. */
    @Resource
    private ChangesetService changesetService;

    /** The application properties. */
    @Resource
    private ApplicationProperties applicationProperties;

    @Resource
    private PluginSettingsFactory pluginSettingsFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception
    {
        removeOrphanRepositoriesExecutor.shutdown();
        if (!removeOrphanRepositoriesExecutor.awaitTermination(1, TimeUnit.MINUTES))
        {
            log.error("Unable properly shutdown queued tasks.");
        }
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
    public List<Repository> getAllByOrganization(int organizationId, boolean includeDeleted)
    {
        return repositoryDao.getAllByOrganization(organizationId, includeDeleted);
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
    public synchronized void syncRepositoryList(Organization organization, boolean soft)
    {
        log.debug("Synchronising list of repositories");

        InvalidOrganizationManager invalidOrganizationsManager = new InvalidOrganizationsManagerImpl(pluginSettingsFactory);
        invalidOrganizationsManager.setOrganizationValid(organization.getId(), true);

        // get repositories from the dvcs hosting server
        DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());

        List<Repository> remoteRepositories;

        try
        {
            remoteRepositories = communicator.getRepositories(organization);
        } catch (SourceControlException.UnauthorisedException e)
        {
            // we could not load repositories, we can't continue
            // mark the organization as invalid
            invalidOrganizationsManager.setOrganizationValid(organization.getId(), false);
            throw e;
        }

        // get local repositories
        List<Repository> storedRepositories = repositoryDao.getAllByOrganization(organization.getId(), true);

        // BBC-231 somehow we ended up with duplicated repositories on QA-EACJ
        removeDuplicateRepositories(organization, storedRepositories);
        // update names of existing repositories in case their names changed
        updateExistingRepositories(storedRepositories, remoteRepositories);
        // repositories that are no longer on hosting server will be marked as deleted
        removeDeletedRepositories(storedRepositories, remoteRepositories);
        // new repositories will be added to the database
        Set<String> newRepoSlugs = addNewReposReturnNewSlugs(storedRepositories, remoteRepositories, organization);

        // start asynchronous changesets synchronization for all linked repositories in organization
        syncAllInOrganization(organization.getId(), soft, newRepoSlugs);
    }

    @Override
    public void syncRepositoryList(Organization organization)
    {
        syncRepositoryList(organization, true);

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
     * @param storedRepositories
     *            the stored repositories
     * @param remoteRepositories
     *            the remote repositories
     * @param organization
     *            the organization
     */
    private Set<String> addNewReposReturnNewSlugs(List<Repository> storedRepositories, List<Repository> remoteRepositories,
            Organization organization)
    {
        Set<String> newRepoSlugs = new HashSet<String>();
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
            newRepoSlugs.add(savedRepository.getSlug());
            log.debug("Adding new repository with name " + savedRepository.getName());

            // if linked install post commit hook
            if (savedRepository.isLinked())
            {
                try
                {
                    addOrRemovePostcommitHook(savedRepository, getPostCommitUrl(savedRepository));
                } catch (SourceControlException.PostCommitHookRegistrationException e)
                {
                    log.warn("Adding postcommit hook for repository " + savedRepository.getRepositoryUrl() + " failed: ", e);
                    updateAdminPermission(savedRepository, false);
                    // if the user didn't have rights to add post commit hook, just unlink the repository
                    savedRepository.setLinked(false);
                    repositoryDao.save(savedRepository);
                }
            }
        }
        return newRepoSlugs;
    }

    private void updateAdminPermission(Repository repository, boolean hasAdminPermission)
    {
        if (repository.isLinked())
        {
            Progress progress = repository.getSync();
            if (progress == null)
            {
                progress = new DefaultProgress();
                progress.setFinished(true);
                synchronizer.putProgress(repository, progress);
            }

            progress.setAdminPermission(hasAdminPermission);
        }
    }

    /**
     * Removes the deleted repositories.
     * 
     * @param storedRepositories
     *            the stored repositories
     * @param remoteRepositories
     *            the remote repositories
     */
    private void removeDeletedRepositories(List<Repository> storedRepositories, List<Repository> remoteRepositories)
    {
        Map<String, Repository> remoteRepos = makeRepositoryMap(remoteRepositories);
        for (Repository localRepo : storedRepositories)
        {
            Repository remotRepo = remoteRepos.get(localRepo.getSlug());
            // does the remote repo exists?
            if (remotRepo == null)
            {
                log.debug("Deleting repository " + localRepo);
                localRepo.setDeleted(true);
                repositoryDao.save(localRepo);
            }
        }
    }

    /**
     * Updates existing repositories - undelete existing deleted - updates names.
     * 
     * @param storedRepositories
     *            the stored repositories
     * @param remoteRepositories
     *            the remote repositories
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
     * Converts collection of repository objects into map where key is repository slug and value is repository object.
     * 
     * @param repositories
     *            the repositories
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

        // looks like repository was deleted before we started to synchronise it
        if (repository != null && !repository.isDeleted())
        {
            doSync(repository, softSync);
        } else
        {
            log.warn("Sync requested but repository with id {} does not exist anymore.", repositoryId);
        }
    }

    /**
     * synchronization of changesets in all repositories which are in given organization
     * 
     * @param organizationId
     *            organizationId
     * @param soft
     * @param newRepoSlugs
     */
    private void syncAllInOrganization(int organizationId, boolean soft, Set<String> newRepoSlugs)
    {
        List<Repository> repositories = getAllByOrganization(organizationId);
        for (Repository repository : repositories)
        {
            if (!newRepoSlugs.contains(repository.getSlug()))
            {
                // not a new repo
                doSync(repository, soft);
            } else
            {
                // it is a new repo, we force to hard sync
                // to disable smart commits on it, make sense
                // in case when someone has just migrated
                // repo to DVCS avoiding duplicate smart commits
                doSync(repository, false);
            }
        }
    }

    /**
     * Do sync.
     * 
     * @param repository
     *            the repository
     * @param softSync
     *            the soft sync
     */
    private void doSync(Repository repository, boolean softSync)
    {
        if (repository.isLinked())
        {
            if (repository.getDvcsType().equals(GithubCommunicator.GITHUB))
            {
                if (!softSync)
                {
                    changesetService.removeAllInRepository(repository.getId());
                    repository.setLastCommitDate(null);
                    save(repository);
                }

                Date synchronizationStartedAt = new Date();
                for (BranchTip branchTip : communicatorProvider.getCommunicator(repository.getDvcsType()).getBranches(repository))
                {
                    SynchronizeChangesetMessage message = new SynchronizeChangesetMessage(repository, //
                            branchTip.getBranchName(), branchTip.getNode(), //
                            synchronizationStartedAt, //
                            null);
                    MessageKey<SynchronizeChangesetMessage> key = messagingService.get( //
                            SynchronizeChangesetMessage.class, //
                            SynchronizeChangesetMessageConsumer.KEY //
                            );
                    messagingService.publish(key, message, message.getSynchronizationTag());
                }

            } else
            {
                DefaultSynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(
                        communicatorProvider.getCommunicator(repository.getDvcsType()), repository, this, changesetService, softSync);
                synchronizer.synchronize(repository, synchronisationOperation, changesetService);

            }
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
    public List<Repository> getAllRepositories(boolean includeDeleted)
    {
        return repositoryDao.getAll(includeDeleted);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsLinkedRepositories()
    {
        return repositoryDao.existsLinkedRepositories(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepositoryRegistration enableRepository(int repoId, boolean linked)
    {
        RepositoryRegistration registration = new RepositoryRegistration();

        Repository repository = repositoryDao.get(repoId);

        if (repository != null)
        {
            registration.setRepository(repository);

            if (!linked)
            {
                synchronizer.stopSynchronization(repository);
                synchronizer.removeProgress(repository);
            }

            repository.setLinked(linked);

            String postCommitUrl = getPostCommitUrl(repository);
            registration.setCallBackUrl(postCommitUrl);
            try
            {
                addOrRemovePostcommitHook(repository, postCommitUrl);
                registration.setCallBackUrlInstalled(linked);
                updateAdminPermission(repository, true);
            } catch (SourceControlException.PostCommitHookRegistrationException e)
            {
                log.debug("Could not add or remove postcommit hook", e);
                registration.setCallBackUrlInstalled(!linked);
                updateAdminPermission(repository, false);
            }

            log.debug("Enable repository [{}]", repository);
            repositoryDao.save(repository);
        }

        return registration;
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
     * @param repository
     *            the repository
     * @param post
     *            commit callback url
     */
    private void addOrRemovePostcommitHook(Repository repository, String postCommitCallbackUrl)
    {
        DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());

        if (repository.isLinked())
        {
            communicator.setupPostcommitHook(repository, postCommitCallbackUrl);
            // TODO: move linkRepository to setupPostcommitHook if possible
            communicator.linkRepository(repository, changesetService.findReferencedProjects(repository.getId()));
        } else
        {
            communicator.removePostcommitHook(repository, postCommitCallbackUrl);
        }
    }

    /**
     * Gets the post commit url.
     * 
     * @param repo
     *            the repo
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
    public void removeRepositories(List<Repository> repositories)
    {
        // we stop all synchronizations first to prevent starting a new redundant synchronization
        for (Repository repository : repositories)
        {
            synchronizer.stopSynchronization(repository);
        }

        for (Repository repository : repositories)
        {
            markForRemove(repository);
            // try remove postcommit hook
            if (repository.isLinked())
            {
                removePostcommitHook(repository);
                repository.setLinked(false);
            }

            repositoryDao.save(repository);
        }
    }

    private void markForRemove(Repository repository)
    {
        synchronizer.removeProgress(repository);
        repository.setDeleted(true);
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
        // remove progress
        synchronizer.removeProgress(repository);
        // delete repository record itself
        repositoryDao.remove(repository.getId());
    }

    /**
     * Removes the postcommit hook.
     * 
     * @param repository
     *            the repository
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
            log.warn(
                    "Failed to uninstall postcommit hook for repository id = " + repository.getId() + ", slug = "
                            + repository.getRepositoryUrl(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeOrphanRepositoriesAsync(final List<Repository> orphanRepositories)
    {
        removeOrphanRepositoriesExecutor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                removeOrphanRepositories(orphanRepositories);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeOrphanRepositories(List<Repository> orphanRepositories)
    {
        synchronized (removeOrphanRepositoriesLock)
        {
            for (Repository repository : orphanRepositories)
            {
                remove(repository);
            }
        }
    }

    @Override
    public void onOffLinkers(boolean enableLinkers)
    {
        log.debug("Enable linkers : " + BooleanUtils.toStringYesNo(enableLinkers));

        // remove the variable first so adding and removing linkers works
        pluginSettingsFactory.createGlobalSettings().remove(DvcsConstants.LINKERS_ENABLED_SETTINGS_PARAM);

        // add or remove linkers
        for (Repository repository : getAllRepositories())
        {
            log.debug((enableLinkers ? "Adding" : "Removing") + " linkers for" + repository.getSlug());

            DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());
            if (enableLinkers && repository.isLinked())
            {
                communicator.linkRepository(repository, changesetService.findReferencedProjects(repository.getId()));
            } else
            {
                communicator.linkRepository(repository, new HashSet<String>());
            }
        }

        if (!enableLinkers)
        {
            pluginSettingsFactory.createGlobalSettings().put(DvcsConstants.LINKERS_ENABLED_SETTINGS_PARAM, Boolean.FALSE.toString());
        }
    }

    @Override
    public DvcsUser getUser(Repository repository, String author, String rawAuthor)
    {
        log.debug("Get user information for: [ {}, {}]", author, rawAuthor);

        try
        {
            DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());
            DvcsUser user = communicator.getUser(repository, author);
            if (user instanceof DvcsUser.UnknownUser)
            {
                user.setRawAuthor(rawAuthor);
            }
            return user;
        } catch (Exception e)
        {
            log.debug("Could not load user [" + author + ", " + rawAuthor + "]", e);
            return new UnknownUser(author, rawAuthor != null ? rawAuthor : author, repository.getOrgHostUrl());
        }
    }

}
