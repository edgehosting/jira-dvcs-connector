package com.atlassian.jira.plugins.dvcs.base.resource;

import com.atlassian.jira.plugins.dvcs.base.AbstractTestListener;
import com.atlassian.jira.plugins.dvcs.base.TestListenerDelegate;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;
import org.apache.commons.httpclient.HttpStatus;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Provides GitHub test resource related functionality.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubTestResource
{

    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(GitHubTestResource.class);

    /**
     * Base GitHub url.
     */
    public static final String URL = "https://github.com";

    /**
     * Username of user for GitHub related tests.
     */
    public static final String USER = "jirabitbucketconnector";

    /**
     * Name of user for GitHub related tests.
     */
    public static final String NAME = "Janko Hrasko";


    /**
     * Username of user for GitHub related tests.
     */
    public static final String OTHER_USER = "dvcsconnectortest";

    /**
     * Appropriate password for {@link #USER}
     */
    public static final String USER_PASSWORD = System.getProperty("jirabitbucketconnector.password");

    /**
     * Appropriate password for {@link #OTHER_USER}
     */
    public static final String OTHER_USER_PASSWORD = System.getProperty("dvcsconnectortest.password");


    /**
     * Organization for GitHub related tests.
     */
    public static final String ORGANIZATION = "jira-dvcs-connector-org";

    /**
     * Lifetime of generated repository.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    public enum Lifetime
    {
        DURING_CLASS, DURING_TEST_METHOD,
    }

    /**
     * Context infomration related to generated {@link OAuth}.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private static class OAuthContext
    {

        private final String gitHubURL;
        private final OAuth oAuth;

        public OAuthContext(String gitHubURL, OAuth oAuth)
        {
            this.gitHubURL = gitHubURL;
            this.oAuth = oAuth;
        }

    }

    /**
     * Context information related to generated repositories.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private static class RepositoryContext
    {

        /**
         * Created GitHub repository.
         */
        private final Repository repository;

        /**
         * Owner under whom was created this repository.
         */
        private final String owner;

        /**
         * Constructor.
         * 
         * @param owner
         *            {@link #owner}
         * @param repository
         *            {@link #repository}
         */
        public RepositoryContext(String owner, Repository repository)
        {
            this.owner = owner;
            this.repository = repository;
        }
    }

    /**
     * {@link MagicVisitor} dependency injected via constructor.
     */
    private final MagicVisitor magicVisitor;

    /**
     * Used by repository name generation.
     */
    private TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    /**
     * Registered owners.
     * 
     * @see #addOwner(String, GitHubClient)
     */
    private Map<String, GitHubClient> gitHubClientByOwner = new HashMap<String, GitHubClient>();

    /**
     * Created OAuths.
     */
    private Map<Lifetime, List<OAuthContext>> oAuthByLifetime = new HashMap<GitHubTestResource.Lifetime, List<OAuthContext>>();

    /**
     * Created repositories.
     * 
     * @see #addRepository(String, String, Lifetime, int)
     */
    private Map<Lifetime, List<RepositoryContext>> repositoryByLifetime = new HashMap<Lifetime, List<RepositoryContext>>();

    /**
     * Created repositories by slug.
     * 
     * @see #addRepository(String, String, Lifetime, int)
     */
    private Map<String, RepositoryContext> repositoryBySlug = new HashMap<String, RepositoryContext>();

    /**
     * Constructor.
     * 
     * @param testListenerDelegate
     */
    public GitHubTestResource(TestListenerDelegate testListenerDelegate, MagicVisitor magicVisitor)
    {
        testListenerDelegate.register(new AbstractTestListener()
        {

            @Override
            public void beforeClass()
            {
                super.beforeClass();
                GitHubTestResource.this.beforeClass();
            }

            @Override
            public void beforeMethod()
            {
                super.beforeMethod();
                GitHubTestResource.this.beforeMethod();
            }

            @Override
            public void afterMethod()
            {
                super.afterMethod();
                GitHubTestResource.this.afterMethod();
            }

            @Override
            public void afterClass()
            {
                super.afterClass();
                GitHubTestResource.this.afterClass();
            }

        });
        this.magicVisitor = magicVisitor;
    }

    // Listeners for test lifecycle.

    public void beforeClass()
    {
        repositoryByLifetime.put(Lifetime.DURING_CLASS, new LinkedList<RepositoryContext>());
        oAuthByLifetime.put(Lifetime.DURING_CLASS, new LinkedList<OAuthContext>());
    }

    /**
     * Prepares staff related to single test method.
     */
    public void beforeMethod()
    {
        repositoryByLifetime.put(Lifetime.DURING_TEST_METHOD, new LinkedList<RepositoryContext>());
        oAuthByLifetime.put(Lifetime.DURING_TEST_METHOD, new LinkedList<OAuthContext>());
    }

    /**
     * Cleans up staff related to single test method.
     */
    public void afterMethod()
    {
        for (OAuthContext oAuthContext : oAuthByLifetime.get(Lifetime.DURING_TEST_METHOD))
        {
            removeOAuth(oAuthContext.gitHubURL, oAuthContext.oAuth);
        }
        for (RepositoryContext testMethodRepository : repositoryByLifetime.remove(Lifetime.DURING_TEST_METHOD))
        {
            removeRepository(testMethodRepository.owner, testMethodRepository.repository.getName());
        }
    }

    /**
     * Cleaning staff related to this resource.
     */
    public void afterClass()
    {
        for (OAuthContext oAuthContext : oAuthByLifetime.get(Lifetime.DURING_CLASS))
        {
            removeOAuth(oAuthContext.gitHubURL, oAuthContext.oAuth);
        }
        for (RepositoryContext testMethodRepository : repositoryByLifetime.remove(Lifetime.DURING_CLASS))
        {
            removeRepository(testMethodRepository.owner, testMethodRepository.repository.getName());
        }
        for (String owner : gitHubClientByOwner.keySet())
        {
            removeExpiredRepository(owner);
        }
    }

    /**
     * Adds {@link OAuth} for provided GitHub information.
     * 
     * @param gitHubURL
     *            URL for GitHub
     * @param callbackURL
     * @param lifetime
     * @return OAuth
     */
    public OAuth addOAuth(String gitHubURL, String callbackURL, Lifetime lifetime)
    {
        magicVisitor.visit(GithubLoginPage.class, gitHubURL).doLogin();
        GithubOAuthPage gitHubOAuthPage = magicVisitor.visit(GithubOAuthPage.class, gitHubURL);
        OAuth result = gitHubOAuthPage.addConsumer(callbackURL);
        oAuthByLifetime.get(lifetime).add(new OAuthContext(gitHubURL, result));
        magicVisitor.visit(GithubLoginPage.class, gitHubURL).doLogout();
        return result;
    }

    /**
     * Removes provided {@link OAuth}.
     * 
     * @param gitHubURL
     *            url of GitHub
     * @param oAuth
     *            to remove
     * @see #addOAuth(String, String, Lifetime)
     */
    private void removeOAuth(String gitHubURL, OAuth oAuth)
    {
        magicVisitor.visit(GithubLoginPage.class, gitHubURL).doLogin();
        GithubOAuthPage gitHubOAuthPage = magicVisitor.visit(oAuth.applicationId, GithubOAuthPage.class);
        gitHubOAuthPage.removeConsumer();
        magicVisitor.visit(GithubLoginPage.class, gitHubURL).doLogout();
    }

    /**
     * Registers owner, which will be used by GitHub communication.
     * 
     * @param owner
     *            name of owner
     * @param gitHubClient
     *            appropriate GitHub client
     */
    public void addOwner(String owner, GitHubClient gitHubClient)
    {
        gitHubClientByOwner.put(owner, gitHubClient);
    }

    /**
     * Adds repository under provided owner, with random generated name based on provided name prefix.
     * 
     * @param owner
     *            of created repository
     * @param namePrefix
     *            of repository
     * @param lifetime
     *            validity of repository (when it should be clean up)
     * @param expirationDuration
     *            duration (expiration time), when can be removed the repository, even by some other test (if cleaning of repository failed,
     *            this can be used by cleaning retry)
     * @return name of created repository
     */
    public String addRepository(String owner, String namePrefix, Lifetime lifetime, int expirationDuration)
    {
        String repositoryName = timestampNameTestResource.randomName(namePrefix, expirationDuration);
        Repository repository = createRepository(owner, repositoryName);

        RepositoryContext repositoryContext = new RepositoryContext(owner, repository);
        repositoryByLifetime.get(lifetime).add(repositoryContext);
        repositoryBySlug.put(getSlug(owner, repositoryName), repositoryContext);

        return repository.getName();
    }

    /**
     * Forks provided repository into the {@link #ORGANIZATION}. The forked repository will be automatically destroyed after test finished.
     * 
     * @param owner
     * @param repositoryOwner
     * @param repositoryName
     * @param lifetime
     *            validity of repository (when it should be clean up)
     * @param expirationDuration
     *            duration (expiration time), when can be removed the repository, even by some other test (if cleaning of repository failed,
     *            this can be used by cleaning retry)
     */
    public void fork(String owner, String repositoryOwner, String repositoryName, Lifetime lifetime, int expirationDuration)
    {
        GitHubClient gitHubClient = getGitHubClient(owner);
        RepositoryService repositoryService = new RepositoryService(gitHubClient);

        try
        {
            Repository repository = repositoryService.forkRepository(RepositoryId.create(repositoryOwner, repositoryName),
                    gitHubClient.getUser().equals(owner) ? null : owner);

            // wait until forked repository is prepared
            do
            {
                sleep(5000);
            } while (repositoryService.getRepository(repository.getOwner().getLogin(), repository.getName()) == null);

            RepositoryContext repositoryContext = new RepositoryContext(owner, repository);
            repositoryByLifetime.get(lifetime).add(repositoryContext);
            repositoryBySlug.put(getSlug(owner, repositoryName), repositoryContext);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns repository for provided name.
     * 
     * @param owner
     *            of repository
     * @param name
     *            of repository
     * @return resolved repository
     */
    public Repository getRepository(String owner, String name)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, name));
        return bySlug.repository;
    }

    /**
     * Open pull request over provided repository, head and base information.
     * 
     * @param owner
     *            of repository
     * @param repositoryName
     *            on which repository
     * @param title
     *            title of Pull request
     * @param description
     *            description of Pull request
     * @param head
     *            from which head e.g.: master or organization:master
     * @param base
     *            to which base
     * @return created EGit pull request
     */
    public PullRequest openPullRequest(String owner, String repositoryName, String title, String description, String head, String base)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));

        PullRequest request = new PullRequest();
        request.setTitle(title);
        request.setBody(description);

        PullRequestMarker headMarker = new PullRequestMarker();
        headMarker.setLabel(head);
        request.setHead(headMarker);

        PullRequestMarker baseMarker = new PullRequestMarker();
        baseMarker.setLabel(base);
        request.setBase(baseMarker);

        PullRequest result = null;

        try
        {
            try
            {
                result = new PullRequestService(getGitHubClient(bySlug.owner)).createPullRequest(bySlug.repository, request);
            }
            catch (RequestException e)
            {
                // let's try once more after while
                sleep(5000);
                result = new PullRequestService(getGitHubClient(bySlug.owner)).createPullRequest(bySlug.repository, request);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // pull request creation is asynchronous process - it is necessary to wait a little bit
        // otherwise unexpected behavior can happened - like next push will be part as open pull request
        sleep(5000);

        return result;
    }

    /**
     * Update pull request over provided repository, head and base information.
     *
     * @param owner
     *            of repository
     * @param repositoryName
     *            on which repository
     * @param title
     *            title of Pull request
     * @param description
     *            description of Pull request
     * @param base
     *            to which base
     * @return created EGit pull request
     */
    public PullRequest updatePullRequest(PullRequest pullRequest, String owner, String repositoryName, String title, String description, String base)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));

        pullRequest.setTitle(title);
        pullRequest.setBody(description);

        PullRequest result;
        try
        {
            try
            {
                result = new PullRequestService(getGitHubClient(bySlug.owner)).editPullRequest(bySlug.repository, pullRequest);
            }
            catch (RequestException e)
            {
                // let's try once more after while
                sleep(5000);
                result = new PullRequestService(getGitHubClient(bySlug.owner)).editPullRequest(bySlug.repository, pullRequest);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        sleep(5000);

        return result;
    }

    /**
     * Returns slug for provided representation.
     * 
     * @param owner
     * @param repositoryName
     * @return
     */
    public String getSlug(String owner, String repositoryName)
    {
        return owner + "/" + repositoryName;
    }

    /**
     * Merges provided pull request.
     *
     * @param owner
     *            of repository
     * @param repositoryName
     *            pull request owner
     * @param pullRequest
     *            to close
     * @param commitMessage the message that will be used got the merge commit
     *
     */
    public void mergePullRequest(String owner, String repositoryName, PullRequest pullRequest, String commitMessage)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));
        PullRequestService pullRequestService = new PullRequestService(getGitHubClient(bySlug.owner));
        try
        {
            pullRequestService.merge(bySlug.repository, pullRequest.getNumber(), commitMessage);

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }

    }

    /**
     * Closes provided pull request.
     * 
     * @param owner
     *            of repository
     * @param repositoryName
     *            pull request owner
     * @param pullRequest
     *            to close
     */
    public void closePullRequest(String owner, String repositoryName, PullRequest pullRequest)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));
        PullRequestService pullRequestService = new PullRequestService(getGitHubClient(bySlug.owner));
        try
        {
            pullRequest.setState("CLOSED");
            pullRequestService.editPullRequest(bySlug.repository, pullRequest);

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }

    }

    /**
     * Gets pull request
     *
     * @param owner of repository
     * @param repositoryName repository name
     * @param pullRequestId pull request id
     * @return pull request
     */
    public PullRequest getPullRequest(String owner, String repositoryName, int pullRequestId)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));
        PullRequestService pullRequestService = new PullRequestService(getGitHubClient(bySlug.owner));
        try
        {
            return pullRequestService.getPullRequest(bySlug.repository, pullRequestId);

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Gets pull request commits
     *
     * @param owner of repository
     * @param repositoryName repository name
     * @param pullRequestId pull request id
     * @return pull request commits
     */
    public List<RepositoryCommit> getPullRequestCommits(String owner, String repositoryName, int pullRequestId)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));
        PullRequestService pullRequestService = new PullRequestService(getGitHubClient(bySlug.owner));
        try
        {
            return pullRequestService.getCommits(bySlug.repository, pullRequestId);

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Adds comment to provided pull request.
     * 
     * @param owner
     *            of repository
     * @param repositoryName
     * @param pullRequest
     *            pull request owner
     * @param comment
     *            message
     * @return created remote comment
     */
    public Comment commentPullRequest(String owner, String repositoryName, PullRequest pullRequest, String comment)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));

        IssueService issueService = new IssueService(getGitHubClient(owner));
        try
        {
            return issueService.createComment(bySlug.repository,
                    pullRequest.getIssueUrl().substring(pullRequest.getIssueUrl().lastIndexOf('/') + 1), comment);

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Adds comment to provided pull request as author.
     *
     * @param owner
     *            of repository
     * @param repositoryName
     * @param pullRequest
     *            pull request owner
     * @param comment
     *            message
     * @param
     *            author
     * @return created remote comment
     */
    public Comment commentPullRequest(String owner, String repositoryName, PullRequest pullRequest, String comment, String author)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));

        IssueService issueService = new IssueService(getGitHubClient(author));
        try
        {
            return issueService.createComment(bySlug.repository,
                    pullRequest.getIssueUrl().substring(pullRequest.getIssueUrl().lastIndexOf('/') + 1), comment);

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Returns comment with a given id
     *
     * @param owner of repository
     * @param repositoryName repository name
     * @param commentId comment id
     * @return remote comment
     */
    public Comment getPullRequestComment(String owner, String repositoryName, long commentId)
    {
        RepositoryContext bySlug = repositoryBySlug.get(getSlug(owner, repositoryName));

        IssueService issueService = new IssueService(getGitHubClient(owner));
        try
        {
            return issueService.getComment(owner, bySlug.repository.getName(), commentId);

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Resolves GitHub client.
     * 
     * @param owner
     *            of repository
     * @return gitHubClient if exists or {@link RuntimeException}
     */
    private GitHubClient getGitHubClient(String owner)
    {
        GitHubClient gitHubClient = gitHubClientByOwner.get(owner);
        if (gitHubClient == null)
        {
            throw new RuntimeException("Owner must be added, before it can be used, @see #addBeforeOwner(String, GitHubClient)");
        }
        return gitHubClient;
    }

    /**
     * Creates repository for provided name and appropriate owner.
     * 
     * @param owner
     *            of repository
     * @param name
     *            of repository
     * @return created repository
     */
    private Repository createRepository(String owner, String name)
    {
        GitHubClient gitHubClient = getGitHubClient(owner);
        RepositoryService repositoryService = new RepositoryService(gitHubClient);

        Repository repository = new Repository();
        repository.setName(name);

        try
        {
            if (gitHubClient.getUser().equals(owner))
            {
                return repositoryService.createRepository(repository);
            } else
            {
                return repositoryService.createRepository(owner, repository);
            }

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes repository for provided name by provided owner.
     * 
     * @param owner
     *            of repository
     * @param name
     *            of repository
     */
    private void removeRepository(String owner, String name)
    {
        try
        {
            // removes repository itself
            getGitHubClient(owner).delete("/repos/" + owner + "/" + name);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes all expired repository for provided owner.
     * 
     * @param owner
     *            of repositories
     * @see #addRepository(String, String, Lifetime, int)
     */
    private void removeExpiredRepository(String owner)
    {
        GitHubClient gitHubClient = getGitHubClient(owner);
        RepositoryService repositoryService = new RepositoryService(gitHubClient);

        List<Repository> repositories;
        try
        {
            repositories = repositoryService.getRepositories(owner);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        for (Repository repository : repositories)
        {
            if (timestampNameTestResource.isExpired(repository.getName()))
            {
                try
                {
                    gitHubClient.delete("/repos/" + owner + "/" + repository.getName());
                } catch (RequestException e)
                {
                    if (e.getStatus() == HttpStatus.SC_NOT_FOUND)
                    {
                        // Old GitHub Enterprise caches list of repositories and if this repository was already removed, it can be still
                        // presented in this list
                        logger.warn("Can not remove repository: " + owner + "/" + repository.getName() + ", because it was not found!", e);
                    } else
                    {
                        throw new RuntimeException(e);
                    }
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        } catch (InterruptedException e)
        {
            // nothing to do
        }
    }
}
