package com.atlassian.jira.plugins.dvcs.github;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.github.api.GitHubRESTClient;
import com.atlassian.jira.plugins.dvcs.github.api.model.GitHubRepositoryHook;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetailsEnvelope;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientWithTimeout;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.UserServiceFactory;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubRateLimitExceededException;
import com.atlassian.jira.plugins.dvcs.spi.github.RateLimit;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryHook;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Martin Skurla
 * @author Miroslav Stencel mstencel@atlassian.com
 */

public class GithubCommunicatorTest
{
    @Mock
    private Repository repository;
    @Mock
    private GithubClientProvider githubClientProvider;
    @Mock
    private CommitService commitService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private UserService userService;
    @Mock
    private GitHubRESTClient gitHubRESTClient;
    @Mock
    private User githubUser;
    @Mock
    private ApplicationProperties applicationProperties;
    @Captor
    private ArgumentCaptor<GitHubRepositoryHook> hookCaptor;
    @Mock
    private SyncDisabledHelper syncDisabledHelper;
    @Mock
    private MessagingService messagingService;
    @Mock
    private GitHubEventService gitHubEventService;
    @Mock
    private UserServiceFactory userServiceFactory;
    @Spy
    private GithubClientWithTimeout gitHubClient = new GithubClientWithTimeout("localhost", 8080, "http");

    private final String HOST_URL = "hostURL";
    private final String ACCOUNT_Name = "ACCOUNT_Name";

    // tested object
    private GithubCommunicator communicator;

    private static final RequestError requestError = new RequestError()
    {
        @Override
        public String getMessage()
        {
            return "API rate limit exceeded for account1. (403)";
        }
    };

    private static final RateLimit rateLimit = new RateLimit(10, 0, System.currentTimeMillis());

    @Test
    @SuppressWarnings ("deprecation")
    public void testSetupPostHookShouldDeleteOrphan() throws IOException
    {
        when(gitHubRESTClient.getHooks(repository)).thenReturn(sampleHooks());
        when(applicationProperties.getBaseUrl()).thenReturn("http://jira.example.com");
        
        String hookUrl = "http://jira.example.com" + DvcsCommunicator.POST_HOOK_SUFFIX + "5/sync";
        communicator.ensureHookPresent(repository, hookUrl);

        verify(gitHubRESTClient, times(2)).addHook(isA(Repository.class), hookCaptor.capture());
        verify(gitHubRESTClient, times(2)).deleteHook(eq(repository), hookCaptor.capture());

        assertEquals(hookCaptor.getAllValues().get(0).getConfig().get(GitHubRepositoryHook.CONFIG_URL), hookUrl);
        assertEquals(hookCaptor.getAllValues().get(0).getName(), GitHubRepositoryHook.NAME_WEB);
        assertNull(hookCaptor.getAllValues().get(0).getConfig().get(GitHubRepositoryHook.CONFIG_CONTENT_TYPE));

        assertEquals(hookCaptor.getAllValues().get(1).getConfig().get(GitHubRepositoryHook.CONFIG_URL), hookUrl);
        assertEquals(hookCaptor.getAllValues().get(1).getName(), GitHubRepositoryHook.NAME_WEB);
        assertEquals(hookCaptor.getAllValues().get(1).getConfig().get(GitHubRepositoryHook.CONFIG_CONTENT_TYPE), GitHubRepositoryHook.CONFIG_CONTENT_TYPE_JSON);

        assertEquals(hookCaptor.getAllValues().get(2).getId(), Long.valueOf(111));

        assertEquals(hookCaptor.getAllValues().get(3).getId(), Long.valueOf(101));
    }

    @Test
    @SuppressWarnings ("deprecation")
    public void testSetupPostHookAlreadySetUpShouldDeleteOrphan() throws IOException
    {

        List<GitHubRepositoryHook> hooks = sampleHooks();
        hooks.add(sampleHook("http://jira.example.com/rest/bitbucket/1.0/repository/5/sync", 1L));
        hooks.add(samplePullRequestHook("http://jira.example.com/rest/bitbucket/1.0/repository/5/sync", 1L));
        when(gitHubRESTClient.getHooks(repository)).thenReturn(hooks);
        
        when(applicationProperties.getBaseUrl()).thenReturn("http://jira.example.com");
        
        String hookUrl = "http://jira.example.com" + DvcsCommunicator.POST_HOOK_SUFFIX + "5/sync";
        communicator.ensureHookPresent(repository, hookUrl);

        verify(gitHubRESTClient, never()).addHook(isA(Repository.class), isA(GitHubRepositoryHook.class));
        verify(gitHubRESTClient, times(2)).deleteHook(eq(repository), hookCaptor.capture());

    }

