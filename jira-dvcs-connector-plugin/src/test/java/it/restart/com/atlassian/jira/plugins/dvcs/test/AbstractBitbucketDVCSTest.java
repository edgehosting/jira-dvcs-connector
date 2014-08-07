package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.DefaultBitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.google.common.base.Function;

/**
 * Abstract, common implementation for all Bitbucket tests
 *
 * @author Miroslav Stencel <mstencel@atlassian.com>
 *
 */
public abstract class AbstractBitbucketDVCSTest extends AbstractDVCSTest
{
    private final Dvcs dvcs;

    protected TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    public AbstractBitbucketDVCSTest(Dvcs dvcs)
    {
        this.dvcs = dvcs;
    }

    public void createBranch(String owner, String repositoryName, String branchName)
    {
        dvcs.createBranch(owner, repositoryName, branchName);
    }

    public void switchBranch(String owner, String repositoryName, String branchName)
    {
        dvcs.switchBranch(owner, repositoryName, branchName);
    }

    public void addFile(String owner, String repositoryName, String filePath, byte[] content)
    {
        dvcs.addFile(owner, repositoryName, filePath, content);
    }

    public String commit(String owner, String repositoryName, String message, String authorName, String authorEmail)
    {
        return dvcs.commit(owner, repositoryName, message, authorName, authorEmail);
    }

    public void push(String owner, String repositoryName, String username, String password)
    {
        dvcs.push(owner, repositoryName, username, password);
    }

    public void push(String owner, String repositoryName, String username, String password, String reference, boolean newBranch)
    {
        dvcs.push(owner, repositoryName, username, password, reference, newBranch);
    }

    public void push(String owner, String repositoryName, String username, String password, String reference)
    {
        dvcs.push(owner, repositoryName, username, password, reference);
    }

    public void deleteTestRepository(String repositoryUri)
    {
        dvcs.deleteTestRepository(repositoryUri);
    }

    /**
     * Repository owner.
     */
    protected static final String ACCOUNT_NAME = "jirabitbucketconnector";

    /**
     * Fork repository owner.
     */
    protected static final String FORK_ACCOUNT_NAME = "dvcsconnectortest";

    /**
     * Appropriate {@link #ACCOUNT_NAME} password.
     */
    protected static final String PASSWORD = System.getProperty("jirabitbucketconnector.password");

    /**
     * Appropriate {@link #FORK_ACCOUNT_NAME} password.
     */
    protected static final String FORK_ACCOUNT_PASSWORD = System.getProperty("dvcsconnectortest.password");

    /**
     * Map between test repository URI and created repository.
     *
     * @see #addTestRepository(String)
     */
    private Map<String, RepositoryInfo> uriToRemoteRepository = new HashMap<String, RepositoryInfo>();

    /**
     * Prepares common test environment.
     */
    @BeforeClass
    public void onTestsEnvironmentSetup()
    {

        super.onTestsEnvironmentSetup();

        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketLoginPage.class).doLogin(ACCOUNT_NAME, PASSWORD);

