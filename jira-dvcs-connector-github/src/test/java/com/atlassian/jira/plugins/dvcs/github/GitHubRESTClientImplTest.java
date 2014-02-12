package com.atlassian.jira.plugins.dvcs.github;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.github.impl.GitHubRESTClientImpl;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Unit test over {@link GitHubRESTClientImpl}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
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
        testedObject = new GitHubRESTClientImpl();
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
        Mockito.when(repository.getOwner()).thenReturn("dvcsconnectortest");
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
    @Test
    public void testAddHook()
    {
        GitHubRepositoryHook hook = new GitHubRepositoryHook();
        hook.setName(GitHubRepositoryHook.HOOK_NAME_WEB);
        hook.getEvents().add(GitHubRepositoryHook.EVENT_PUSH);
        hook.getConfig().put("url", "http://localhost:2990/jira/rest/github/");
        hook.getConfig().put("content_type", "json");
        createdHook = testedObject.addHook(repository, hook);
        
        Assert.assertEquals(createdHook.getName(), hook.getName());
        Assert.assertEquals(createdHook.getEvents(), hook.getEvents());
        Assert.assertEquals(createdHook.getConfig(), hook.getConfig());
    }

    /**
     * Unit test of {@link GitHubRESTClient#getHooks(Repository)}.
     */
    @Test(dependsOnMethods = "testAddHook")
    public void testGetHooks()
    {
        GitHubRepositoryHook[] hooks = testedObject.getHooks(repository);
        Assert.assertEquals(hooks.length, 1);
    }

    @Test(dependsOnMethods = { "testAddHook", "testGetHooks" })
    public void testDeleteHook()
    {
        testedObject.deleteHook(repository, createdHook);
    }

}
