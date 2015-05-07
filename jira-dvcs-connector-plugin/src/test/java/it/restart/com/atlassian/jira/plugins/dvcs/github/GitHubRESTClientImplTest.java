package it.restart.com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.github.impl.GitHubRESTClientImpl;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.util.TestAccounts;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Unit test over {@link GitHubRESTClientImpl}.
 *
 * @author Stanislav Dvorscak
 */
// TODO: BBC-688: tests are disabled, they need to have generated access token, which is not available until BBC-647 branch will be merged
@Test (enabled = false)
public class GitHubRESTClientImplTest extends DvcsWebDriverTestCase
{

    /**
     * Manually generated access token - only for test purposes.
     */
    private static final String ACCESS_TOKEN = System.getProperty(TestAccounts.JIRA_BB_CONNECTOR_ACCOUNT + ".accessToken");

    /**
     * Tested object.
     */
    private GitHubRESTClient testedObject;

    /**
     * Over which repository.
     */
    private Repository repository;

    /**
     * Hook created by {@link #testAddHook()}.
     */
    private GitHubRepositoryHook createdHook;

    /**
     * Prepares environment for tests.
     */
    @BeforeClass
    public void beforeClass()
    {
        GitHubRESTClientImpl gitHubRESTClientImpl = new GitHubRESTClientImpl();

        RepositoryService repositoryService = Mockito.mock(RepositoryService.class);

        gitHubRESTClientImpl.setRepositoryService(repositoryService);
        this.testedObject = gitHubRESTClientImpl;

        initRepositoryMock();
        removeAllHooks();
    }

    /**
     * Initializes repository mock, which is used by tests.
     */
    private void initRepositoryMock()
    {
        this.repository = Mockito.mock(Repository.class);
        Mockito.when(repository.getId()).thenReturn(1);
        Mockito.when(repository.getOrgHostUrl()).thenReturn("https://github.com");
        Mockito.when(repository.getOrgName()).thenReturn(TestAccounts.DVCS_CONNECTOR_TEST_ACCOUNT);
        Mockito.when(repository.getSlug()).thenReturn("hooks");

        Credential credential = Mockito.mock(Credential.class);
        Mockito.when(credential.getAccessToken()).thenReturn(ACCESS_TOKEN);
        Mockito.when(repository.getCredential()).thenReturn(credential);
    }

    /**
     * Remove all hooks on testing repository.
     */
    private void removeAllHooks()
    {
        for (GitHubRepositoryHook hook : testedObject.getHooks(repository))
        {
            testedObject.deleteHook(repository, hook);
        }
    }

    /**
     * Unit test of {@link GitHubRESTClient#addHook(Repository, GitHubRepositoryHook)}.
     */
    @Test(enabled = false)
    public void testAddHook()
    {
        GitHubRepositoryHook hook = new GitHubRepositoryHook();
        hook.setName(GitHubRepositoryHook.NAME_WEB);
        hook.getEvents().add(GitHubRepositoryHook.EVENT_PUSH);
        hook.getConfig().put(GitHubRepositoryHook.CONFIG_URL, "http://localhost:2990/jira/rest/github/");
        hook.getConfig().put(GitHubRepositoryHook.CONFIG_CONTENT_TYPE, GitHubRepositoryHook.CONFIG_CONTENT_TYPE_JSON);
        createdHook = testedObject.addHook(repository, hook);

        Assert.assertEquals(createdHook.getName(), hook.getName());
        Assert.assertEquals(createdHook.getEvents(), hook.getEvents());
        Assert.assertEquals(createdHook.getConfig(), hook.getConfig());
    }

    /**
     * Unit test of {@link GitHubRESTClient#getHooks(Repository)}.
     */
    @Test(dependsOnMethods = "testAddHook", enabled = false)
    public void testGetHooks()
    {
        List<GitHubRepositoryHook> hooks = testedObject.getHooks(repository);
        Assert.assertEquals(hooks.size(), 1);
    }

    /**
     * Unit test of {@link GitHubRESTClient#deleteHook(Repository, GitHubRepositoryHook)}.
     */
    @Test(dependsOnMethods = { "testAddHook", "testGetHooks" }, enabled = false)
    public void testDeleteHook()
    {
        testedObject.deleteHook(repository, createdHook);
    }

}
