package com.atlassian.jira.plugins.dvcs.base.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;

import com.atlassian.jira.plugins.dvcs.base.AbstractTestListener;
import com.atlassian.jira.plugins.dvcs.base.TestListenerDelegate;

/**
 * Provides GitHub test resource related functionality.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubTestResource
{

    /**
     * Base GitHub url.
     */
    public static final String URL = "https://github.com";

    /**
     * Username of user for GitHub related tests.
     */
    public static final String USER = "jirabitbucketconnector";

    /**
     * Appropriate password for {@link #USER}
     */
    public static final String USER_PASSWORD = System.getProperty("jirabitbucketconnector.password");

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
        DURING_TEST_METHOD
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
    public GitHubTestResource(TestListenerDelegate testListenerDelegate)
    {
        testListenerDelegate.register(new AbstractTestListener()
        {
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
    }

    // Listeners for test lifecycle.

    /**
     * Prepares staff related to single test method.
     */
    public void beforeMethod()
    {
        repositoryByLifetime.put(Lifetime.DURING_TEST_METHOD, new LinkedList<RepositoryContext>());
    }

    /**
     * Cleans up staff related to single test method.
     */
    public void afterMethod()
    {
        List<RepositoryContext> testMethodRepositories = repositoryByLifetime.remove(Lifetime.DURING_TEST_METHOD);
        for (RepositoryContext testMethodRepository : testMethodRepositories)
        {
            removeRepository(testMethodRepository.owner, testMethodRepository.repository.getName());
        }
    }

    /**
     * Cleaning staff related to this resource.
     */
    public void afterClass()
    {
        for (String owner : gitHubClientByOwner.keySet())
        {
            removeExpiredRepository(owner);
        }
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
            Repository repository = repositoryService.forkRepository(RepositoryId.create(repositoryOwner, repositoryName), gitHubClient
                    .getUser().equals(owner) ? null : owner);

            // wait until forked repository is prepared
            do
            {
                try
                {
                    Thread.sleep(5000);
                } catch (InterruptedException e)
                {
                    // nothing to do
                }
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

        try
        {
            PullRequest result = new PullRequestService(getGitHubClient(bySlug.owner)).createPullRequest(bySlug.repository, request);

            // pull request creation is asynchronous process - it is necessary to wait a little bit
            // otherwise unexpected behavior can happened - like next push will be part as open pull request
            try
            {
                Thread.sleep(5000);
            } catch (InterruptedException e)
            {
                // nothing to do
            }

            return result;
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
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
        try
        {
            for (Repository repository : repositoryService.getRepositories(owner))
            {
                if (timestampNameTestResource.isExpired(repository.getName()))
                {
                    gitHubClient.delete("/repos/" + owner + "/" + repository.getName());
                }
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
