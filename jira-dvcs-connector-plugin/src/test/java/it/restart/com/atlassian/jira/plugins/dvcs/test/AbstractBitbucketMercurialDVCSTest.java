package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.JiraBitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketCreatePullRequestPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;

/**
 * Abstract, common implementation for all Bitbucket tests using Mercurial.
 * 
 * @author Miroslav Stencel <mstencel@atlassian.com>
 * 
 */
public abstract class AbstractBitbucketMercurialDVCSTest extends AbstractMercurialDVCSTest
{

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
     * Bitbucket OAuth for {@link #USERNAME}.
     */
    private OAuth bitbucketOAuth;

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
        bitbucketOAuth = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketOAuthPage.class).addConsumer();
        
        getJiraTestedProduct().visit(JiraBitbucketOAuthPage.class).setCredentials(bitbucketOAuth.key, bitbucketOAuth.secret);

        // adds Bitbucket account into Jira
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.addOrganization(RepositoriesPageController.BITBUCKET, ACCOUNT_NAME, false);
//        repositoriesPageController.addOrganization(RepositoriesPageController.BITBUCKET, ORGANIZATION, false);
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
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketOAuthPage.class).removeConsumer(bitbucketOAuth.applicationId);

        super.onTestsEnvironmentCleanUp();
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
        // removes repository if it was not properly removed during clean up
        if (isRepositoryExists(owner, slug, repositoryService))
        {
            deleteTestRepository(owner, slug, repositoryService);
        }
        createTestRepository(owner, slug, repositoryService);
    }

    /**
     * Forks provided repository into the {@link #ORGANIZATION}. The forked repository will be automatically destroyed after test finished.
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     * @return forked repository URI
     */
    protected BitbucketRepository fork(String owner, String repositoryName, String forkAccount, String forkPassword)
    {
        RepositoryRemoteRestpoint forkRepositoryService = createRepositoryService(forkAccount, forkPassword);
        
        BitbucketRepository remoteRepository = forkRepositoryService.forkRepository(owner, repositoryName, repositoryName, true);

        String result = getUriKey(remoteRepository.getOwner(), remoteRepository.getSlug());
        uriToRemoteRepository.put(result, new RepositoryInfo(remoteRepository, forkRepositoryService));
        createTestLocalRepository(forkAccount, repositoryName, generateCloneUrl(remoteRepository.getOwner(), remoteRepository.getSlug(), forkAccount, forkPassword));

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
    protected String openPullRequest(String owner, String repositoryName, String title, String description, String head, String base)
    {
        BitbucketCreatePullRequestPage createPullRequestPage = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketCreatePullRequestPage.class, BitbucketCreatePullRequestPage.getUrl(owner, repositoryName));
        return createPullRequestPage.createPullRequest(title, description, head, base, owner + "/" + repositoryName);
    }
    
    protected String openForkPullRequest(String owner, String repositoryName, String title, String description, String head, String base, String forkOwner)
    {
        BitbucketCreatePullRequestPage createPullRequestPage = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketCreatePullRequestPage.class, BitbucketCreatePullRequestPage.getUrl(forkOwner, repositoryName));
        return createPullRequestPage.createPullRequest(title, description, head, base, owner + "/" + repositoryName);
    }
    
    /**
     * Update pull request over provided repository
     * 
     * @param owner
     *            repository owner
     * @param repositoryName
     *               repository name
     * @return pull request url
     */
    protected String updatePullRequest(String owner, String repositoryName)
    {
        BitbucketCreatePullRequestPage pullRequestPage = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketCreatePullRequestPage.class, BitbucketCreatePullRequestPage.getUrl(owner, repositoryName));
        return pullRequestPage.createPullRequest(null, null, null, null, owner + "/" + repositoryName);
    }

    /**
     * Closes provided pull request.
     * 
     * @param owner
     *            repository owner
     * @param repositoryName
     *               repository name
     * @param pullRequestUrl
     *            pull request url to close
     */
    protected void closePullRequest(String owner, String repositoryName, String pullRequestUrl)
    {
        BitbucketPullRequestPage pullRequestPage = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketPullRequestPage.class, pullRequestUrl);
        pullRequestPage.declinePullRequest();
    }
    
    /**
     * Approves pull request
     * 
     * @param owner
     *                 repository owner
     * @param repositoryName
     *                 repository name
     * @param pullRequestUrl
     *                  url of pull request to close
     */
    protected void approvePullRequest(String owner, String repositoryName, String pullRequestUrl)
    {
        BitbucketPullRequestPage pullRequestPage = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketPullRequestPage.class, pullRequestUrl);
        pullRequestPage.approvePullRequest();
    }
    
    /**
     * Merges pull request
     * 
     * @param owner
     *                 repository owner
     * @param repositoryName
     *                 repository name
     * @param pullRequestUrl
     *                 url of pull request to merge
     */
    protected void mergePullRequest(String owner, String repositoryName, String pullRequestUrl)
    {
        BitbucketPullRequestPage pullRequestPage = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketPullRequestPage.class, pullRequestUrl);
        pullRequestPage.mergePullRequest();
    }
    
    /**
     * Adds comment to provided pull request.
     * 
     * @param pullRequestUrl
     *            pull request url
     * @param comment
     *            message
     * @return created remote comment
     */
    protected String commentPullRequest(String pullRequestUrl, String comment)
    {
        BitbucketPullRequestPage pullRequestPage = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketPullRequestPage.class, pullRequestUrl);
        return pullRequestPage.commentPullRequest(comment);
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
    private void createTestRepository(String owner, String repositoryName, RepositoryRemoteRestpoint repositoryService)
    {
        BitbucketRepository remoteRepository = createTestRemoteRepository(owner, repositoryName, repositoryService);
        createTestLocalRepository(owner, repositoryName, generateCloneUrl(remoteRepository.getOwner(), remoteRepository.getSlug(), ACCOUNT_NAME, PASSWORD));
    }

    /**
     * Creates provided test repository - remote side.
     * 
     * @param repositoryUri
     */
    private BitbucketRepository createTestRemoteRepository(String owner, String repositoryName, RepositoryRemoteRestpoint repositoryService)
    {
        BitbucketRepository remoteRepository;
        
        if (ACCOUNT_NAME.equals(owner))
        {
            remoteRepository = repositoryService.createHgRepository(repositoryName);
        } else
        {
            remoteRepository = repositoryService.createHgRepository(owner, repositoryName);
        }
        
        uriToRemoteRepository.put(getRepositoriUri(remoteRepository), new RepositoryInfo(remoteRepository, repositoryService));
        return remoteRepository;
    }

    private String getRepositoriUri(BitbucketRepository repository)
    {
        return getUriKey(repository.getOwner() , repository.getSlug());
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
        super.deleteTestRepository(repositoryUri);
    }
    
    private RepositoryRemoteRestpoint createRepositoryService(String username, String password)
    {
        // Bitbucket client setup
        AuthProvider authProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                username,
                password);
        
        BitbucketRemoteClient bitbucketClient = new BitbucketRemoteClient(authProvider);        
        return bitbucketClient.getRepositoriesRest();
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
