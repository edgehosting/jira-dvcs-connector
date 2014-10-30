package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetailsEnvelope;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.JsonParsingException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranchesAndTags;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetWithDiffstat;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceField;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ServiceRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.URLPathFormatter;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync.OldBitbucketSynchronizeCsetMsg;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetFileTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.GroupTransformer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.transformers.RepositoryTransformer;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeActivityMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.FlightTimeInterceptor;
import com.atlassian.jira.plugins.dvcs.sync.OldBitbucketSynchronizeCsetMsgConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.jira.plugins.dvcs.util.Retryer;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.annotation.Resource;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class BitbucketCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(BitbucketCommunicator.class);

    public static final int CHANGESET_LIMIT = Integer.getInteger("bitbucket.request.changeset.limit", 15);

    public static final String BITBUCKET = "bitbucket";

    private final BitbucketLinker bitbucketLinker;
    private final String pluginVersion;
    private final BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    private final ApplicationProperties applicationProperties;

    @Resource
    private BranchService branchService;

    @Resource
    private MessagingService messagingService;
    
    @Resource
    private ChangesetDao changesetDao;

    @Resource
    private SyncDisabledHelper syncDisabledHelper;

    @Autowired
    public BitbucketCommunicator(@Qualifier ("deferredBitbucketLinker") BitbucketLinker bitbucketLinker,
            @ComponentImport PluginAccessor pluginAccessor,
            BitbucketClientBuilderFactory bitbucketClientBuilderFactory, @ComponentImport ApplicationProperties ap)
    {
        this.bitbucketLinker = bitbucketLinker;
        this.bitbucketClientBuilderFactory = bitbucketClientBuilderFactory;
        this.pluginVersion = DvcsConstants.getPluginVersion(checkNotNull(pluginAccessor));
        this.applicationProperties = checkNotNull(ap);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDvcsType()
    {
        return BITBUCKET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.noAuthClient(hostUrl).build();

            // just to call the rest
            remoteClient.getAccountRest().getUser(accountName);
            return new AccountInfo(BitbucketCommunicator.BITBUCKET);
        }
        catch (BitbucketRequestException e)
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Repository> getRepositories(Organization organization, List<Repository> storedRepositories)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forOrganization(organization).cached().build();
            List<BitbucketRepository> repositories = remoteClient.getRepositoriesRest().getAllRepositories(organization.getName());
            return RepositoryTransformer.fromBitbucketRepositories(repositories);
        }
        catch (BitbucketRequestException.Unauthorized_401 e)
        {
            log.debug("Invalid credentials", e);
            throw new SourceControlException.UnauthorisedException("Invalid credentials", e);
        }
        catch (BitbucketRequestException.BadRequest_400 e)
        {
            // We received bad request status code and we assume that an invalid
            // OAuth is the cause
            throw new SourceControlException.UnauthorisedException("Invalid credentials");
        }
        catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException(e.getMessage(), e);
        }
        catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            if (organization.isIntegratedAccount())
            {
                throw new SourceControlException.UnauthorisedException(
                        "Unexpected response was returned back from server side. Check that all provided information of account '"
                                + organization.getName()
                                + "' is valid. Basically it means: unexisting account or invalid key/secret combination.", e);
            }
            throw new SourceControlException.InvalidResponseException(
                    "The response could not be parsed. This is most likely caused by invalid credentials.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Changeset getChangeset(Repository repository, String node)
    {
        try
        {
            // get the changeset
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
            BitbucketChangeset bitbucketChangeset = remoteClient.getChangesetsRest().getChangeset(repository.getOrgName(),
                    repository.getSlug(), node);

            Changeset fromBitbucketChangeset = ChangesetTransformer.fromBitbucketChangeset(repository.getId(), bitbucketChangeset);
            return fromBitbucketChangeset;
        }
        catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get changeset [" + node + "] from " + repository.getRepositoryUrl(), e);
        }
        catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not get changeset [" + node + "] from "
                    + repository.getRepositoryUrl(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangesetFileDetailsEnvelope getFileDetails(Repository repository, Changeset changeset)
    {
        try
        {
            // get the commit statistics for changeset
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
            List<BitbucketChangesetWithDiffstat> changesetDiffStat = remoteClient.getChangesetsRest().getChangesetDiffStat(
                    repository.getOrgName(), repository.getSlug(), changeset.getNode(), Changeset.MAX_VISIBLE_FILES);
            // merge it all
            List<ChangesetFileDetail> changesetFileDetails = ChangesetFileTransformer.fromBitbucketChangesetsWithDiffstat(changesetDiffStat);
            int fileCount = getFileCount(repository, changeset, changesetFileDetails.size(), remoteClient);
            return new ChangesetFileDetailsEnvelope(changesetFileDetails, fileCount);
        }
        catch (BitbucketRequestException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException("Could not get detailed changeset [" + changeset.getNode() + "] from "
                    + repository.getRepositoryUrl(), e);
        }
        catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not get changeset [" + changeset.getNode() + "] from "
                    + repository.getRepositoryUrl(), e);
        }
    }

    private int getFileCount(final Repository repository, final Changeset changeset, final int fileDetailsSize, final BitbucketRemoteClient remoteClient)
    {
        if (fileDetailsSize < Changeset.MAX_VISIBLE_FILES)
        {
            return fileDetailsSize;
        }
        else
        {
            // if files in statistics is greater than maximum visible files, we need to find out the number of files changed
            BitbucketChangeset bitbucketChangeset = remoteClient.getChangesetsRest().getChangeset(repository.getOrgName(), repository.getSlug(), changeset.getNode());
            if (bitbucketChangeset.getFiles() != null)
            {
                return Math.max(bitbucketChangeset.getFiles().size(), fileDetailsSize);
            }
            else
            {
                log.warn("Bitbucket returned changeset ({}) without any files information, could not find out the number of changes. Using file details size instead.", changeset.getNode());
                return fileDetailsSize;
            }
        }
    }

    /**
     * getNextPage. If currentPage is null, returns the first page for the given
     * repository and include / exclude parameters.
     * 
     * @param repository
     * @param includeNodes
     * @param excludeNodes
     * @param currentPage
     * @return
     */
    public BitbucketChangesetPage getNextPage(final Repository repository, final List<String> includeNodes, final List<String> excludeNodes, final BitbucketChangesetPage currentPage)
    {
        final Progress sync = repository.getSync();

        return FlightTimeInterceptor.execute(sync, new FlightTimeInterceptor.Callable<BitbucketChangesetPage>()
        {
            @Override
            public BitbucketChangesetPage call() throws RuntimeException
            {
                BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
                return remoteClient.getChangesetsRest().getNextChangesetsPage(repository.getOrgName(),
                        repository.getSlug(),
                        includeNodes,
                        excludeNodes,
                        CHANGESET_LIMIT,
                        currentPage);
            }
        });
    }

    private List<String> extractBranchHeadsFromBranches(List<Branch> branches)
    {
        List<String> result = new ArrayList<String>();
        for (Branch branch : branches)
        {
            for (BranchHead branchHead : branch.getHeads())
            {
                result.add(branchHead.getHead());
            }
        }

        return result;
    }

    private List<String> extractBranchHeads(List<BranchHead> branchHeads)
    {
        List<String> result = new ArrayList<String>();
        for (BranchHead branchHead : branchHeads)
        {
            if (!StringUtils.isBlank(branchHead.getHead()))
            {
                result.add(branchHead.getHead());
            }
        }

        return result;
    }

    @Override
    public List<Branch> getBranches(Repository repository)
    {
        List<Branch> branches = new ArrayList<Branch>();
        BitbucketBranchesAndTags branchesAndTags = retrieveBranchesAndTags(repository);

        List<BitbucketBranch> bitbucketBranches = branchesAndTags.getBranches();
        for (BitbucketBranch bitbucketBranch : bitbucketBranches)
        {
            List<String> bitbucketHeads = bitbucketBranch.getHeads();
            List<BranchHead> heads = new ArrayList<BranchHead>();

            for (String head : bitbucketHeads)
            {
                // make sure "default" branch is first in the list
                if (bitbucketBranch.isMainbranch())
                {
                    heads.add(0, new BranchHead(bitbucketBranch.getName(), head));
                }
                else
                {
                    heads.add(new BranchHead(bitbucketBranch.getName(), head));
                }
            }

            Branch branch = new Branch(bitbucketBranch.getName());
            branch.setRepositoryId(repository.getId());
            branch.setHeads(heads);
            branches.add(branch);
        }

        return branches;
    }

    private BitbucketBranchesAndTags retrieveBranchesAndTags(final Repository repository)
    {
        return new Retryer<BitbucketBranchesAndTags>().retry(new Callable<BitbucketBranchesAndTags>()
        {
            @Override
            public BitbucketBranchesAndTags call() throws Exception
            {
                return getBranchesAndTags(repository);
            }
        });
    }

    private BitbucketBranchesAndTags getBranchesAndTags(final Repository repository)
    {
        final Progress sync = repository.getSync();

        return FlightTimeInterceptor.execute(sync, new FlightTimeInterceptor.Callable<BitbucketBranchesAndTags>()
        {
            @Override
            public BitbucketBranchesAndTags call()
            {
                try
                {
                    BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).cached().build();
                    // Using undocumented https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/branches-tags
                    return remoteClient.getBranchesAndTagsRemoteRestpoint().getBranchesAndTags(repository.getOrgName(), repository.getSlug());
                }
                catch (BitbucketRequestException e)
                {
                    log.debug("Could not retrieve list of branches", e);
                    throw new SourceControlException("Could not retrieve list of branches", e);
                }
                catch (JsonParsingException e)
                {
                    log.debug("The response could not be parsed", e);
                    throw new SourceControlException.InvalidResponseException("Could not retrieve list of branches", e);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureHookPresent(Repository repository, String postCommitUrl)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).closeIdleConnections().build();

        try
        {
            if (!cleanupAndGetExists(repository, postCommitUrl, remoteClient, ServiceRemoteRestpoint.SERVICE_TYPE_POST))
            {
                remoteClient.getServicesRest().addPOSTService(repository.getOrgName(), // owner
                        repository.getSlug(), postCommitUrl);
            }
        }
        catch (BitbucketRequestException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not add postcommit hook", e);
        }
        try
        {
            if (!cleanupAndGetExists(repository, postCommitUrl, remoteClient, ServiceRemoteRestpoint.SERVICE_TYPE_PULL_REQUEST_POST))
            {
                remoteClient.getServicesRest().addPullRequestPOSTService(repository.getOrgName(), // owner
                        repository.getSlug(), postCommitUrl);
            }

        }
        catch (BitbucketRequestException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not add pull request hook", e);
        }
    }
    
    /**
     * Cleanup orphan hooks related to this instance.
     * 
     * @return <code>true</code> if required hook already installed (so you don't need to install new one),
     * <code>false</code> otherwise 
     */
    private boolean cleanupAndGetExists(Repository repository, String postCommitUrl, BitbucketRemoteClient remoteClient, String type)
    {
        ServiceRemoteRestpoint servicesRest = remoteClient.getServicesRest();
        List<BitbucketServiceEnvelope> services = servicesRest.getAllServices(repository.getOrgName(), // owner
                repository.getSlug());
        
        String thisHostAndRest = applicationProperties.getBaseUrl() + DvcsCommunicator.POST_HOOK_SUFFIX;
        
        boolean found = false;
        
        for (BitbucketServiceEnvelope bitbucketServiceEnvelope : services)
        {
            String serviceType = bitbucketServiceEnvelope.getService().getType();
            if (type.equals(serviceType))
            {
                for (BitbucketServiceField serviceField : bitbucketServiceEnvelope.getService().getFields())
                {
                    boolean fieldNameIsUrl = serviceField.getName().equals("URL");
                    
                    if (!fieldNameIsUrl || !serviceField.getValue().startsWith(thisHostAndRest)) 
                    {
                        continue;
                    }

                    boolean isRequiredPostCommitUrl = serviceField.getValue().equals(postCommitUrl);

                    if (!found && isRequiredPostCommitUrl)
                    {
                        found = true;
                    }
                    // If the hook is on localhost then we don't clean up as otherwise the tests mess with each other
                    else if(!serviceField.getValue().startsWith("http://localhost:"))
                    {
                        servicesRest.deleteService(repository.getOrgName(), repository.getSlug(), bitbucketServiceEnvelope.getId());
                    }
                }
            }
        }
        return found;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkRepository(Repository repository, Set<String> withProjectkeys)
    {
        try
        {
            bitbucketLinker.linkRepository(repository, withProjectkeys);
        }
        catch (Exception e)
        {
            log.warn("Failed to link repository " + repository.getName() + " : " + e.getClass() + " :: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkRepositoryIncremental(Repository repository, Set<String> withPossibleNewProjectkeys)
    {
        try
        {
            bitbucketLinker.linkRepositoryIncremental(repository, withPossibleNewProjectkeys);
        }
        catch (Exception e)
        {
            log.warn("Failed to do incremental repository linking " + repository.getName() + " : " + e.getClass() + " :: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePostcommitHook(Repository repository, String postCommitUrl)
    {
        try
        {
            bitbucketLinker.unlinkRepository(repository);
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
            List<BitbucketServiceEnvelope> services = remoteClient.getServicesRest().getAllServices(repository.getOrgName(), // owner
                    repository.getSlug());

            for (BitbucketServiceEnvelope bitbucketServiceEnvelope : services)
            {
                for (BitbucketServiceField serviceField : bitbucketServiceEnvelope.getService().getFields())
                {
                    boolean fieldNameIsUrl = serviceField.getName().equals("URL");
                    boolean fieldValueIsRequiredPostCommitUrl = serviceField.getValue().equals(postCommitUrl);

                    if (fieldNameIsUrl && fieldValueIsRequiredPostCommitUrl)
                    {
                        remoteClient.getServicesRest().deleteService(repository.getOrgName(), // owner
                                repository.getSlug(), bitbucketServiceEnvelope.getId());
                    }
                }
            }
        }
        catch (BitbucketRequestException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not remove postcommit hook", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}/{2}/changeset/{3}?dvcsconnector={4}", repository.getOrgHostUrl(), repository.getOrgName(),
                repository.getSlug(), changeset.getNode(), pluginVersion);
    }

    @Override
    public String getBranchUrl(Repository repository, Branch branch)
    {
        return MessageFormat.format("{0}/{1}/{2}/branch/{3}", repository.getOrgHostUrl(), repository.getOrgName(), repository.getSlug(),
                branch.getName());
    }

    @Override
    public String getCreatePullRequestUrl(Repository repository, String sourceSlug, final String sourceBranch, String destinationSlug,
            final String destinationBranch, String eventSource)
    {
        return URLPathFormatter.format("{0}/{1}/{2}/pull-request/new?source={3}/{4}&dest={5}/{6}&event_source={7}",
                repository.getOrgHostUrl(), repository.getOrgName(), repository.getSlug(), sourceSlug, sourceBranch, destinationSlug,
                destinationBranch, eventSource);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index)
    {
        return MessageFormat.format("{0}#chg-{1}", getCommitUrl(repository, changeset), file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DvcsUser getUser(Repository repository, String author)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forRepository(repository).build();
        BitbucketAccount bitbucketAccount = remoteClient.getAccountRest().getUser(author);
        String username = bitbucketAccount.getUsername();
        String fullName = bitbucketAccount.getFirstName() + " " + bitbucketAccount.getLastName();
        String avatar = bitbucketAccount.getAvatar();
        return new DvcsUser(username, fullName, null, avatar, repository.getOrgHostUrl() + "/" + username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DvcsUser getTokenOwner(Organization organization)
    {
        BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forOrganization(organization).build();
        BitbucketAccount bitbucketAccount = remoteClient.getAccountRest().getCurrentUser();
        String username = bitbucketAccount.getUsername();
        String fullName = bitbucketAccount.getFirstName() + " " + bitbucketAccount.getLastName();
        String avatar = bitbucketAccount.getAvatar();
        return new DvcsUser(username, fullName, null, avatar, organization.getHostUrl() + "/" + username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Group> getGroupsForOrganization(Organization organization)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forOrganization(organization).build();
            List<BitbucketGroup> groups = remoteClient.getGroupsRest().getGroups(organization.getName()); // owner

            return GroupTransformer.fromBitbucketGroups(groups);

        }
        catch (BitbucketRequestException.Forbidden_403 e)
        {
            log.debug("Could not get groups for organization [" + organization.getName() + "]");
            throw new SourceControlException.Forbidden_403(e);

        }
        catch (BitbucketRequestException e)
        {
            log.debug("Could not get groups for organization [" + organization.getName() + "]");
            throw new SourceControlException(e);

        }
        catch (JsonParsingException e)
        {
            log.debug(e.getMessage(), e);
            throw new SourceControlException.InvalidResponseException("Could not parse response [" + organization.getName()
                    + "]. This is most likely caused by invalid credentials.", e);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsInvitation(Organization organization)
    {
        return organization.getDvcsType().equalsIgnoreCase(BITBUCKET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
    {
        try
        {
            BitbucketRemoteClient remoteClient = bitbucketClientBuilderFactory.forOrganization(organization).build();
            for (String groupSlug : groupSlugs)
            {
                log.debug("Going invite " + userEmail + " to group " + groupSlug + " of bitbucket organization " + organization.getName());
                remoteClient.getAccountRest().inviteUser(organization.getName(), userEmail, organization.getName(), groupSlug);
            }
        }
        catch (BitbucketRequestException exception)
        {
            log.warn("Failed to invite user {} to organization {}. Response HTTP code {}", new Object[] { userEmail,
                    organization.getName(), exception.getClass().getName() });
        }
    }

    @Override
    public void startSynchronisation(Repository repo, EnumSet<SynchronizationFlag> flags, int auditId)
    {
        final boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);
        final boolean webHookSync = flags.contains(SynchronizationFlag.WEBHOOK_SYNC);
        final boolean changestesSync = flags.contains(SynchronizationFlag.SYNC_CHANGESETS);
        final boolean pullRequestSync = flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS);

        if (changestesSync)
        {
            // sync csets
            BranchFilterInfo filterNodes = getFilterNodes(repo);
            processBitbucketCsetSync(repo, softSync, filterNodes, auditId, webHookSync);

            branchService.updateBranchHeads(repo, filterNodes.newBranches, filterNodes.oldHeads);
            branchService.updateBranches(repo, filterNodes.newBranches);
        }
        // sync pull requests
        if (pullRequestSync)
        {
            processBitbucketPrSync(repo, softSync, auditId, webHookSync);
        }
    }

    @Override
    public boolean isSyncDisabled(final Repository repo, final EnumSet<SynchronizationFlag> flags)
    {
        return syncDisabledHelper.isBitbucketSyncDisabled();
    }

    protected BranchFilterInfo getFilterNodes(Repository repository)
    {
        List<Branch> newBranches = getBranches(repository);
        List<BranchHead> oldBranches = branchService.getListOfBranchHeads(repository);

        List<String> exclude = extractBranchHeads(oldBranches);

        BranchFilterInfo filter = new BranchFilterInfo(newBranches, oldBranches, exclude);
        return filter;
    }

    public static String getApiUrl(String hostUrl)
    {
        return hostUrl + "/!api/1.0";
    }

    private static class BranchFilterInfo
    {
        private List<Branch> newBranches;
        private List<BranchHead> oldHeads;
        private List<String> oldHeadsHashes;

        public BranchFilterInfo(List<Branch> newBranches, List<BranchHead> oldHeads, List<String> oldHeadsHashes)
        {
            super();
            this.newBranches = newBranches;
            this.oldHeads = oldHeads;
            this.oldHeadsHashes = oldHeadsHashes;
        }
    }

    private void processBitbucketCsetSync(Repository repository, boolean softSync, BranchFilterInfo filterNodes, int auditId, boolean webHookSync)
    {
        List<Branch> newBranches = filterNodes.newBranches;

        if (filterNodes.oldHeads.isEmpty() && changesetDao.getChangesetCount(repository.getId()) > 0)
        {
            log.info("No previous branch heads were found, switching to old changeset synchronization for repository [{}].",
                    repository.getId());
            Date synchronizationStartedAt = new Date();
            for (Branch branch : newBranches)
            {
                for (BranchHead branchHead : branch.getHeads())
                {
                    OldBitbucketSynchronizeCsetMsg message = new OldBitbucketSynchronizeCsetMsg(repository, //
                            branchHead.getName(), branchHead.getHead(), //
                            synchronizationStartedAt, //
                            null, softSync, auditId, webHookSync);
                    MessageAddress<OldBitbucketSynchronizeCsetMsg> key = messagingService.get( //
                            OldBitbucketSynchronizeCsetMsg.class, //
                            OldBitbucketSynchronizeCsetMsgConsumer.KEY //
                            );
                    messagingService.publish(key, message, softSync ? MessagingService.SOFTSYNC_PRIORITY
                            : MessagingService.DEFAULT_PRIORITY, messagingService.getTagForSynchronization(repository), messagingService
                            .getTagForAuditSynchronization(auditId));
                }
            }
        }
        else
        {
            if (CollectionUtils.isEmpty(getInclude(filterNodes)))
            {
                log.debug("No new changesets detected for repository [{}].", repository.getSlug());
                return;
            }
            MessageAddress<BitbucketSynchronizeChangesetMessage> key = messagingService.get(BitbucketSynchronizeChangesetMessage.class,
                    BitbucketSynchronizeChangesetMessageConsumer.KEY);
            Date synchronizationStartedAt = new Date();

            BitbucketSynchronizeChangesetMessage message = new BitbucketSynchronizeChangesetMessage(repository, synchronizationStartedAt,
                    (Progress) null, createInclude(filterNodes), filterNodes.oldHeadsHashes, null,
                    asNodeToBranches(filterNodes.newBranches), softSync, auditId, webHookSync);

            messagingService.publish(key, message, softSync ? MessagingService.SOFTSYNC_PRIORITY : MessagingService.DEFAULT_PRIORITY,
                    messagingService.getTagForSynchronization(repository), messagingService.getTagForAuditSynchronization(auditId));
        }
    }

    private List<String> createInclude(BranchFilterInfo filterNodes)
    {
        List<String> newHeadsNodes = extractBranchHeadsFromBranches(filterNodes.newBranches);
        if (newHeadsNodes != null && filterNodes.oldHeadsHashes != null)
        {
            newHeadsNodes.removeAll(filterNodes.oldHeadsHashes);
        }
        return newHeadsNodes;
    }

    protected void processBitbucketPrSync(Repository repo, boolean softSync, int auditId, boolean webHookSync)
    {
        MessageAddress<BitbucketSynchronizeActivityMessage> key = messagingService.get( //
                BitbucketSynchronizeActivityMessage.class, //
                BitbucketSynchronizeActivityMessageConsumer.KEY //
                );
        messagingService.publish(key, new BitbucketSynchronizeActivityMessage(repo, softSync, repo.getActivityLastSync(), auditId, webHookSync),
                softSync ? MessagingService.SOFTSYNC_PRIORITY : MessagingService.DEFAULT_PRIORITY,
                messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
    }

    private Collection<String> getInclude(BranchFilterInfo filterNodes)
    {
        List<String> newNodes = extractBranchHeadsFromBranches(filterNodes.newBranches);
        if (newNodes != null && filterNodes.oldHeadsHashes != null)
        {
            newNodes.removeAll(filterNodes.oldHeadsHashes);
        }
        return newNodes;
    }

    private Map<String, String> asNodeToBranches(List<Branch> list)
    {
        Map<String, String> changesetBranch = new HashMap<String, String>();
        for (Branch branch : list)
        {
            for (BranchHead branchHead : branch.getHeads())
            {
                changesetBranch.put(branchHead.getHead(), branch.getName());
            }
        }
        return changesetBranch;
    }
}