    private List<GitHubRepositoryHook> sampleHooks()
    {
        List<GitHubRepositoryHook> hooks = Lists.newArrayList();
        hooks.add(sampleHook("http://jira.example.com/rest/bitbucket/1.0/repository/55/sync", 111L));
        hooks.add(sampleHook("http://jira.example.com/rest/bitbucket/1.0/repository/45/sync", 101L));
        hooks.add(sampleNonWebHook(222L));
        return hooks;
    }

    protected GitHubRepositoryHook sampleHook(String url, long id)
    {
        GitHubRepositoryHook hook = new GitHubRepositoryHook();
        hook.setId(id);
        hook.setName(GitHubRepositoryHook.NAME_WEB);
        hook.setConfig(ImmutableMap.of(GitHubRepositoryHook.CONFIG_URL, url));
        return hook;
    }

    protected GitHubRepositoryHook samplePullRequestHook(String url, long id)
    {
        GitHubRepositoryHook hook = new GitHubRepositoryHook();
        hook.setId(id);
        hook.setName(GitHubRepositoryHook.NAME_WEB);
        hook.setConfig(ImmutableMap.of(
                GitHubRepositoryHook.CONFIG_URL, url,
                GitHubRepositoryHook.CONFIG_CONTENT_TYPE, GitHubRepositoryHook.CONFIG_CONTENT_TYPE_JSON
        ));
        hook.setEvents(ImmutableList.of(GitHubRepositoryHook.EVENT_PUSH,
                GitHubRepositoryHook.EVENT_PULL_REQUEST,
                GitHubRepositoryHook.EVENT_PULL_REQUEST_REVIEW_COMMENT,
                GitHubRepositoryHook.EVENT_ISSUE_COMMENT));
        return hook;
    }

    protected GitHubRepositoryHook sampleNonWebHook(long id)
    {
        GitHubRepositoryHook hook = new GitHubRepositoryHook();
        hook.setId(id);
        hook.setName("zendesk");
        hook.setConfig(ImmutableMap.of(
                "subdomain", "domain",
                "username", "username",
                "password", "password"
        ));
        hook.setEvents(ImmutableList.of(
                "commit_comment",
                "issues",
                "issue_comment",
                "pull_request",
                "push"));
        return hook;
    }


    @BeforeMethod
    public void initializeMocksAndGithubCommunicator()
    {
        MockitoAnnotations.initMocks(this);

        communicator = new GithubCommunicator(mock(OAuthStore.class), githubClientProvider);
        communicator.setGitHubRESTClient(gitHubRESTClient);
        ReflectionTestUtils.setField(communicator, "applicationProperties", applicationProperties);
        ReflectionTestUtils.setField(communicator, "syncDisabledHelper", syncDisabledHelper);
        ReflectionTestUtils.setField(communicator, "messagingService", messagingService);
        ReflectionTestUtils.setField(communicator, "gitHubEventService", gitHubEventService);
        ReflectionTestUtils.setField(communicator, "userServiceFactory", userServiceFactory);

        when(githubClientProvider.getRepositoryService(repository)).thenReturn(repositoryService);
        when(githubClientProvider.getUserService(repository)).thenReturn(userService);
        when(githubClientProvider.getCommitService(repository)).thenReturn(commitService);

        when(commitService.getClient()).thenReturn(gitHubClient);
        when(repositoryService.getClient()).thenReturn(gitHubClient);
        when(userService.getClient()).thenReturn(gitHubClient);
        
        when(githubClientProvider.createClient(HOST_URL)).thenReturn(gitHubClient);
        when(userServiceFactory.createUserService(gitHubClient)).thenReturn(userService);
        when(userService.getClient()).thenReturn(gitHubClient);

        doAnswer(new Answer<RateLimit>()
        {
            @Override
            public RateLimit answer(final InvocationOnMock invocation) throws Throwable
            {
                return rateLimit;
            }
        }).when(gitHubClient).getRateLimit();

        when(repository.getSlug()).thenReturn("SLUG");
        when(repository.getOrgName()).thenReturn("ORG");
    }