        // Creates & adds OAuth settings
        oAuth = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketOAuthPage.class).addConsumer();

        // getJiraTestedProduct().visit(JiraBitbucketOAuthPage.class).setCredentials(bitbucketOAuth.key, bitbucketOAuth.secret);

        // adds Bitbucket account into Jira
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.BITBUCKET, FORK_ACCOUNT_NAME, getOAuthCredentials(), false);
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

        // removes OAuth from Bitbucket
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketOAuthPage.class).removeConsumer(oAuth.applicationId);

        // log out from bitbucket
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketLoginPage.class).doLogout();

        removeExpiredRepositories(ACCOUNT_NAME, PASSWORD);
        removeExpiredRepositories(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);
    }


    /**
     * Destroys test environment.
     */
    @AfterMethod(alwaysRun = true)
    public void onTestCleanUp()
    {
        super.onTestCleanUp();

        Iterator<RepositoryInfo> valuesIterator = uriToRemoteRepository.values().iterator();
        while (valuesIterator.hasNext())
        {
            RepositoryInfo repositoryInfo = valuesIterator.next();
            deleteTestRepository(repositoryInfo.getRepository().getOwner(), repositoryInfo.getRepository().getSlug(), repositoryInfo.getRepositoryService());
            // iterator have to be refreshed, because delete method makes modification on it
            valuesIterator = uriToRemoteRepository.values().iterator();
        }
    }

    /**
     * Creates test repository for provided URI, which will be automatically clean up, when test is finished.
     *
     * @param owner
     * @param slug
     * @return created repository
     */
    protected void addTestRepository(String owner, String slug, String password)
    {
        RepositoryRemoteRestpoint repositoryService = createRepositoryService(owner, password);

        createTestRepository(owner, slug, password, repositoryService);
    }

    /**
     * Forks provided repository into the {@link #ORGANIZATION}. The forked repository will be automatically destroyed after test finished.
     *
     * @param repositoryUri
     *            e.g.: owner/name
     * @return forked repository URI
     */
    protected BitbucketRepository fork(final String owner, final String repositoryName, final String forkAccount, final String forkPassword)
    {
        final RepositoryRemoteRestpoint forkRepositoryService = createRepositoryService(forkAccount, forkPassword);

        BitbucketRepository remoteRepository = forkRepositoryService.forkRepository(owner, repositoryName, repositoryName, true);

        String result = getUriKey(remoteRepository.getOwner(), remoteRepository.getSlug());
        uriToRemoteRepository.put(result, new RepositoryInfo(remoteRepository, forkRepositoryService));

        getJiraTestedProduct().getTester().getDriver().waitUntil(new Function<WebDriver, Boolean>()
        {
            @Override
            public Boolean apply(@Nullable final WebDriver input)
            {
                return isRepositoryExists(forkAccount, repositoryName, forkRepositoryService);
            }
        }, 5);

        // waiting for fork repository to be available
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }

        dvcs.createTestLocalRepository(forkAccount, repositoryName, forkAccount, forkPassword);

        return remoteRepository;
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
     * @return pull request url
     */
    protected BitbucketPullRequest openPullRequest(String owner, String repositoryName, String password, String title, String description, String head, String base, String... reviewers)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);
        List<String> reviewersList = reviewers == null? null : Arrays.asList(reviewers);

        BitbucketPullRequest pullRequest = pullRequestRemoteRestpoint.createPullRequest(owner, repositoryName, title, description, head, base, reviewersList);

        return pullRequest;
    }

    protected BitbucketPullRequest updatePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest, String title, String description, String base)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        BitbucketPullRequest updatedPullRequest = pullRequestRemoteRestpoint.updatePullRequest(owner, repositoryName, pullRequest, title, description, base);

        return updatedPullRequest;
    }

    protected BitbucketPullRequest openForkPullRequest(String owner, String repositoryName, String title, String description, String head, String base, String forkOwner, String forkPassword)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(forkOwner, forkPassword);

        BitbucketPullRequest pullRequest = pullRequestRemoteRestpoint.createPullRequest(owner, repositoryName, title, description, forkOwner, repositoryName, head, base);

        return pullRequest;
    }

    /**
     * Closes provided pull request.
     *
     * @param owner
     *            repository owner
     * @param repositoryName
     *               repository name
     * @param pullRequest
     *            pull request to close
     */
    protected void declinePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.declinePullRequest(owner, repositoryName, pullRequest.getId(), null);
    }

    /**
     * Approves pull request
     *
     * @param owner
     *                 repository owner
     * @param repositoryName
     *                 repository name
     * @param pullRequest
     *                  pull request to close
     */
    protected void approvePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.approvePullRequest(owner, repositoryName, pullRequest.getId());
    }

    /**
     * Merges pull request
     *
     * @param owner
     *                 repository owner
     * @param repositoryName
     *                 repository name
     * @param pullRequest
     *                 url of pull request to merge
     */
    protected void mergePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.mergePullRequest(owner, repositoryName, pullRequest.getId(), "Merge message", true);
    }

    /**
     * Adds comment to provided pull request.
     *
     * @param pullRequest
     *            pull request
     * @param comment
     *            message
     * @return created remote comment
     */
    protected void commentPullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest, String comment)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);
        pullRequestRemoteRestpoint.commentPullRequest(owner, repositoryName, pullRequest.getId(), comment);
    }

    protected String getDefaultBranchName()
    {
        return dvcs.getDefaultBranchName();
    }

    /**
     * @param repositoryUri
     *            e.g.: owner/name
     * @return True when test repository exists.
     */
    private boolean isRepositoryExists(String owner, String slug, RepositoryRemoteRestpoint repositoryService)
    {
        try
        {
            return repositoryService.getRepository(owner, slug) != null;

        } catch (BitbucketRequestException.NotFound_404 e)
        {
            return false;
        }
    }

    /**
     * Creates provided test repository.
     *
     * @param owner
     * @param repositoryName
     */
    private void createTestRepository(String owner, String repositoryName, String password, RepositoryRemoteRestpoint repositoryService)
    {
        BitbucketRepository remoteRepository = createTestRemoteRepository(repositoryName, repositoryService);
        dvcs.createTestLocalRepository(owner, repositoryName, owner, password);
    }

    /**
     * Creates provided test repository - remote side.
     *
     * @param repositoryName name of the repository to be created
     * @param repositoryService configured repository service
     */
    private BitbucketRepository createTestRemoteRepository(String repositoryName, RepositoryRemoteRestpoint repositoryService)
    {
        BitbucketRepository remoteRepository;

        remoteRepository = repositoryService.createRepository(repositoryName, dvcs.getDvcsType(), false);

        uriToRemoteRepository.put(getRepositoriUri(remoteRepository), new RepositoryInfo(remoteRepository, repositoryService));
        return remoteRepository;
    }

    private String getRepositoriUri(BitbucketRepository repository)
    {
        return getUriKey(repository.getOwner(), repository.getSlug());
    }

    /**
     * Deletes provided test repository.
     *
     * @param repositoryUri
     *            e.g.: owner/name
     */
    private void deleteTestRepository(String owner, String slug, RepositoryRemoteRestpoint repositoryService)
    {
        repositoryService.removeRepository(owner, slug);
        String repositoryUri = getUriKey(owner, slug);
        uriToRemoteRepository.remove(repositoryUri);
        dvcs.deleteTestRepository(repositoryUri);
    }

    private RepositoryRemoteRestpoint createRepositoryService(String username, String password)
    {
        HttpClientProvider httpClientProvider = new HttpClientProvider();
        httpClientProvider.setUserAgent("jirabitbucketconnectortest");

        // Bitbucket client setup
        AuthProvider authProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                username,
                password,
                httpClientProvider);

        BitbucketRemoteClient bitbucketClient = new BitbucketRemoteClient(authProvider);
        return bitbucketClient.getRepositoriesRest();
    }

    private String getUriKey(String owner, String slug)
    {
        return owner + "/" + slug;
    }

    private PullRequestRemoteRestpoint getPullRequestRemoteRestpoint(String owner, String password)
    {
        BitbucketClientBuilderFactory bitbucketClientBuilderFactory = new DefaultBitbucketClientBuilderFactory(new Encryptor()
        {

            @Override
            public String encrypt(final String input, final String organizationName, final String hostUrl)
            {
                return input;
            }

            @Override
            public String decrypt(final String input, final String organizationName, final String hostUrl)
            {
                return input;
            }
        }, "DVCS Connector Tests", new HttpClientProvider());
        Credential credential = new Credential();
        credential.setAdminUsername(owner);
        credential.setAdminPassword(password);
        BitbucketRemoteClient bitbucketClient = bitbucketClientBuilderFactory.authClient("https://bitbucket.org", null, credential).apiVersion(2).build();
        return bitbucketClient.getPullRequestAndCommentsRemoteRestpoint();
    }

    private void removeExpiredRepositories(String owner, String password)
    {
        RepositoryRemoteRestpoint repositoryService = createRepositoryService(owner, password);

        for ( BitbucketRepository repository : repositoryService.getAllRepositories(owner))
        {
            if (timestampNameTestResource.isExpired(repository.getName()))
            {
                try
                {
                    repositoryService.removeRepository(repository.getName(), owner);
                }
                catch (BitbucketRequestException.NotFound_404 e) {} // the repo does not exist
            }
        }
    }

    public static class RepositoryInfo
    {
        private BitbucketRepository repository;
        private RepositoryRemoteRestpoint repositoryService;

        public RepositoryInfo(BitbucketRepository repository, RepositoryRemoteRestpoint repositoryService)
        {
            this.repository = repository;
            this.repositoryService = repositoryService;
        }

        public BitbucketRepository getRepository()
        {
            return repository;
        }

        public void setRepository(BitbucketRepository repository)
        {
            this.repository = repository;
        }

        public RepositoryRemoteRestpoint getRepositoryService()
        {
            return repositoryService;
        }

        public void setRepositoryService(RepositoryRemoteRestpoint repositoryService)
        {
            this.repositoryService = repositoryService;
        }
    }
}
