package com.atlassian.jira.plugins.dvcs.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser.UnknownUser;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryRegistration;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync.OldBitbucketSynchronizeCsetMsg;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeActivityMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.GithubSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.OldBitbucketSynchronizeCsetMsgConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
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


    @Resource
    private DvcsCommunicatorProvider communicatorProvider;

    @Resource
    private RepositoryDao repositoryDao;

    @Resource
    private RepositoryActivityDao repositoryActivityDao;

    @Resource
    private MessagingService messagingService;

    @Resource
    private Synchronizer synchronizer;

    @Resource
    private ChangesetService changesetService;

    @Resource
    private BranchService branchService;

    @Resource
    private ApplicationProperties applicationProperties;

    @Resource
    private PluginSettingsFactory pluginSettingsFactory;

    @Resource
    private ChangesetCache changesetCache;

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
        EnumSet<SynchronizationFlag> synchronizationFlags = EnumSet.of(SynchronizationFlag.SYNC_CHANGESETS, SynchronizationFlag.SYNC_PULL_REQUESTS);
        if (soft)
        {
            synchronizationFlags.add(SynchronizationFlag.SOFT_SYNC);
        }
        syncAllInOrganization(organization.getId(), synchronizationFlags, newRepoSlugs);
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

            // if linked install post commit hook
            if (savedRepository.isLinked())
            {
                try
                {
                    addOrRemovePostcommitHook(savedRepository, getPostCommitUrl(savedRepository));
                } catch (SourceControlException.PostCommitHookRegistrationException e)
                {
                    log.warn("Adding postcommit hook for repository "
                            + savedRepository.getRepositoryUrl() + " failed: ", e);
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
     * @param organizationId
     *            organizationId
     * @param flags
     * @param newRepoSlugs
     */
    private void syncAllInOrganization(int organizationId, EnumSet<SynchronizationFlag> flags, Set<String> newRepoSlugs)
    {
        List<Repository> repositories = getAllByOrganization(organizationId);
        for (Repository repository : repositories)
        {
            if (!newRepoSlugs.contains(repository.getSlug()))
            {
                // not a new repo
                doSync(repository, flags);
            } else
            {
                // it is a new repo, we force to hard sync
                // to disable smart commits on it, make sense
                // in case when someone has just migrated
                // repo to DVCS avoiding duplicate smart commits
                doSync(repository, flags);
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
    @SuppressWarnings("unchecked")
    private void doSync(Repository repository, EnumSet<SynchronizationFlag> flags)
    {
        boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);

        if (skipSync(repository, softSync)) {
            return;
        }

        if (repository.isLinked())
        {
            if (!softSync)
            {
                // we are doing full sync, lets delete all existing changesets
                // also required as GHCommunicator.getChangesets() returns only changesets not already stored in database
                changesetService.removeAllInRepository(repository.getId());
                branchService.removeAllBranchHeadsInRepository(repository.getId());
                repository.setLastCommitDate(null);
                if (flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS))
                {
                    repositoryActivityDao.removeAll(repository);
                    repository.setActivityLastSync(null);
                }
                save(repository);
            }

            if (repository.getDvcsType().equals(GithubCommunicator.GITHUB))
            {
                Date synchronizationStartedAt = new Date();
                for (BranchHead branchHead : communicatorProvider.getCommunicator(repository.getDvcsType()).getBranches(repository))
                {
                    SynchronizeChangesetMessage message = new SynchronizeChangesetMessage(repository, //
                            branchHead.getName(), branchHead.getHead(), //
                            synchronizationStartedAt, //
                            null, softSync);
                    MessageKey<SynchronizeChangesetMessage> key = messagingService.get( //
                            SynchronizeChangesetMessage.class, //
                            GithubSynchronizeChangesetMessageConsumer.KEY //
                            );
                    messagingService.publish(key, message, UUID.randomUUID().toString());
                }

            } else
            {
                /*BranchFilterInfo filterNodes = getFilterNodes(repository);
                processBitbucketSync(repository, softSync, filterNodes);
                updateBranchHeads(repository, filterNodes.newHeads, filterNodes.oldHeads);*/
            }

            if (flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS))
            {
                MessageKey<SynchronizeChangesetMessage> key = messagingService.get( //
                        BitbucketSynchronizeActivityMessage.class, //
                        BitbucketSynchronizeActivityMessageConsumer.KEY //
                        );
                messagingService.publish(key, new BitbucketSynchronizeActivityMessage(repository, softSync), UUID.randomUUID().toString());
            }
        }
    }

    private boolean skipSync(Repository repository, boolean softSync)
    {
        Progress progress = synchronizer.getProgress(repository.getId());
        return progress != null && !progress.isFinished();
    }

    @SuppressWarnings("unchecked")
    protected void processBitbucketSync(Repository repository, boolean softSync, BranchFilterInfo filterNodes)
    {
        List<BranchHead> newBranchHeads = filterNodes.newHeads;

        if (filterNodes.oldHeads.isEmpty() && !changesetCache.isEmpty(repository.getId()))
        {
            log.info("No previous branch heads were found, switching to old changeset synchronization for repository [{}].", repository.getId());
            Date synchronizationStartedAt = new Date();
            for (BranchHead branchHead : newBranchHeads)
            {
                OldBitbucketSynchronizeCsetMsg message = new OldBitbucketSynchronizeCsetMsg(repository, //
                        branchHead.getName(), branchHead.getHead(), //
                        synchronizationStartedAt, //
                        null, newBranchHeads, softSync);
                MessageKey<OldBitbucketSynchronizeCsetMsg> key = messagingService.get( //
                        OldBitbucketSynchronizeCsetMsg.class, //
                        OldBitbucketSynchronizeCsetMsgConsumer.KEY //
                        );
                messagingService.publish(key, message, UUID.randomUUID().toString());
            }
        } else
        {
            if (CollectionUtils.isEmpty(getInclude(filterNodes))) {
                log.debug("No new changesets detected for repository [{}].", repository.getSlug());
                return;
            }
            MessageKey<BitbucketSynchronizeChangesetMessage> key = messagingService.get(
                    BitbucketSynchronizeChangesetMessage.class,
                    BitbucketSynchronizeChangesetMessageConsumer.KEY
                    );
            Date synchronizationStartedAt = new Date();

            BitbucketSynchronizeChangesetMessage message = new BitbucketSynchronizeChangesetMessage(repository, synchronizationStartedAt,
                    (Progress) null, filterNodes.newHeads, filterNodes.oldHeadsHashes, 1, asNodeToBranches(filterNodes.newHeads), softSync);

            messagingService.publish(key, message, UUID.randomUUID().toString());
        }
    }

    private Collection<String> getInclude(BranchFilterInfo filterNodes)
    {
        List<String> newNodes = extractBranchHeads(filterNodes.newHeads);
        if (newNodes != null && filterNodes.oldHeadsHashes != null)
        {
            newNodes.removeAll(filterNodes.oldHeadsHashes);
        }
        return newNodes;
    }

    protected void updateBranchHeads(Repository repo, List<BranchHead> newBranchHeads, List<BranchHead> oldHeads)
    {
        branchService.updateBranchHeads(repo, newBranchHeads, oldHeads);
    }

    protected BranchFilterInfo getFilterNodes(Repository repository)
    {
        CachingDvcsCommunicator cachingCommunicator = (CachingDvcsCommunicator) communicatorProvider
                .getCommunicator(BitbucketCommunicator.BITBUCKET);
        BitbucketCommunicator communicator = (BitbucketCommunicator) cachingCommunicator.getDelegate();
        List<BranchHead> newBranches = communicator.getBranches(repository);
        List<BranchHead> oldBranches = communicator.getOldBranches(repository);

        List<String> exclude = extractBranchHeads(oldBranches);

        BranchFilterInfo filter = new BranchFilterInfo(newBranches, oldBranches, exclude);
        return filter;
    }

    private List<String> extractBranchHeads(List<BranchHead> branchHeads)
    {
        if (branchHeads == null)
        {
            return null;
        }
        List<String> result = new ArrayList<String>();
        for (BranchHead branchHead : branchHeads)
        {
            result.add(branchHead.getHead());
        }
        return result;
    }

    private Map<String, String> asNodeToBranches(List<BranchHead> list)
    {
        Map<String, String> changesetBranch = new HashMap<String, String>();
        for (BranchHead branchHead : list)
        {
            changesetBranch.put(branchHead.getHead(), branchHead.getName());
        }
        return changesetBranch;
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
     * @param repository the repository
     * @param postCommitCallbackUrl       commit callback url
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
        // delete branch heads saved for repository
        branchService.removeAllBranchHeadsInRepository(repository.getId());
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
            log.warn("Failed to uninstall postcommit hook for repository id = " + repository.getId() + ", slug = "
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
    private static class BranchFilterInfo {

        private List<BranchHead> newHeads;
        private List<BranchHead> oldHeads;
        private List<String> oldHeadsHashes;

        public BranchFilterInfo(List<BranchHead> newHeads, List<BranchHead> oldHeads, List<String> oldHeadsHashes)
        {
            super();
            this.newHeads = newHeads;
            this.oldHeads = oldHeads;
            this.oldHeadsHashes = oldHeadsHashes;
        }
    }

}