    @Test
    public void settingUpPostcommitHook_ShouldSendPOSTRequestToGithub() throws IOException
    {
        when(gitHubRESTClient.getHooks(any(Repository.class))).thenReturn(new LinkedList<GitHubRepositoryHook>());
        when(repository.getOrgName()).thenReturn("ORG");
        when(repository.getSlug())   .thenReturn("SLUG");

        String hookUrl = "POST-COMMIT-URL";
        communicator.ensureHookPresent(repository, hookUrl);

        // two times - one for changesets hook and one for pull requests hook
        ArgumentCaptor<GitHubRepositoryHook> hooks = ArgumentCaptor.forClass(GitHubRepositoryHook.class);
        verify(gitHubRESTClient, times(2)).addHook(any(Repository.class), hooks.capture());

        GitHubRepositoryHook hook;
        hook = hooks.getAllValues().get(0);
        Assert.assertEquals(hook.getConfig().get(GitHubRepositoryHook.CONFIG_URL), hookUrl);
        Assert.assertNotEquals(hook.getConfig().get(GitHubRepositoryHook.CONFIG_CONTENT_TYPE), GitHubRepositoryHook.CONFIG_CONTENT_TYPE_JSON);
        Assert.assertTrue(hook.getEvents().contains(GitHubRepositoryHook.EVENT_PUSH));
        Assert.assertFalse(hook.getEvents().contains(GitHubRepositoryHook.EVENT_PULL_REQUEST));
        Assert.assertFalse(hook.getEvents().contains(GitHubRepositoryHook.EVENT_PULL_REQUEST_REVIEW_COMMENT));
        Assert.assertFalse(hook.getEvents().contains(GitHubRepositoryHook.EVENT_ISSUE_COMMENT));

        hook = hooks.getAllValues().get(1);
        Assert.assertEquals(hook.getConfig().get(GitHubRepositoryHook.CONFIG_CONTENT_TYPE), GitHubRepositoryHook.CONFIG_CONTENT_TYPE_JSON);
        Assert.assertEquals(hook.getConfig().get(GitHubRepositoryHook.CONFIG_URL), hookUrl);
        Assert.assertTrue(hook.getEvents().contains(GitHubRepositoryHook.EVENT_PUSH));
        Assert.assertTrue(hook.getEvents().contains(GitHubRepositoryHook.EVENT_PULL_REQUEST));
        Assert.assertTrue(hook.getEvents().contains(GitHubRepositoryHook.EVENT_PULL_REQUEST_REVIEW_COMMENT));
        Assert.assertTrue(hook.getEvents().contains(GitHubRepositoryHook.EVENT_ISSUE_COMMENT));
    }

    @Test
    public void settingUpPostcommitHook_alreadyExisting() throws Exception
    {
        String postCommitUrl = "postCommitUrl";

        GitHubRepositoryHook changesetsHook = mock(GitHubRepositoryHook.class);
        when(changesetsHook.getConfig()).thenReturn(MapBuilder.build("url", postCommitUrl));

        GitHubRepositoryHook prHook = mock(GitHubRepositoryHook.class);
        when(prHook.getConfig()).thenReturn(MapBuilder.build("url", postCommitUrl, "content_type", "json"));

        List<GitHubRepositoryHook> hooks = Arrays.asList(changesetsHook, prHook);

        when(gitHubRESTClient.getHooks(any(Repository.class))).thenReturn(hooks);

        communicator.ensureHookPresent(repository, postCommitUrl);

        verify(repositoryService, never()).createHook(any(IRepositoryIdProvider.class), any(RepositoryHook.class));
    }

    @Test
    public void gettingUser_ShouldSendGETRequestToGithub_AndParseJsonResult() throws Exception
    {
        when(userService.getUser("USER-NAME")).thenReturn(githubUser);
        when(githubUser.getLogin()).thenReturn("Test GitHub user login");
        when(githubUser.getName()).thenReturn("Test GitHub user name");
        when(githubUser.getAvatarUrl()).thenReturn("https://secure.gravatar.com/avatar/gravatarId?s=60");

        DvcsUser githubUser = communicator.getUser(repository, "USER-NAME");

        assertThat(githubUser.getAvatar()).isEqualTo("https://secure.gravatar.com/avatar/gravatarId?s=60");
        assertThat(githubUser.getUsername()).isEqualTo("Test GitHub user login");
        assertThat(githubUser.getFullName()).isEqualTo("Test GitHub user name");
    }

