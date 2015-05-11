package com.atlassian.jira.plugins.dvcs.spi.github;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetailsEnvelope;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.parsers.GithubChangesetFactory;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.GitHubPullRequestPageMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.GithubSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.PagedRequest;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;

@Component("githubCommunicator")
public class GithubCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(GithubCommunicator.class);

    public static final String GITHUB = "github";
    public static final int PULLREQUEST_PAGE_SIZE = 30;
    private int lastKnowNumberOfRemainingRequests;

    @Resource
    private MessagingService messagingService;

    @Resource
    private BranchService branchService;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService} dependency.
     */
    @Resource
    private GitHubEventService gitHubEventService;

    /**
     * Injected {@link GitHubRESTClient} dependency.
     */
    @Resource
    private GitHubRESTClient gitHubRESTClient;

    @Resource
    protected SyncDisabledHelper syncDisabledHelper;

    @Resource
    @ComponentImport
    private ApplicationProperties applicationProperties;

    protected final GithubClientProvider githubClientProvider;
    protected final OAuthStore oAuthStore;

    @Autowired
    public GithubCommunicator(OAuthStore oAuthStore,
            @Qualifier ("githubClientProvider") GithubClientProvider githubClientProvider)
    {
        this.oAuthStore = oAuthStore;
        this.githubClientProvider = githubClientProvider;
    }

    public void setGitHubRESTClient(GitHubRESTClient gitHubRESTClient)
    {
        this.gitHubRESTClient = gitHubRESTClient;
    }

    @Override
    public String getDvcsType()
    {
        return GITHUB;
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {

        UserService userService = new UserService(githubClientProvider.createClient(hostUrl));
        try
        {
            userService.getUser(accountName);
            return new AccountInfo(GithubCommunicator.GITHUB);

        }
        catch (IOException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(), hostUrl,
                    accountName);
        }
        return null;

    }

    public boolean isErrorInUsername(String hostUrl, String accountName){
        UserService userService = new UserService(githubClientProvider.createClient(hostUrl));
        User user;
        try
        {
          user = userService.getUser(accountName);
        }
        catch (IOException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(), hostUrl,
                    accountName);
        }
        if(user != null)
        {
            return true;
        }
        else{
            return hasExceededRateLimit(userService.getClient());
        }


    }

    @Override
    public List<Repository> getRepositories(Organization organization, List<Repository> storedRepositories)
    {
        RepositoryService repositoryService = githubClientProvider.getRepositoryService(organization);
        repositoryService.getClient().setOAuth2Token(organization.getCredential().getAccessToken());

        // We don't know if this is team account or standard account. Let's
        // first get repositories
        // by calling getOrgRepositories

        List<org.eclipse.egit.github.core.Repository> repositoriesFromOrganization;
        try
        {
            repositoriesFromOrganization = repositoryService.getOrgRepositories(organization.getName());
        }
        catch (IOException e)
        {
            // looks like this is not a team account but standard account
            repositoriesFromOrganization = Collections.emptyList();
        }
        try
        {
            // for normal account
            List<org.eclipse.egit.github.core.Repository> publicRepositoriesFromOrganization = repositoryService
                    .getRepositories(organization.getName());
            List<org.eclipse.egit.github.core.Repository> allRepositoriesFromAuthorizedUser = repositoryService
                    .getRepositories();

            Iterator<org.eclipse.egit.github.core.Repository> iteratorAll = Iterators.concat(
                    repositoriesFromOrganization.iterator(), publicRepositoriesFromOrganization.iterator(),
                    allRepositoriesFromAuthorizedUser.iterator());

            Set<Repository> repositories = new HashSet<Repository>();
            ImmutableMap<String, Repository> storedReposMap = Maps.uniqueIndex(storedRepositories, new Function<Repository, String>()
            {
                @Override
                public String apply(Repository r)
                {
                    return r.getSlug();
                }
            });

            Set<String> processed = Sets.newHashSet();

            while (iteratorAll.hasNext())
            {
                org.eclipse.egit.github.core.Repository ghRepository = iteratorAll.next();
                if (StringUtils.equalsIgnoreCase(ghRepository.getOwner().getLogin(), organization.getName()))
                {
                    String repoName = ghRepository.getName();
                    if (processed.contains(repoName))
                    {
                        continue;
                    }

                    processed.add(repoName);

                    Repository repository = new Repository();
                    repository.setSlug(repoName);
                    repository.setName(repoName);
                    repository.setFork(ghRepository.isFork());
                    if (ghRepository.isFork() && ghRepository.getParent() != null)
                    {
                        setForkOfInfo(ghRepository.getParent(), repository);
                    }
                    else if (ghRepository.isFork() && /*is new repo*/ !storedReposMap.containsKey(repoName))
                    {
                        tryFindAndSetForkOf(repositoryService, ghRepository, repository);
                    }
                    repositories.add(repository);
                }
            }

            log.debug("Found repositories: " + repositories.size());
            return new ArrayList<Repository>(repositories);
        }
        catch (RequestException e)
        {
            if (e.getStatus() == 401)
            {
                throw new SourceControlException.UnauthorisedException("Invalid credentials", e);
            }
            throw new SourceControlException("Error retrieving list of repositories", e);
        }
        catch (IOException e)
        {
            throw new SourceControlException("Error retrieving list of repositories", e);
        }
    }

    private void tryFindAndSetForkOf(RepositoryService repositoryService, org.eclipse.egit.github.core.Repository ghRepository,
            Repository repository) throws IOException
    {
        org.eclipse.egit.github.core.Repository repoDetail = repositoryService.getRepository(ghRepository.getOwner().getLogin(), ghRepository.getName());
        setForkOfInfo(repoDetail.getParent(), repository);
    }

    private void setForkOfInfo(org.eclipse.egit.github.core.Repository parentRepository, Repository repositoryTo)
    {
        Repository forkOf = new Repository();
        forkOf.setSlug(parentRepository.getName());
        forkOf.setName(parentRepository.getName());
        forkOf.setRepositoryUrl(parentRepository.getHtmlUrl());
        forkOf.setOwner(parentRepository.getOwner().getLogin());
        repositoryTo.setForkOf(forkOf);
    }

    @Override
    public Changeset getChangeset(Repository repository, String node)
    {
        CommitService commitService = githubClientProvider.getCommitService(repository);
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

        try
        {
            RepositoryCommit commit = commitService.getCommit(repositoryId, node);

            //TODO Workaround for BBC-455, we need more sophisticated solution that prevents connector to hit GitHub too often when downloading changesets
            checkRequestRateLimit(commitService.getClient());

            Changeset changeset = GithubChangesetFactory.transformToChangeset(commit, repository.getId(), null);
            changeset.setFileDetails(GithubChangesetFactory.transformToFileDetails(commit.getFiles()));

            return changeset;
        }
        catch (IOException e)
        {
            throw new SourceControlException("could not get result", e);
        }
    }

    private void checkRequestRateLimit(GitHubClient gitHubClient)
    {
        if (gitHubClient == null)
        {
            return;
        }

        int requestLimit = gitHubClient.getRequestLimit();
        int remainingRequests = gitHubClient.getRemainingRequests();

        if (requestLimit == -1 || remainingRequests == -1)
        {
            return;
        }

        double threshold = Math.ceil(0.01f * requestLimit);
        if (remainingRequests < threshold)
        {
            long sleepTime = (long) (Math.pow((remainingRequests / threshold) - 1, 2) * 60 * 60);
            log.info("Sleeping for " + sleepTime + " s to avoid request rate limit overrun");
            try
            {
                //TODO when sleeping the synchronization cannot be cancelled
                Thread.sleep(sleepTime * 1000);
            }
            catch (InterruptedException e)
            {
                //nop
            }
        }
    }

    @Override
    public ChangesetFileDetailsEnvelope getFileDetails(Repository repository, Changeset changeset)
    {
        CommitService commitService = githubClientProvider.getCommitService(repository);
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

        // Workaround for BBC-455
        checkRequestRateLimit(commitService.getClient());
        try
        {
            RepositoryCommit commit = commitService.getCommit(repositoryId, changeset.getNode());

            return new ChangesetFileDetailsEnvelope(GithubChangesetFactory.transformToFileDetails(commit.getFiles()), commit.getFiles().size());
        }
        catch (IOException e)
        {
            throw new SourceControlException("could not get result", e);
        }
    }

    @SuppressWarnings ("unused")
    public PageIterator<RepositoryCommit> getPageIterator(Repository repository, String branch)
    {
        final CommitService commitService = githubClientProvider.getCommitService(repository);

        return commitService.pageCommits(RepositoryId.create(repository.getOrgName(), repository.getSlug()),
                doTheUtfEncoding(branch), null);

    }

    /**
     * The git library encodes parameters using ISO-8859-1. Let's trick it and encode using UTF-8 instead.
     *
     * @param branch the branch name to encode as UTF-8
     * @return the UTF-8 encoded branch name
     */
    private String doTheUtfEncoding(String branch)
    {
        String isoDecoded = branch;
        try
        {
            String utfEncoded = URLEncoder.encode(branch, "UTF-8");
            isoDecoded = URLDecoder.decode(utfEncoded, "ISO-8859-1");
        }
        catch (UnsupportedEncodingException e)
        {
            log.warn("Error encoding branch name: " + branch + e.getMessage());
        }
        return isoDecoded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureHookPresent(Repository repository, String hookUrl)
    {
        try
        {
            List<GitHubRepositoryHook> hooks = gitHubRESTClient.getHooks(repository);

            //Cleanup orphan this instance related hooks.
            boolean foundChangesetHook = false;
            boolean foundPullRequesttHook = false;
            for (GitHubRepositoryHook hook : hooks)
            {
                if (GitHubRepositoryHook.NAME_WEB.equals(hook.getName()))
                {
                    String url = hook.getConfig().get(GitHubRepositoryHook.CONFIG_URL);
                    boolean isPullRequestHook = isPullRequestHook(hook);

                    if (!foundChangesetHook && hookUrl.equals(url) && !isPullRequestHook)
                    {
                        foundChangesetHook = true;
                        continue;
                    }

                    if (!foundPullRequesttHook && hookUrl.equals(url) && isPullRequestHook)
                    {
                        foundPullRequesttHook = true;
                        continue;
                    }
                    String thisHostAndRest = applicationProperties.getBaseUrl() + DvcsCommunicator.POST_HOOK_SUFFIX;
                    String postCommitHookUrl = hook.getConfig().get(GitHubRepositoryHook.CONFIG_URL);
                    if (StringUtils.startsWith(postCommitHookUrl, thisHostAndRest) && !StringUtils.startsWith(thisHostAndRest, "http://localhost:"))
                    {
                        gitHubRESTClient.deleteHook(repository, hook);
                    }
                }
            }

            if (!foundChangesetHook)
            {
                // create hook if needed
                createChangesetsHook(repository, hookUrl);
            }

            if (!foundPullRequesttHook)
            {
                // adds pull requests hook, if it does not exist
                createPullRequestsHook(repository, hookUrl);
            }
        }
        catch (UniformInterfaceException e)
        {
            if (e.getResponse().getStatus() == HttpStatus.SC_NOT_FOUND)
            {
                throw new SourceControlException.PostCommitHookRegistrationException(
                        "Could not add webhook. Possibly due to lack of admin permissions.", e);
            }
            else
            {
                throw new SourceControlException.PostCommitHookRegistrationException(
                        "Could not add webhook.", e);
            }
        }
    }

    private boolean isPullRequestHook(GitHubRepositoryHook hook)
    {
        return GitHubRepositoryHook.CONFIG_CONTENT_TYPE_JSON.equals(hook.getConfig().get(GitHubRepositoryHook.CONFIG_CONTENT_TYPE));
    }

    /**
     * Creates new hook for events related to changesets.
     *
     * @param repository on which repository
     * @param hookUrl on which URL
     */
    private void createChangesetsHook(Repository repository, String hookUrl)
    {
        GitHubRepositoryHook hook = new GitHubRepositoryHook();
        hook.setName(GitHubRepositoryHook.NAME_WEB);
        hook.setActive(true);
        hook.getEvents().add(GitHubRepositoryHook.EVENT_PUSH);
        hook.getConfig().put(GitHubRepositoryHook.CONFIG_URL, hookUrl);
        gitHubRESTClient.addHook(repository, hook);
    }

    /**
     * Creates new hook for events related to pull requests.
     *
     * @param repository on which repository
     * @param hookUrl on which URL
     */
    private void createPullRequestsHook(Repository repository, String hookUrl)
    {
        GitHubRepositoryHook hook = new GitHubRepositoryHook();
        hook.setName(GitHubRepositoryHook.NAME_WEB);
        hook.setActive(true);
        hook.getEvents().add(GitHubRepositoryHook.EVENT_PUSH);
        hook.getEvents().add(GitHubRepositoryHook.EVENT_PULL_REQUEST);
        hook.getEvents().add(GitHubRepositoryHook.EVENT_PULL_REQUEST_REVIEW_COMMENT);
        hook.getEvents().add(GitHubRepositoryHook.EVENT_ISSUE_COMMENT);
        hook.getConfig().put(GitHubRepositoryHook.CONFIG_URL, hookUrl);
        hook.getConfig().put(GitHubRepositoryHook.CONFIG_CONTENT_TYPE, GitHubRepositoryHook.CONFIG_CONTENT_TYPE_JSON);
        gitHubRESTClient.addHook(repository, hook);
    }

    @Override
    public void removePostcommitHook(Repository repository, String postCommitUrl)
    {
        final List<GitHubRepositoryHook> hooks = gitHubRESTClient.getHooks(repository);
        for (GitHubRepositoryHook hook : hooks)
        {
            if (postCommitUrl.equals(hook.getConfig().get(GitHubRepositoryHook.CONFIG_URL)))
            {
                try
                {
                    gitHubRESTClient.deleteHook(repository, hook);
                }
                catch (UniformInterfaceException e)
                {
                    throw new SourceControlException.PostCommitHookRegistrationException("Could not remove postcommit hook", e);
                }
            }
        }
    }

    @Override
    public String getCommitUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}/{2}/commit/{3}", repository.getOrgHostUrl(), repository.getOrgName(),
                repository.getSlug(), changeset.getNode());
    }

    @Override
    public String getFileCommitUrl(Repository repository, Changeset changeset, String file, int index)
    {
        return MessageFormat.format("{0}#diff-{1}", getCommitUrl(repository, changeset), index);
    }

    @Override
    public DvcsUser getUser(Repository repository, String username)
    {
        try
        {
            UserService userService = githubClientProvider.getUserService(repository);
            User ghUser = userService.getUser(username);
            String login = ghUser.getLogin();
            String name = ghUser.getName();
            String displayName = StringUtils.isNotBlank(name) ? name : login;
            String gravatarUrl = ghUser.getAvatarUrl();

            return new DvcsUser(login, displayName, null, gravatarUrl, repository.getOrgHostUrl() + "/" + login);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DvcsUser getTokenOwner(Organization organization)
    {
        try
        {
            UserService userService = githubClientProvider.getUserService(organization);
            User ghUser = userService.getUser();
            String login = ghUser.getLogin();
            String name = ghUser.getName();
            String displayName = StringUtils.isNotBlank(name) ? name : login;
            String gravatarUrl = ghUser.getAvatarUrl();

            return new DvcsUser(login, displayName, null, gravatarUrl, organization.getHostUrl() + "/" + login);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Branch> getBranches(Repository repository)
    {
        RepositoryService repositoryService = githubClientProvider.getRepositoryService(repository);

        List<Branch> branches = new ArrayList<Branch>();
        try
        {
            final List<RepositoryBranch> ghBranches = repositoryService.getBranches(RepositoryId.create(
                    repository.getOrgName(), repository.getSlug()));
            log.debug("Found branches: " + ghBranches.size());

            for (RepositoryBranch ghBranch : ghBranches)
            {
                List<BranchHead> branchHeads = new ArrayList<BranchHead>();
                BranchHead branchTip = new BranchHead(ghBranch.getName(), ghBranch.getCommit().getSha());
                branchHeads.add(branchTip);
                Branch branch = new Branch(ghBranch.getName());
                branch.setRepositoryId(repository.getId());
                branch.setHeads(branchHeads);

                if ("master".equalsIgnoreCase(ghBranch.getName()))
                {
                    branches.add(0, branch);
                }
                else
                {
                    branches.add(branch);
                }
            }
        }
        catch (IOException e)
        {
            log.info("Can not obtain branches list from repository [ " + repository.getSlug() + " ]", e);
            // we need tip changeset of the branch
            throw new SourceControlException("Could not retrieve list of branches", e);
        }
        return branches;
    }

    @Override
    public boolean supportsInvitation(Organization organization)
    {
        return false;
    }

    @Override
    public List<Group> getGroupsForOrganization(Organization organization)
    {
        return Collections.emptyList();
    }

    @Override
    public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
    {
        throw new UnsupportedOperationException("You can not invite users to github so far, ...");
    }

    @Override
    public String getBranchUrl(final Repository repository, final Branch branch)
    {
        return MessageFormat.format("{0}/{1}/{2}/tree/{3}", repository.getOrgHostUrl(), repository.getOrgName(),
                repository.getSlug(), branch.getName());
    }

    @Override
    public String getCreatePullRequestUrl(final Repository repository, final String sourceSlug, final String sourceBranch, final String destinationSlug, final String destinationBranch, final String eventSource)
    {
        return MessageFormat.format("{0}/{1}/{2}/compare/{3}...{4}",
                repository.getOrgHostUrl(),
                repository.getOrgName(),
                repository.getSlug(),
                getRef(sourceSlug, sourceBranch),
                getRef(destinationSlug, destinationBranch)
        );
    }

    @Override
    public void startSynchronisation(final Repository repo, final EnumSet<SynchronizationFlag> flags, final int auditId)
    {
        final boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);
        final boolean webHookSync = flags.contains(SynchronizationFlag.WEBHOOK_SYNC);
        final boolean changestesSync = flags.contains(SynchronizationFlag.SYNC_CHANGESETS);
        final boolean pullRequestSync = flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS);

        String[] synchronizationTags = new String[] { messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId) };
        if (changestesSync)
        {
            Date synchronizationStartedAt = new Date();
            List<Branch> branches = getBranches(repo);
            for (Branch branch : branches)
            {
                for (BranchHead branchHead : branch.getHeads())
                {
                    SynchronizeChangesetMessage message = new SynchronizeChangesetMessage(repo, //
                            branch.getName(), branchHead.getHead(), //
                            synchronizationStartedAt, //
                            null, softSync, auditId, webHookSync);
                    MessageAddress<SynchronizeChangesetMessage> key = messagingService.get( //
                            SynchronizeChangesetMessage.class, //
                            GithubSynchronizeChangesetMessageConsumer.ADDRESS //
                    );
                    messagingService.publish(key, message, softSync ? MessagingService.SOFTSYNC_PRIORITY : MessagingService.DEFAULT_PRIORITY, messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
                }
            }
            List<BranchHead> oldBranchHeads = branchService.getListOfBranchHeads(repo);
            branchService.updateBranchHeads(repo, branches, oldBranchHeads);
            branchService.updateBranches(repo, branches);
        }
        if (pullRequestSync)
        {
            if (softSync || syncDisabledHelper.isGitHubUsePullRequestListDisabled())
            {
                gitHubEventService.synchronize(repo, softSync, synchronizationTags, webHookSync);
            }
            else
            {
                GitHubPullRequestPageMessage message = new GitHubPullRequestPageMessage(null, auditId, softSync, repo, PagedRequest.PAGE_FIRST, PULLREQUEST_PAGE_SIZE, null, webHookSync);
                MessageAddress<GitHubPullRequestPageMessage> key = messagingService.get(
                        GitHubPullRequestPageMessage.class,
                        GitHubPullRequestPageMessageConsumer.ADDRESS
                );
                messagingService.publish(key, message, messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
            }
        }
    }

    @Override
    public boolean isSyncDisabled(final Repository repo, final EnumSet<SynchronizationFlag> flags)
    {
        return syncDisabledHelper.isGithubSyncDisabled();
    }

    private String getRef(String slug, String branch)
    {
        if (slug != null)
        {
            return slug + ":" + branch;
        }
        return branch;
    }

    @Override
    public void linkRepository(Repository repository, Set<String> withProjectkeys)
    {

    }

    private boolean hasExceededRateLimit(GitHubClient client){
        return client.getRemainingRequests() != -1;
    }

}
