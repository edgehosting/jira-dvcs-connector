package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.event.CarefulEventService;
import com.atlassian.jira.plugins.dvcs.event.ThreadPoolUtil;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser.UnknownUser;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryRegistration;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.SYNC_CHANGESETS;
import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.SYNC_PULL_REQUESTS;
import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class RepositoryServiceImpl implements RepositoryService
{
    @VisibleForTesting
    static final String SYNC_REPOSITORY_LIST_LOCK = RepositoryService.class.getName() + ".syncRepositoryList";

    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    @Resource
    private DvcsCommunicatorProvider communicatorProvider;

    @Resource
    private RepositoryDao repositoryDao;

    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Resource
    private Synchronizer synchronizer;

    @Resource
    private ChangesetService changesetService;

    @Resource
    private BranchService branchService;

    @Resource
    @ComponentImport
    private ApplicationProperties applicationProperties;

    @Resource
    @ComponentImport
    private PluginSettingsFactory pluginSettingsFactory;

    @Resource
    private GitHubEventService gitHubEventService;

    @Resource
    private SyncAuditLogDao syncAuditDao;

    @Resource
    private ClusterLockServiceFactory clusterLockServiceFactory;

    @Resource
    private CarefulEventService eventService;

    private ClusterLockService clusterLockService;

    private ThreadPoolExecutor repositoryDeletionExecutor = ThreadPoolUtil.newSingleThreadExecutor(
            ThreadFactories.namedThreadFactory("DVCSConnector.RepositoryDeletion"));

    @PostConstruct
    public void init()
    {
        clusterLockService = clusterLockServiceFactory.getClusterLockService();
    }

    @VisibleForTesting
    public void setThreadPoolExecutor(ThreadPoolExecutor executor) {
        this.repositoryDeletionExecutor = checkNotNull(executor);
    }

    /**
     * Stops the executor.
     */
    @PreDestroy
    public void destroy() throws Exception
    {
        // Stop processing messages and also ignore the other messages in the queue
        //  removal of orphan repositories could be triggered in two places:
        //    1) scheduled job to remove any repository whose organization is null
        //    2) after removing the organization
        //  2) is actually covered by 1). And 1) could always pick up from where it stopped last time.
        //  Thus it's safe to cancel the rest of the tasks in the queue.
        repositoryDeletionExecutor.shutdown();
        repositoryDeletionExecutor.getQueue().clear();

        if (!repositoryDeletionExecutor.awaitTermination(1, TimeUnit.MINUTES))
        {
            log.error("Unable properly shutdown repository deletion executor.");
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
    public Repository   get(int repositoryId)
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
    public void syncRepositoryList(final Organization organization, final boolean soft)
    {
        final Lock lock = clusterLockService.getLockForName(SYNC_REPOSITORY_LIST_LOCK);
        lock.lock();
        try
        {
            log.debug("Synchronizing list of repositories");

            InvalidOrganizationManager invalidOrganizationsManager = new InvalidOrganizationsManagerImpl(pluginSettingsFactory);
            invalidOrganizationsManager.setOrganizationValid(organization.getId(), true);

            // get repositories from the dvcs hosting server
            DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());

            // get local repositories
            List<Repository> storedRepositories = repositoryDao.getAllByOrganization(organization.getId(), true);

            List<Repository> remoteRepositories;

            try
            {
                remoteRepositories = communicator.getRepositories(organization, storedRepositories);
            } catch (SourceControlException.UnauthorisedException e)
            {
                // we could not load repositories, we can't continue
                // mark the organization as invalid
                invalidOrganizationsManager.setOrganizationValid(organization.getId(), false);
                throw e;
            }

            // BBC-231 somehow we ended up with duplicated repositories on QA-EACJ
            removeDuplicateRepositories(organization, storedRepositories);
            // update names of existing repositories in case their names changed
            updateExistingRepositories(storedRepositories, remoteRepositories);
            // repositories that are no longer on hosting server will be marked as deleted
            removeDeletedRepositories(storedRepositories, remoteRepositories);
            // new repositories will be added to the database
            Set<String> newRepoSlugs = addNewReposReturnNewSlugs(storedRepositories, remoteRepositories, organization);

            // start asynchronous changesets synchronization for all linked repositories in organization
            EnumSet<SynchronizationFlag> synchronizationFlags = EnumSet.of(SYNC_CHANGESETS, SYNC_PULL_REQUESTS);
            if (soft)
            {
                synchronizationFlags.add(SynchronizationFlag.SOFT_SYNC);
            }
            syncAllInOrganization(organization.getId(), synchronizationFlags, newRepoSlugs);

        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public void syncRepositoryList(Organization organization)
    {
        syncRepositoryList(organization, true);

    }

    /**
     * Removes any repositories with duplicate slugs.
     *
     * @param organization the owning organisation
     * @param storedRepositories the repositories from which to remove the duplicates
     */
    private void removeDuplicateRepositories(final Organization organization, final List<Repository> storedRepositories)
    {
        final Set<String> existingRepositories = new HashSet<String>();
        for (final Repository repository : storedRepositories)
        {
            final String slug = repository.getSlug();
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
     * @param organization       the organization
     */
    private Set<String> addNewReposReturnNewSlugs(List<Repository> storedRepositories, List<Repository> remoteRepositories, Organization organization)
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
     * Updates existing repositories
     * - undelete existing deleted
     * - updates names.
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
                localRepo.setLogo(remoteRepo.getLogo());
                localRepo.setFork(remoteRepo.isFork());
                localRepo.setForkOf(remoteRepo.getForkOf());
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
    public void sync(int repositoryId, EnumSet<SynchronizationFlag> flags)
    {
        Repository repository = get(repositoryId);

        // looks like repository was deleted before we started to synchronise it
        if (repository != null && !repository.isDeleted())
        {
            doSync(repository, flags);
        } else
        {
            log.warn("Sync requested but repository with id {} does not exist anymore.", repositoryId);
        }
    }

    /**
     * synchronization of changesets in all repositories which are in given organization
     *
     * @param organizationId organizationId
     * @param flags flags
     * @param newRepoSlugs newRepoSlugs
     */
    private void syncAllInOrganization(int organizationId, EnumSet<SynchronizationFlag> flags, Set<String> newRepoSlugs)
    {
        List<Repository> repositories = getAllByOrganization(organizationId);
        for (Repository repository : repositories)
        {
            if (!newRepoSlugs.contains(repository.getSlug()))
            {
                // not a new repo
                try
                {
                    doSync(repository, flags);
                }
                catch (SourceControlException.SynchronizationDisabled e)
                {
                    // ignoring
                }
            } else
            {
                // it is a new repo, we force to hard sync
                // to disable smart commits on it, make sense
                // in case when someone has just migrated
                // repo to DVCS avoiding duplicate smart commits
                EnumSet<SynchronizationFlag> newFlags = EnumSet.copyOf(flags);
                newFlags.remove(SynchronizationFlag.SOFT_SYNC);

                try
                {
                    doSync(repository, newFlags);
                }
                catch (SourceControlException.SynchronizationDisabled e)
                {
                    // ignoring
                }

                if (repository.isLinked())
                {
                    try
                    {
                        addOrRemovePostcommitHook(repository, getPostCommitUrl(repository));
                    }
                    catch (SourceControlException.PostCommitHookRegistrationException e)
                    {
                        log.warn("Adding postcommit hook for repository "
                                + repository.getRepositoryUrl() + " failed. Probably insufficient permission", e);
                        updateAdminPermission(repository, false);
                        // if the user didn't have rights to add post commit hook, just unlink the repository
                        repositoryDao.save(repository);
                    }
                    catch (Exception e)
                    {
                        log.warn("Adding postcommit hook for repository "
                                + repository.getRepositoryUrl() + " failed: ", e);
                        // we could not add hooks for some reason, let's leave the repository unlinked
                        repositoryDao.save(repository);
                    }
                }
            }
        }
    }

    /**
     * Do sync.
     *
     * @param repository the repository
     * @param flags the flags
     */
    private void doSync(Repository repository, EnumSet<SynchronizationFlag> flags)
    {
        synchronizer.doSync(repository, flags);
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

    @Override
    public List<Repository> getAllRepositories(String dvcsType, boolean includeDeleted)
    {
        return repositoryDao.getAllByType(dvcsType, includeDeleted);
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
    public RepositoryRegistration  enableRepository(int repoId, boolean linked)
    {
        RepositoryRegistration registration = new RepositoryRegistration();

        Repository repository = repositoryDao.get(repoId);

        if (repository != null)
        {
            registration.setRepository(repository);

            // un/pause possible synchronization
            synchronizer.pauseSynchronization(repository, !linked);

            repository.setLinked(linked);

            String postCommitUrl = getPostCommitUrl(repository);
            registration.setCallBackUrl(postCommitUrl);
            try
            {
                addOrRemovePostcommitHook(repository, postCommitUrl);
                registration.setCallBackUrlInstalled(linked);
                updateAdminPermission(repository, true);
            }
            catch (SourceControlException.PostCommitHookRegistrationException e)
            {
                log.warn("Error when " + (linked ? "adding": "removing") + " web hooks for repository " + repository.getRepositoryUrl(), e);
                registration.setCallBackUrlInstalled(!linked);
                updateAdminPermission(repository, false);
            }
            catch (Exception e)
            {
                log.warn("Error when " + (linked ? "adding" : "removing") + " web hooks for repository " + repository.getRepositoryUrl(), e);
                registration.setCallBackUrlInstalled(!linked);
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
     * @param repository the repository
     * @param postCommitCallbackUrl commit callback url
     */
    private void addOrRemovePostcommitHook(Repository repository, String postCommitCallbackUrl)
    {
        DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());

        if (repository.isLinked())
        {
            communicator.ensureHookPresent(repository, postCommitCallbackUrl);
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
        return applicationProperties.getBaseUrl() + DvcsCommunicator.POST_HOOK_SUFFIX + repo.getId() + "/sync";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRepositories(List<Repository> repositories)
    {
        for (Repository repository : repositories)
        {
            if (!repository.isDeleted())
            {
                repository.setDeleted(true);
                repositoryDao.save(repository);
                synchronizer.pauseSynchronization(repository, true);
            }

            // try remove postcommit hook
            if (repository.isLinked())
            {
                removePostcommitHook(repository);
                repository.setLinked(false);
                repositoryDao.save(repository);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Repository repository)
    {
        long startTime = System.currentTimeMillis();
        synchronizer.stopSynchronization(repository);
        eventService.discardEvents(repository);

        // try remove postcommit hook
        if (repository.isLinked())
        {
            removePostcommitHook(repository);
        }
        // remove all changesets from DB that references this repository
        changesetService.removeAllInRepository(repository.getId());
        // remove progress
        synchronizer.removeProgress(repository);
        // delete branches saved for repository
        branchService.removeAllBranchHeadsInRepository(repository.getId());
        branchService.removeAllBranchesInRepository(repository.getId());
        // remove pull requests things
        gitHubEventService.removeAll(repository);
        repositoryPullRequestDao.removeAll(repository);
        // remove sync logs
        syncAuditDao.removeAllForRepo(repository.getId());
        // delete repository record itself
        repositoryDao.remove(repository.getId());
        log.debug("Repository {} was deleted in {} ms", repository.getId(), System.currentTimeMillis() - startTime);
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
            log.warn("Failed to uninstall postcommit hook for repository id = " + repository.getId() + ", slug = "
                    + repository.getRepositoryUrl(), e);
        }
    }

    @Override
    public void removeOrphanRepositories(final List<Repository> orphanRepositories)
    {
        // submit as multiple tasks so that we could quickly terminate the executor
        log.debug("Starting to remove {} orphan repositories", orphanRepositories.size());
        for (final Repository orphanRepository : orphanRepositories)
        {
            repositoryDeletionExecutor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        remove(orphanRepository);
                        log.debug("Removed orphan repository {}", orphanRepository);
                    }
                    catch (Exception e)
                    {
                        log.info("Unexpected exception when removing orphan repository " + orphanRepository, e);
                    }
                }
            });
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

    /**
     * {@inheritDoc}
     */
    @Override
    public DvcsUser getUser(Repository repository, String author, String rawAuthor)
    {
        log.debug("Get user information for: [ {}, {}]", author, rawAuthor);
        DvcsCommunicator communicator = communicatorProvider.getCommunicator(repository.getDvcsType());

        DvcsUser user = null;

        if (!Strings.isNullOrEmpty(author))
        {
            try
            {
                user = communicator.getUser(repository, author);
            } catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Could not load user [" + author + ", " + rawAuthor + "]", e);
                } else
                {
                    log.warn("Could not load user [" + author + ", " + rawAuthor + "]: " + e.getMessage());
                }
                return getUnknownUser(repository, author, rawAuthor);
            }
        }

        return user != null ? user : getUnknownUser(repository, author, rawAuthor);
    }

    @Override
    public Set<String> getEmails(Repository repository, DvcsUser user)
    {
        return changesetService.findEmails(repository.getId(), user.getUsername());
    }

    /**
     * Creates user, which is unknown - it means he does not exist as real user inside a repository. But we still want to provide some
     * information about him.
     * 
     * @param repository
     *            system, which should know, who is the provided user
     * @param username
     *            of user or null/empty string if does not exist
     * @param rawUser
     *            DVCS representation of user, for git/mercurial is it: <i>Full Name &lt;email&gt;</i>
     * @return "unknown" user
     */
    private UnknownUser getUnknownUser(Repository repository, String username, String rawUser)
    {
        return new UnknownUser(username, rawUser != null ? rawUser : username, repository.getOrgHostUrl());
    }
}