    @Test
    public void gettingDetailChangeset_ShouldSendGETRequestToGithub_AndParseJsonResult()
            throws ResponseException, IOException
    {
        when(repository.getSlug())   .thenReturn("SLUG");
        when(repository.getOrgName()).thenReturn("ORG");

        RepositoryCommit repositoryCommit = mock(RepositoryCommit.class);
        when(commitService.getCommit(Matchers.<IRepositoryIdProvider>anyObject(), anyString())).thenReturn(repositoryCommit);
        Commit commit = mock(Commit.class);
        when(repositoryCommit.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("ABC-123 fix");

        Changeset detailChangeset = communicator.getChangeset(repository, "abcde");

        verify(commitService).getCommit(Matchers.<IRepositoryIdProvider>anyObject(), anyString());

        assertThat(detailChangeset.getMessage()).isEqualTo("ABC-123 fix");
    }

    @Test
    public void getChangesetShouldHitGithubRateLimit() throws ResponseException, IOException
    {
        when(commitService.getCommit(any(IRepositoryIdProvider.class), anyString())).thenThrow(new RequestException(requestError, 403));
        try
        {
            communicator.getChangeset(repository, "abcde");
            fail("GithubRateLimitExceededException expected");
        }
        catch (GithubRateLimitExceededException e)
        {
            assertThat(e.getRateLimit()).isSameAs(rateLimit);
            verify(commitService).getCommit(Matchers.<IRepositoryIdProvider>anyObject(), anyString());
        }
    }

    @Test
    public void getBranchesShouldHitGithubRateLimit() throws IOException
    {
        try
        {
            when(repositoryService.getBranches(any(IRepositoryIdProvider.class))).thenThrow(new RequestException(requestError, 403));
            communicator.getBranches(repository);
            fail("GithubRateLimitExceededException expected");
        }
        catch (GithubRateLimitExceededException e)
        {
            assertThat(e.getRateLimit()).isSameAs(rateLimit);
        }
    }

    @Test
    public void getFileDetailsShouldFetchCommitsFromGitHub() throws Exception
    {
        // Repository
        when(repository.getSlug())   .thenReturn("SLUG");
        when(repository.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

        final Changeset cs = mock(Changeset.class);
        when(cs.getNode()).thenReturn("some-hash");

        final CommitFile file = new CommitFile()
                .setStatus("modified")
                .setFilename("my_file")
                .setAdditions(1)
                .setDeletions(2);

        final RepositoryCommit ghCommit = mock(RepositoryCommit.class);
        when(ghCommit.getFiles()).thenReturn(ImmutableList.of(file));

        when(commitService.getCommit(repositoryId, cs.getNode())).thenReturn(ghCommit);

        ChangesetFileDetailsEnvelope changesetFileDetailsEnvelope = communicator.getFileDetails(repository, cs);

        List<ChangesetFileDetail> fileDetails = changesetFileDetailsEnvelope.getFileDetails();
        assertEquals(fileDetails, ImmutableList.of(
                new ChangesetFileDetail(CustomStringUtils.getChangesetFileAction(file.getStatus()), file.getFilename(), file.getAdditions(), file.getDeletions())
        ));
        assertEquals(changesetFileDetailsEnvelope.getCount(), 1);
    }

    @Test
    public void testPRFullSyncWithDarkFeature()
    {
        when(syncDisabledHelper.isGitHubUsePullRequestListDisabled()).thenReturn(true);

        communicator.startSynchronisation(repository, EnumSet.of(SynchronizationFlag.SYNC_PULL_REQUESTS), 0);

        verify(gitHubEventService).synchronize(eq(repository), eq(false), any(String[].class), eq(false));
        verify(messagingService, never()).publish(any(MessageAddress.class), any(GitHubPullRequestPageMessage.class), Matchers.<String[]>anyVararg());
    }

    @Test
    public void testPRFullSyncWithoutDarkFeature()
    {
        when(syncDisabledHelper.isGitHubUsePullRequestListDisabled()).thenReturn(false);

        communicator.startSynchronisation(repository, EnumSet.of(SynchronizationFlag.SYNC_PULL_REQUESTS), 0);

        verify(gitHubEventService, never()).synchronize(eq(repository), eq(false), any(String[].class), eq(false));
        verify(messagingService).publish(any(MessageAddress.class), any(GitHubPullRequestPageMessage.class), Matchers.<String[]>anyVararg());
    }

    @Test
    public void TestIsUsernameCorrect() throws Exception{
        when(userService.getUser(ACCOUNT_Name)).thenReturn(githubUser);

        assertTrue(communicator.isUsernameCorrect(HOST_URL, ACCOUNT_Name));
    }

    @Test
    public void TestIsUsernameIncorrect() throws Exception{
        when(userService.getUser(ACCOUNT_Name)).thenReturn(null);
        when(gitHubClient.getRemainingRequests()).thenReturn(1);

        assertFalse(communicator.isUsernameCorrect(HOST_URL, ACCOUNT_Name));
    }

    @Test
    public void TestIsUsernameIncorrectAndBlownRateLimit() throws Exception{
        when(userService.getUser(ACCOUNT_Name)).thenReturn(null);
        when(gitHubClient.getRemainingRequests()).thenReturn(0);

        assertTrue(communicator.isUsernameCorrect(HOST_URL, ACCOUNT_Name));
    }
}
