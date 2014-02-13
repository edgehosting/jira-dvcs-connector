package it.restart.com.atlassian.jira.plugins.dvcs.github;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheFactory;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheSettings;
import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.github.impl.AbstractGitHubRESTClientImpl.WebResourceCacheKey;
import com.atlassian.jira.plugins.dvcs.github.impl.GitHubRESTClientImpl;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.sun.jersey.api.client.WebResource;

/**
 * Unit test over {@link GitHubRESTClientImpl}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
// TODO: tests are disabled, they need to have generated access token, which is not available until BBC-647 branch will be merged
@Test(enabled = false)
public class GitHubRESTClientImplTest
{

    /**
     * Manually generated access token - only for test purposes.
     */
    private static final String ACCESS_TOKEN = System.getProperty("jirabitbucketconnector.accessToken");

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

        CacheFactory cacheFactory = Mockito.mock(CacheFactory.class);
        RepositoryService repositoryService = Mockito.mock(RepositoryService.class);

        repositoryCacheMock(cacheFactory, repositoryService);

        gitHubRESTClientImpl.setCacheFactory(cacheFactory);
        gitHubRESTClientImpl.setRepositoryService(repositoryService);
        gitHubRESTClientImpl.init();
        this.testedObject = gitHubRESTClientImpl;

        initRepositoryMock();
        removeAllHooks();
    }

    /**
     * Mock, which returns each time {@link #repository}, if it is loaded from cache.
     * 
     * @param cacheFactory
     * @param repositoryService
     */
    private void repositoryCacheMock(CacheFactory cacheFactory, RepositoryService repositoryService)
    {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final ArgumentCaptor<CacheLoader<WebResourceCacheKey, WebResource>> cacheLoader = ArgumentCaptor.<CacheLoader<WebResourceCacheKey, WebResource>> forClass((Class) CacheLoader.class);
        @SuppressWarnings("unchecked")
        Cache<WebResourceCacheKey, WebResource> cache = Mockito.mock(Cache.class);
        Mockito.when(cacheFactory.getCache(Mockito.anyString(), cacheLoader.capture(), Mockito.<CacheSettings> any())).thenReturn(cache);
        Mockito.when(cache.get(Mockito.<WebResourceCacheKey> any())).then(new Answer<WebResource>()
        {

            @Override
            public WebResource answer(InvocationOnMock invocation) throws Throwable
            {
                return cacheLoader.getValue().load((WebResourceCacheKey) invocation.getArguments()[0]);
            }

        });
        Mockito.when(repositoryService.get(Mockito.anyInt())).then(new Answer<Repository>()
        {

            @Override
            public Repository answer(InvocationOnMock invocation) throws Throwable
            {
                return repository;
            }

        });
    }

    /**
     * Initializes repository mock, which is used by tests.
     */
    private void initRepositoryMock()
    {
        this.repository = Mockito.mock(Repository.class);
        Mockito.when(repository.getId()).thenReturn(1);
        Mockito.when(repository.getOrgHostUrl()).thenReturn("https://github.com");
        Mockito.when(repository.getOrgName()).thenReturn("dvcsconnectortest");
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
        GitHubRepositoryHook[] hooks = testedObject.getHooks(repository);
        Assert.assertEquals(hooks.length, 1);
    }

    @Test(dependsOnMethods = { "testAddHook", "testGetHooks" }, enabled = false)
    public void testDeleteHook()
    {
        testedObject.deleteHook(repository, createdHook);
    }

}
