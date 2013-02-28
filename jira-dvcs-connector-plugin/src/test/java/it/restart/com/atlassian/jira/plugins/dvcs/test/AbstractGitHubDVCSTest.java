package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.JiraGithubOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import com.google.common.io.Files;

/**
 * Abstract, common implementation for all GitHub tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public abstract class AbstractGitHubDVCSTest extends AbstractDVCSTest
{

    /**
     * Repository owner.
     */
    protected static final String USERNAME = "jirabitbucketconnector";

    /**
     * Appropriate {@link #USERNAME} password.
     */
    protected static final String PASSWORD = System.getProperty("jirabitbucketconnector.password");

    /**
     * @see #getGitHubClient()
     */
    private GitHubClient gitHubClient;

    /**
     * GitHub OAuth for {@link #USERNAME}.
     */
    private OAuth gitHubOAuth;

    /**
     * GitHub {@link RepositoryService}
     */
    private RepositoryService repositoryService;

    /**
     * Map between test repository URI and created repository.
     * 
     * @see #addTestRepository(String)
     */
    private Map<String, Repository> uriToRemoteRepository = new HashMap<String, Repository>();

    /**
     * Map between test repository URI and local directory of this repository.
     */
    private Map<String, Git> uriToLocalRepository = new HashMap<String, Git>();

    /**
     * Prepares common test environment.
     */
    @BeforeClass
    public void onTestsEnvironmentSetup()
    {
        super.onTestsEnvironmentSetup();
        new MagicVisitor(getJiraTestedProduct()).visit(GithubLoginPage.class).doLogin();

        // Creates & adds OAuth settings
        gitHubOAuth = new MagicVisitor(getJiraTestedProduct()).visit(GithubOAuthPage.class).addConsumer(
                getJiraTestedProduct().getProductInstance().getBaseUrl());
        getJiraTestedProduct().visit(JiraGithubOAuthPage.class).setCredentials(gitHubOAuth.key, gitHubOAuth.secret);

        // adds GitHub account into Jira
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.addOrganization(RepositoriesPageController.GITHUB, USERNAME, false);
    }

    /**
     * Cleans common test environment.
     */
    @AfterClass(alwaysRun = true)
    public void onTestsEnvironmentCleanUp()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(getJiraTestedProduct());
        rpc.getPage().deleteAllOrganizations();

        // removes OAuth from GitHub
        new MagicVisitor(getJiraTestedProduct()).visit(GithubOAuthPage.class, gitHubOAuth.applicationId).removeConsumer();

        // log out from GitHub
        new MagicVisitor(getJiraTestedProduct()).visit(GithubLoginPage.class).doLogout();
    }

    /**
     * Destroys test environment.
     */
    @AfterMethod(alwaysRun = true)
    public void onTestCleanUp()
    {
        super.onTestCleanUp();

        for (Repository repository : uriToRemoteRepository.values())
        {
            deleteTestRepository(repository.generateId());
        }
    }

    /**
     * Creates test repository for provided URI, which will be automatically clean up, when test is finished.
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     * @return created repository
     */
    protected void addTestRepository(String repositoryUri)
    {

        gitHubClient = GitHubClient.createClient("https://github.com/stanislav-dvorscak");
        gitHubClient.setCredentials(USERNAME, PASSWORD);
        repositoryService = new RepositoryService(gitHubClient);

        // removes repository if it was not properly removed during clean up
        if (isRepositoryExists(repositoryUri))
        {
            deleteTestRepository(repositoryUri);
        }
        createTestRepository(repositoryUri);
    }

    /**
     * @return Client used by GitHub REST services.
     */
    protected GitHubClient getGitHubClient()
    {
        return gitHubClient;
    }

    /**
     * @param repositoryUri
     *            e.g.: owner/name
     * @return local repository for provided repository uri
     */
    protected Git getLocalRepository(String repositoryUri)
    {
        return uriToLocalRepository.get(repositoryUri);
    }

    /**
     * @param repositoryUri
     *            e.g.: owner/name
     * @return remote repository for provided repository uri
     */
    protected Repository getRemoteRepository(String repositoryUri)
    {
        return uriToRemoteRepository.get(repositoryUri);
    }

    /**
     * Initializes repository - git init.
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     */
    protected void init(String repositoryUri)
    {
        Git localRepository = getLocalRepository(repositoryUri);
        Repository remoteRepository = getRemoteRepository(repositoryUri);

        try
        {
            InitCommand initCommand = Git.init();
            initCommand.setDirectory(localRepository.getRepository().getDirectory().getParentFile());
            initCommand.call();
        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }

        try
        {
            StoredConfig config = localRepository.getRepository().getConfig();
            config.setString("remote", "origin", "url", remoteRepository.getCloneUrl());
            config.save();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates branch on provided repository - git branch name
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     * @param name
     *            of branch
     */
    protected void createBranch(String repositoryUri, String name)
    {
        try
        {
            CreateBranchCommand createBranchCommand = getLocalRepository(repositoryUri).branchCreate();
            createBranchCommand.setName(name);
            createBranchCommand.call();
        } catch (RefAlreadyExistsException e)
        {
            throw new RuntimeException(e);

        } catch (RefNotFoundException e)
        {
            throw new RuntimeException(e);

        } catch (InvalidRefNameException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Checkout on provided repository - git checkout.
     * 
     * @param repositoryUri
     *            over which repository e.g.: owner/name
     * @param name
     *            name to checkout e.g.: branch name
     */
    protected void checkout(String repositoryUri, String name)
    {
        try
        {
            CheckoutCommand checkoutCommand = getLocalRepository(repositoryUri).checkout();
            checkoutCommand.setName(name);
            checkoutCommand.call();
        } catch (RefAlreadyExistsException e)
        {
            throw new RuntimeException(e);

        } catch (RefNotFoundException e)
        {
            throw new RuntimeException(e);

        } catch (InvalidRefNameException e)
        {
            throw new RuntimeException(e);

        } catch (CheckoutConflictException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Creates and adds file to repository - git add.
     * 
     * @param repositoryUri
     *            for which repository
     * @param filePath
     *            repository relative path of file, parents directories will be automatically created
     * @param content
     *            of new file
     */
    protected void addFile(String repositoryUri, String filePath, byte[] content)
    {
        Git git = getLocalRepository(repositoryUri);

        File targetFile = new File(git.getRepository().getDirectory().getParentFile(), filePath);
        targetFile.getParentFile().mkdirs();
        try
        {
            targetFile.createNewFile();
            FileOutputStream output = new FileOutputStream(targetFile);
            output.write(content);
            output.close();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        try
        {
            AddCommand addCommand = git.add();
            addCommand.addFilepattern(filePath);
            addCommand.call();
        } catch (NoFilepatternException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Commits current changes.
     * 
     * @param repositoryUri
     *            for which repository
     * @param message
     *            commit message
     * @return SHA-1 commit id
     */
    protected String commit(String repositoryUri, String message)
    {
        try
        {
            CommitCommand commitCommand = getLocalRepository(repositoryUri).commit();
            commitCommand.setMessage(message);
            RevCommit commit = commitCommand.call();
            return commit.getId().getName();
        } catch (NoHeadException e)
        {
            throw new RuntimeException(e);

        } catch (NoMessageException e)
        {
            throw new RuntimeException(e);

        } catch (UnmergedPathsException e)
        {
            throw new RuntimeException(e);

        } catch (ConcurrentRefUpdateException e)
        {
            throw new RuntimeException(e);

        } catch (WrongRepositoryStateException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Push current state to remote repository.
     * 
     * @param repositoryUri
     *            e.g. owner/name
     * @param username
     *            committer username
     * @param password
     *            committer password
     */
    protected void push(String repositoryUri, String username, String password)
    {
        try
        {
            PushCommand pushCommand = getLocalRepository(repositoryUri).push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            pushCommand.call();
        } catch (InvalidRemoteException e)
        {
            throw new RuntimeException(e);

        } catch (TransportException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Push current state to remote repository.
     * 
     * @param repositoryUri
     *            e.g. owner/name
     * @param username
     *            committer username
     * @param password
     *            committer password
     * @param reference
     *            e.g.: name of branch
     */
    protected void push(String repositoryUri, String username, String password, String reference)
    {
        try
        {
            Git localRepository = getLocalRepository(repositoryUri);
            PushCommand pushCommand = localRepository.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            pushCommand.setRefSpecs(new RefSpec(localRepository.getRepository().getRef(reference).getName()));
            pushCommand.call();

        } catch (InvalidRemoteException e)
        {
            throw new RuntimeException(e);

        } catch (TransportException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Open pull request over provided repository, head and base information.
     * 
     * @param repositoryUri
     *            e.g.:owner/name
     * @param title
     *            title of Pull request
     * @param description
     *            description of Pull request
     * @param head
     *            from which head
     * @param base
     *            to which base
     */
    protected void openPullRequest(String repositoryUri, String title, String description, String head, String base)
    {
        Repository remoteRepository = getRemoteRepository(repositoryUri);
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
            new PullRequestService(getGitHubClient()).createPullRequest(remoteRepository, request);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param repositoryUri
     *            e.g.: owner/name
     * @return True when test repository exists.
     */
    private boolean isRepositoryExists(String repositoryUri)
    {
        try
        {
            return repositoryService.getRepository(RepositoryId.createFromId(repositoryUri)) != null;

        } catch (RequestException e)
        {
            if (e.getStatus() == 404)
            {
                return false;

            } else
            {
                throw new RuntimeException(e);
            }

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates provided test repository.
     * 
     * @param repositoryUri
     *            repository uri - e.g.: owner/name
     */
    private void createTestRepository(String repositoryUri)
    {
        // eGit initialization
        Repository remoteRepository = new Repository();
        RepositoryId repositoryId = RepositoryId.createFromId(repositoryUri);
        remoteRepository.setName(repositoryId.getName());

        try
        {
            if (USERNAME.equals(repositoryId.getOwner()))
            {
                remoteRepository = repositoryService.createRepository(remoteRepository);

            } else
            {
                remoteRepository = repositoryService.createRepository(repositoryId.getOwner(), remoteRepository);

            }

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        uriToRemoteRepository.put(repositoryUri, remoteRepository);

        // jGit initialization
        try
        {
            org.eclipse.jgit.lib.Repository localRepository = new FileRepository(Files.createTempDir() + "/.git");
            uriToLocalRepository.put(repositoryUri, new Git(localRepository));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes provided test repository.
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     */
    private void deleteTestRepository(String repositoryUri)
    {
        RepositoryId repositoryId = RepositoryId.createFromId(repositoryUri);
        try
        {
            gitHubClient.delete("/repos/" + repositoryId.getOwner() + "/" + repositoryId.getName());
            try
            {
                // deleting is asynchronous process, we have to waiting
                Thread.sleep(10000);
            } catch (InterruptedException e)
            {
                // nothing to do
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        uriToRemoteRepository.remove(repositoryUri);

        Git localRepository = uriToLocalRepository.remove(repositoryUri);
        if (localRepository != null)
        {
            localRepository.getRepository().close();
            try
            {
                FileUtils.deleteDirectory(localRepository.getRepository().getDirectory().getParentFile());

            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

}
