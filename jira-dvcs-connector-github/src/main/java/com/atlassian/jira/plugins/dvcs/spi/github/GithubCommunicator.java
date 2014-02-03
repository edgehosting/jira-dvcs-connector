package com.atlassian.jira.plugins.dvcs.spi.github;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.GithubSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryHook;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.impl.OAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.BranchedChangesetIterator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.parsers.GithubChangesetFactory;
import com.google.common.collect.Iterators;

import javax.annotation.Resource;

public class GithubCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(GithubCommunicator.class);

    private final String ENABLE_GITHUB_PR_SYNCHRONIZATION_FEATURE = "dvcs.connector.synchronization.pullrequest.github";

    public static final String GITHUB = "github";

    @Resource
    private  MessagingService messagingService;

    @Resource
    private  BranchService branchService;

    @Resource
    private  ChangesetCache changesetCache;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService} dependency.
     */
    @Resource
    private GitHubEventService gitHubEventService;

    @Resource
    private FeatureManager featureManager;

    protected final GithubClientProvider githubClientProvider;
    private final HttpClient3ProxyConfig proxyConfig = new HttpClient3ProxyConfig();
    protected final OAuthStore oAuthStore;

    public GithubCommunicator(ChangesetCache changesetCache, OAuthStore oAuthStore,
            @Qualifier("githubClientProvider") GithubClientProvider githubClientProvider)
    {
        this.changesetCache = changesetCache;
        this.oAuthStore = oAuthStore;
        this.githubClientProvider = githubClientProvider;
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

        } catch (IOException e)
        {
            log.debug("Unable to retrieve account information. hostUrl: {}, account: {} " + e.getMessage(), hostUrl,
                    accountName);
        }
        return null;

    }

    @Override
    public List<Repository> getRepositories(Organization organization)
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
        } catch (IOException e)
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

            Iterator<org.eclipse.egit.github.core.Repository> iterator = Iterators.concat(
                    repositoriesFromOrganization.iterator(), publicRepositoriesFromOrganization.iterator(),
                    allRepositoriesFromAuthorizedUser.iterator());

            Set<Repository> repositories = new HashSet<Repository>();
            while (iterator.hasNext())
            {
                org.eclipse.egit.github.core.Repository ghRepository = iterator.next();
                if (StringUtils.equalsIgnoreCase(ghRepository.getOwner().getLogin(), organization.getName()))
                {
                    Repository repository = new Repository();
                    repository.setSlug(ghRepository.getName());
                    repository.setName(ghRepository.getName());
                    repository.setFork(ghRepository.isFork());
                    if (ghRepository.getParent() != null)
                    {
                        org.eclipse.egit.github.core.Repository parentRepository = ghRepository.getParent();
                        Repository forkOf = new Repository();
                        forkOf.setSlug(parentRepository.getName());
                        forkOf.setName(parentRepository.getName());
                        forkOf.setRepositoryUrl(parentRepository.getHtmlUrl());
                        forkOf.setOwner(parentRepository.getOwner().getLogin());
                        repository.setForkOf(forkOf);
                    }
                    repositories.add(repository);
                }
            }

            log.debug("Found repositories: " + repositories.size());
            return new ArrayList<Repository>(repositories);
        } catch ( RequestException e)
        {
            if ( e.getStatus() == 401 )
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
        } catch (IOException e)
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
        if (remainingRequests<threshold)
        {
            long sleepTime = (long) (Math.pow( (remainingRequests / threshold) - 1, 2) * 60 * 60);
            log.info("Sleeping for " + sleepTime + " s to avoid request rate limit overrun");
            try
            {
                //TODO when sleeping the synchronization cannot be cancelled
                Thread.sleep(sleepTime * 1000);
            } catch (InterruptedException e)
            {
                //nop
            }
        }
    }
    
    @Override
    public List<ChangesetFileDetail> getFileDetails(Repository repository, Changeset changeset)
    {
        CommitService commitService = githubClientProvider.getCommitService(repository);
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

        // Workaround for BBC-455
        checkRequestRateLimit(commitService.getClient());
        try
        {
            RepositoryCommit commit = commitService.getCommit(repositoryId, changeset.getNode());

            return GithubChangesetFactory.transformToFileDetails(commit.getFiles());
        }
        catch (IOException e)
        {
            throw new SourceControlException("could not get result", e);
        }
    }

    public PageIterator<RepositoryCommit> getPageIterator(Repository repository, String branch)
    {
        final CommitService commitService = githubClientProvider.getCommitService(repository);

        return commitService.pageCommits(RepositoryId.create(repository.getOrgName(), repository.getSlug()),
                doTheUtfEncoding(branch), null);

    }

    /**
     * The git library is encoding parameters using ISO-8859-1. Lets trick it
     * and encode UTF-8 instead
     * 
     * @param branch
     * @return
     */
    private String doTheUtfEncoding(String branch)
    {
        String isoDecoded = branch;
        try
        {
            String utfEncoded = URLEncoder.encode(branch, "UTF-8");
            isoDecoded = URLDecoder.decode(utfEncoded, "ISO-8859-1");
        } catch (UnsupportedEncodingException e)
        {
            log.warn("Error encoding branch name: " + branch + e.getMessage());
        }
        return isoDecoded;
    }

    @Override
    public Iterable<Changeset> getChangesets(final Repository repository)
    {
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                List<Branch> branches = getBranches(repository);
                return new BranchedChangesetIterator(changesetCache, GithubCommunicator.this, repository, branches);
            }
        };
    }

    @Override
    public void setupPostcommitHook(Repository repository, String postCommitUrl)
    {
        RepositoryService repositoryService = githubClientProvider.getRepositoryService(repository);
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

	    Map<String, RepositoryHook> hooksForRepo = getHooksForRepo(repositoryService, repositoryId);
        if (hooksForRepo.containsKey(postCommitUrl))
        {
            return;
        }

        final RepositoryHook repositoryHook = new RepositoryHook();
        repositoryHook.setName("web");
        repositoryHook.setActive(true);

        Map<String, String> config = new HashMap<String, String>();
        config.put("url", postCommitUrl);
        repositoryHook.setConfig(config);

        try
        {
            repositoryService.createHook(repositoryId, repositoryHook);
        } catch (IOException e)
        {
            if ((e instanceof RequestException) && ((RequestException) e).getStatus() == 422)
            {
                throw new SourceControlException.PostCommitHookRegistrationException("Could not add postcommit hook. Maximum number of postcommit hooks exceeded. ", e);

            }
            throw new SourceControlException.PostCommitHookRegistrationException("Could not add postcommit hook. Do you have administrator permissions?" , e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, RepositoryHook> getHooksForRepo(RepositoryService repositoryService,
            RepositoryId repositoryId)
    {
	    try
        {
	        List<RepositoryHook> hooks = repositoryService.getHooks(repositoryId);
	        Map<String, RepositoryHook> urlToHooks = new HashMap<String, RepositoryHook>();
	        for (RepositoryHook repositoryHook : hooks)
	        {
	            urlToHooks.put(repositoryHook.getConfig().get("url"), repositoryHook);
	        }
	        return urlToHooks;
        } catch (IOException e)
        {
        	log.info("Problem getting hooks from Github for repository '" + repositoryId + "': ", e);
        	return Collections.EMPTY_MAP;
        }
    }

    @Override
    public void removePostcommitHook(Repository repository, String postCommitUrl)
    {
        RepositoryService repositoryService = githubClientProvider.getRepositoryService(repository);
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

        try
        {
            final List<RepositoryHook> hooks = repositoryService.getHooks(repositoryId);
            for (RepositoryHook hook : hooks)
            {
                if (postCommitUrl.equals(hook.getConfig().get("url")))
                {
                    try 
                    {
                        repositoryService.deleteHook(repositoryId, (int) hook.getId());
                    } catch (ProtocolException pe)
                    {
                        //BBC-364 if delete rest call doesn't work on Java client, we try Apache HttpClient
                        log.debug("Error removing postcommit hook [{}] for repository [{}], trying Apache HttpClient.", hook.getId(), repository.getRepositoryUrl());
 
                        deleteHookByHttpClient(repository, hook);
                        
                        log.debug("Deletion was successfull.");
                    }
                }
            }
        } catch (IOException e)
        {
            throw new SourceControlException.PostCommitHookRegistrationException("Could not remove postcommit hook", e);
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
        } catch (IOException e)
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
        } catch (IOException e)
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
                } else
                {
                    branches.add(branch);
                }
            }
        } catch (IOException e)
        {
            log.info("Can not obtain branches list from repository [ "+repository.getSlug()+" ]", e);
            // we need tip changeset of the branch
            throw new SourceControlException("Could not retrieve list of branches", e);
        }
        return branches;
    }

    private void deleteHookByHttpClient(Repository repository, RepositoryHook hook) throws HttpException, IOException
    {
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());
        HttpClient httpClient = new HttpClient();
        String baseUrl = repository.getOrgHostUrl();
        if ("https://github.com".equals(baseUrl))
        {
            baseUrl = "https://api.github.com";
        } else
        {
            baseUrl = baseUrl + "/api/v3";
        }
        
        String url = baseUrl + "/repos/" + repositoryId.generateId() + "/hooks/" + hook.getId();
        HttpMethod method = new DeleteMethod(url);

        proxyConfig.configureProxy(httpClient, url);
        
        Authentication auth = new OAuthAuthentication(repository.getCredential().getAccessToken());
        auth.addAuthentication(method, httpClient);
        
        httpClient.executeMethod(method);
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
        boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);
        boolean changestesSync = flags.contains(SynchronizationFlag.SYNC_CHANGESETS);
        boolean pullRequestSync = flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS);

        String[] synchronizationTags = new String[] {messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId)};
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
                            null, softSync, auditId);
                    MessageAddress<SynchronizeChangesetMessage> key = messagingService.get( //
                            SynchronizeChangesetMessage.class, //
                            GithubSynchronizeChangesetMessageConsumer.ADDRESS //
                    );
                    messagingService.publish(key, message, softSync ? MessagingService.SOFTSYNC_PRIORITY: MessagingService.DEFAULT_PRIORITY, messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
                }
            }
            List<BranchHead> oldBranchHeads = branchService.getListOfBranchHeads(repo);
            branchService.updateBranchHeads(repo, branches, oldBranchHeads);
            branchService.updateBranches(repo, branches);
        }
        if (pullRequestSync && featureManager.isEnabled(ENABLE_GITHUB_PR_SYNCHRONIZATION_FEATURE))
        {
            gitHubEventService.synchronize(repo, softSync, synchronizationTags);
        }
    }

    private String getRef(String slug, String branch)
    {
        String ref = null;
        if (slug != null)
        {
            ref = slug + ":" + branch;
        } else
        {
            ref = branch;
        }

        return ref;
    }

    @Override
    public void linkRepository(Repository repository, Set<String> withProjectkeys)
    {

    }

    @Override
    public void linkRepositoryIncremental(Repository repository, Set<String> withPossibleNewProjectkeys)
    {

    }

}
