package com.atlassian.jira.plugins.dvcs.spi.github;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryHook;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.parsers.GithubChangesetFactory;
import com.atlassian.jira.plugins.dvcs.spi.github.parsers.GithubUserFactory;
import com.atlassian.jira.plugins.dvcs.util.Retryer;
import com.google.common.collect.Iterators;

public class GithubCommunicator implements DvcsCommunicator
{
    private static final Logger log = LoggerFactory.getLogger(GithubCommunicator.class);

    public static final String GITHUB = "github";

    private final ChangesetCache changesetCache;
    private final GithubOAuth githubOAuth;

    private final GithubClientProvider githubClientProvider;

    public GithubCommunicator(ChangesetCache changesetCache, GithubOAuth githubOAuth,
            GithubClientProvider githubClientProvider)
    {
        this.changesetCache = changesetCache;
        this.githubOAuth = githubOAuth;
        this.githubClientProvider = githubClientProvider;
    }

    @Override
    public boolean isOauthConfigured()
    {
        return StringUtils.isNotBlank(githubOAuth.getClientId())
                && StringUtils.isNotBlank(githubOAuth.getClientSecret());
    }

    @Override
    public String getDvcsType()
    {
        return GITHUB;
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        UserService userService = new UserService(GitHubClient.createClient(hostUrl));
        try
        {
            userService.getUser(accountName);
            boolean requiresOauth = StringUtils.isBlank(githubOAuth.getClientId())
                    || StringUtils.isBlank(githubOAuth.getClientSecret());

            return new AccountInfo(GithubCommunicator.GITHUB, requiresOauth);

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
                if (StringUtils.equals(ghRepository.getOwner().getLogin(), organization.getName()))
                {
                    Repository repository = new Repository();
                    repository.setSlug(ghRepository.getName());
                    repository.setName(ghRepository.getName());
                    repositories.add(repository);
                }
            }

            log.debug("Found repositories: " + repositories.size());
            return new ArrayList<Repository>(repositories);
        } catch (IOException e)
        {
            throw new SourceControlException("Error retrieving list of repositories", e);
        }
    }

    @Override
    public Changeset getDetailChangeset(Repository repository, Changeset changeset)
    {
        return getDetailChangeset(repository, changeset.getBranch(), changeset.getNode());
    }

    public Changeset getDetailChangeset(Repository repository, String branch, String node) {
        CommitService commitService = githubClientProvider.getCommitService(repository);
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

        try
        {
            RepositoryCommit commit = commitService.getCommit(repositoryId, node);
            return GithubChangesetFactory.transform(commit, repository.getId(), branch);
        } catch (IOException e)
        {
            throw new SourceControlException("could not get result", e);
        }
    }
    
    public PageIterator<RepositoryCommit> getPageIterator(final Repository repository, final String branch)
    {
        return new Retryer<PageIterator<RepositoryCommit>>().retry(new Callable<PageIterator<RepositoryCommit>>()
        {
            @Override
            public PageIterator<RepositoryCommit> call()
            {
                return getPageIteratorInternal(repository, branch);
            }
        });

    }

    private PageIterator<RepositoryCommit> getPageIteratorInternal(Repository repository, String branch)
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
    public Iterable<Changeset> getChangesets(final Repository repository, final Date lastCommitDate)
    {
        return new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                List<RepositoryBranch> branches = getBranches(repository);
                // TODO if there are more than X (20?) branches then we should
                // do something smarter...
                // maybe search for new commits in scheduler job only? (once an
                // hour)
                return new GithubChangesetIterator(changesetCache, GithubCommunicator.this, repository, branches,
                        lastCommitDate);
            }
        };
    }

    @Override
    public void setupPostcommitHook(Repository repository, String postCommitUrl)
    {
        RepositoryService repositoryService = githubClientProvider.getRepositoryService(repository);
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

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
            throw new SourceControlException("Could not add postcommit hook. " + e.getMessage(), e);
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
                    repositoryService.deleteHook(repositoryId, (int) hook.getId());
                }
            }
        } catch (IOException e)
        {
            log.warn("Error removing postcommit service [{}]", e.getMessage());
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
        final UserService userService = githubClientProvider.getUserService(repository);

        try
        {
            log.debug("Get user information for: [ {} ]", username);
            final User ghUser = userService.getUser(username);
            return GithubUserFactory.transform(ghUser);
        } catch (IOException e)
        {
            log.debug("could not load user [ " + username + " ]");
            return DvcsUser.UNKNOWN_USER;
        }
    }

    @Override
    public String getUserUrl(Repository repository, Changeset changeset)
    {
        return MessageFormat.format("{0}/{1}", repository.getOrgHostUrl(), changeset.getAuthor());
    }

    private List<RepositoryBranch> getBranches(Repository repository)
    {
        RepositoryService repositoryService = githubClientProvider.getRepositoryService(repository);

        List<RepositoryBranch> branches = new ArrayList<RepositoryBranch>();
        try
        {
            final List<RepositoryBranch> ghBranches = repositoryService.getBranches(RepositoryId.create(
                    repository.getOrgName(), repository.getSlug()));
            log.debug("Found branches: " + ghBranches.size());

            for (RepositoryBranch ghBranch : ghBranches)
            {
                if ( "master".equalsIgnoreCase(ghBranch.getName()) )
                {
                    branches.add(0, ghBranch);
                } else
                {
                    branches.add(ghBranch);
                }
            }

        } catch (IOException e)
        {
            log.info("Can not obtain branches list from repository [ {} ]", repository.getSlug());
            // we need tip changeset of the branch
            
            return Collections.emptyList();
        }
        return branches;
    }

    @Override
    public boolean validateCredentials(Organization organization)
    {
        return true;
    }

    @Override
    public boolean supportsInvitation(Organization organization)
    {
        return false;
    }

    @Override
    public Set<Group> getGroupsForOrganization(Organization organization)
    {
        return Collections.emptySet();
    }

    @Override
    public void inviteUser(Organization organization, Collection<String> groupSlugs, String userEmail)
    {
        throw new UnsupportedOperationException("You can not invite users to github so far, ...");
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
