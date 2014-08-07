package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.google.common.io.Files;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Abstract, common implementation for all GitHub tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public abstract class AbstractGitHubDVCSTest extends AbstractDVCSTest
{

    /**
     * @see #getGitHubClient()
     */
    private GitHubClient gitHubClient;

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
     * SignIn into the GitHub.
     */
    protected abstract void signInGitHub();

    /**
     * SignOut from the GitHub.
     */
    protected abstract void signOutGitHub();

    /**
     * Creates {@link OAuth} settings necessary by Jira.
     * 
     * @return created OAuth
     */
    protected abstract OAuth createOAuthSettings();

    /**
     * Destroys {@link OAuth} settings.
     * 
     * @param oAuth
     *            Github OAuth
     */
    protected abstract void destroyOAuthSettings(OAuth oAuth);

    /**
     * @return Creates {@link GitHubClient} with filled credentials.
     */
    protected abstract GitHubClient createGitHubClient();

    /**
     * @return Adds all organizations necessary by tests.
     */
    protected abstract void addDVCSOrganizations();

    /**
     * Prepares common test environment.
     */
    @BeforeClass
    public void onTestsEnvironmentSetup()
    {
        super.onTestsEnvironmentSetup();

        signInGitHub();
        this.oAuth = createOAuthSettings();
        this.gitHubClient = createGitHubClient();
        addDVCSOrganizations();

        repositoryService = new RepositoryService(gitHubClient);
    }

    /**
     * @return Username used for authentication.
     */
    protected String getUsername()
    {
        return "jirabitbucketconnector";
    }

    /**
     * @return Password credential of {@link #getUsername()}.
     */
    protected String getPassword()
    {
        return System.getProperty("jirabitbucketconnector.password");
    }

    /**
     * @return Organization used by repository forking.
     */
    protected String getOrganization()
    {
        return "jira-dvcs-connector-org";
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

        destroyOAuthSettings(oAuth);
        signOutGitHub();
    }

    /**
     * Destroys test environment.
     */
    @AfterMethod(alwaysRun = true)
    public void onTestCleanUp()
    {
        super.onTestCleanUp();

        Iterator<Repository> valuesIterator = uriToRemoteRepository.values().iterator();
        while (valuesIterator.hasNext())
        {
            deleteTestRepository(valuesIterator.next().generateId());
            // iterator have to be refreshed, because delete method makes modification on it
            valuesIterator = uriToRemoteRepository.values().iterator();
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
     * Clones repository, which is defined by provided repository URI. Clone URL will be obtained from {@link #uriToRemoteRepository}.
     * Useful for {@link #fork(String)} repository.
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     * @param username
     *            for get access to clone
     * @param password
     *            for get access to clone
     */
    protected void clone(String repositoryUri, String username, String password)
    {
        try
        {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            cloneCommand.setURI(getRemoteRepository(repositoryUri).getCloneUrl());
            cloneCommand.setDirectory(getLocalRepository(repositoryUri).getRepository().getDirectory().getParentFile());
            cloneCommand.call();

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
     * Forks provided repository into the {@link #ORGANIZATION}. The forked repository will be automatically destroyed after test finished.
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     * @return forked repository URI
     */
    protected String fork(String repositoryUri)
    {
        RepositoryId fromRepository = RepositoryId.createFromId(repositoryUri);

        try
        {
            Repository remoteRepository = repositoryService.forkRepository(fromRepository, getOrganization());

            // wait until forked repository is prepared
            do
            {
                try
                {
                    Thread.sleep(500);
                } catch (InterruptedException e)
                {
                    // nothing to do
                }
            } while (repositoryService.getRepository(remoteRepository.getOwner().getLogin(), remoteRepository.getName()) == null);

            // builds URI of forked repository
            String result = remoteRepository.getOwner().getLogin() + "/" + remoteRepository.getName();
            uriToRemoteRepository.put(result, remoteRepository);
            createTestLocalRepository(result);

            return result;

        } catch (IOException e)
        {
            throw new RuntimeException(repositoryUri);
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
     * 
     * @param authorName
     *            name of author
     * @param authorEmail
     *            email of author
     * @return SHA-1 commit id
     */
    protected String commit(String repositoryUri, String message, String authorName, String authorEmail)
    {
        try
        {
            CommitCommand commitCommand = getLocalRepository(repositoryUri).commit();
            commitCommand.setMessage(message);
            commitCommand.setAuthor(authorName, authorEmail);
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
     *            on which repository e.g.:owner/name
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
    protected PullRequest openPullRequest(String repositoryUri, String title, String description, String head, String base)
    {
        Repository repository = getRemoteRepository(repositoryUri);

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
            PullRequest result = new PullRequestService(getGitHubClient()).createPullRequest(repository, request);

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
     * Closes provided pull request.
     * 
     * @param repositoryUri
     *            pull request owner
     * @param pullRequest
     *            to close
     */
    protected void closePullRequest(String repositoryUri, PullRequest pullRequest)
    {
        Repository repository = getRemoteRepository(repositoryUri);

        PullRequestService pullRequestService = new PullRequestService(getGitHubClient());
        try
        {
            pullRequest.setState("CLOSED");
            pullRequestService.editPullRequest(repository, pullRequest);

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }

    }

    /**
     * Adds comment to provided pull request.
     * 
     * @param repositoryUri
     * @param pullRequest
     *            pull request owner
     * @param comment
     *            message
     * @return created remote comment
     */
    protected Comment commentPullRequest(String repositoryUri, PullRequest pullRequest, String comment)
    {
        IssueService issueService = new IssueService(getGitHubClient());
        try
        {
            return issueService.createComment(getRemoteRepository(repositoryUri),
                    pullRequest.getIssueUrl().substring(pullRequest.getIssueUrl().lastIndexOf('/') + 1), comment);

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
        createTestRemoteRepository(repositoryUri);
        createTestLocalRepository(repositoryUri);
    }

    /**
     * Creates provided test repository - remote side.
     * 
     * @param repositoryUri
     */
    private void createTestRemoteRepository(String repositoryUri)
    {
        Repository remoteRepository = new Repository();
        RepositoryId repositoryId = RepositoryId.createFromId(repositoryUri);
        remoteRepository.setName(repositoryId.getName());

        try
        {
            if (getUsername().equals(repositoryId.getOwner()))
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
    }

    /**
     * Creates provided test repository - local side.
     * 
     * @param repositoryUri
     */
    private void createTestLocalRepository(String repositoryUri)
    {
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
