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
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
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
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Martin Skurla
 * @author Miroslav Stencel mstencel@atlassian.com
 */
public class GithubCommunicatorTest
{
    @Mock
    private Repository repositoryMock;
    @Mock
    private GithubClientProvider githubClientProviderMock;
    @Mock
    private CommitService commitServiceMock;
    @Mock
    private RepositoryService repositoryServiceMock;
    @Mock
    private UserService userServiceMock;
    @Mock
    private GitHubRESTClient gitHubRESTClient;

    @Mock
    private User githubUserMock;
    @Mock
    private ApplicationProperties applicationPropertiesMock;
    @Captor
    private ArgumentCaptor<GitHubRepositoryHook> hookCaptor;
    @Mock
    private SyncDisabledHelper syncDisabledHelper;
    @Mock
    private MessagingService messagingService;
    @Mock
    private GitHubEventService gitHubEventService;

    // tested object
    private GithubCommunicator communicator;

    @Test
    public void testSetupPostHookShouldDeleteOrphan() throws IOException
    {
        when(repositoryMock.getOrgName()).thenReturn("owner");
        when(repositoryMock.getSlug()).thenReturn("slug");
        
        when(gitHubRESTClient.getHooks(repositoryMock)).thenReturn(sampleHooks());
        
        when(applicationPropertiesMock.getBaseUrl()).thenReturn("http://jira.example.com");
        
        String hookUrl = "http://jira.example.com" + DvcsCommunicator.POST_HOOK_SUFFIX + "5/sync";
        communicator.ensureHookPresent(repositoryMock, hookUrl);
        
        verify(gitHubRESTClient, times(2)).addHook(isA(Repository.class), hookCaptor.capture());
        verify(gitHubRESTClient, times(2)).deleteHook(eq(repositoryMock), hookCaptor.capture());

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
    public void testSetupPostHookAlreadySetUpShouldDeleteOrphan() throws IOException
    {
        when(repositoryMock.getOrgName()).thenReturn("owner");
        when(repositoryMock.getSlug()).thenReturn("slug");
        
        List<GitHubRepositoryHook> hooks = sampleHooks();
        hooks.add(sampleHook("http://jira.example.com/rest/bitbucket/1.0/repository/5/sync", 1L));
        hooks.add(samplePullRequestHook("http://jira.example.com/rest/bitbucket/1.0/repository/5/sync", 1L));
        when(gitHubRESTClient.getHooks(repositoryMock)).thenReturn(hooks);
        
        when(applicationPropertiesMock.getBaseUrl()).thenReturn("http://jira.example.com");
        
        String hookUrl = "http://jira.example.com" + DvcsCommunicator.POST_HOOK_SUFFIX + "5/sync";
        communicator.ensureHookPresent(repositoryMock, hookUrl);
        
        verify(gitHubRESTClient, never()).addHook(isA(Repository.class), isA(GitHubRepositoryHook.class));
        verify(gitHubRESTClient, times(2)).deleteHook(eq(repositoryMock), hookCaptor.capture());

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

        communicator = new GithubCommunicator(mock(OAuthStore.class), githubClientProviderMock);
        communicator.setGitHubRESTClient(gitHubRESTClient);
        ReflectionTestUtils.setField(communicator, "applicationProperties", applicationPropertiesMock);
        ReflectionTestUtils.setField(communicator, "syncDisabledHelper", syncDisabledHelper);
        ReflectionTestUtils.setField(communicator, "messagingService", messagingService);
        ReflectionTestUtils.setField(communicator, "gitHubEventService", gitHubEventService);

        when(githubClientProviderMock.getRepositoryService(repositoryMock)).thenReturn(repositoryServiceMock);
        when(githubClientProviderMock.getUserService(repositoryMock)).thenReturn(userServiceMock);
        when(githubClientProviderMock.getCommitService(repositoryMock)).thenReturn(commitServiceMock);
	}

	@Test
	public void settingUpPostcommitHook_ShouldSendPOSTRequestToGithub() throws IOException
    {
        when(gitHubRESTClient.getHooks(any(Repository.class))).thenReturn(new LinkedList<GitHubRepositoryHook>());
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        when(repositoryMock.getSlug())   .thenReturn("SLUG");

        String hookUrl = "POST-COMMIT-URL";
        communicator.ensureHookPresent(repositoryMock, hookUrl);

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

        when(repositoryMock.getOrgName()).thenReturn("ORG");
        when(repositoryMock.getSlug()).thenReturn("SLUG");

        GitHubRepositoryHook changesetsHook = mock(GitHubRepositoryHook.class);
        when(changesetsHook.getConfig()).thenReturn(MapBuilder.build("url", postCommitUrl));

        GitHubRepositoryHook prHook = mock(GitHubRepositoryHook.class);
        when(prHook.getConfig()).thenReturn(MapBuilder.build("url", postCommitUrl, "content_type", "json"));

        List<GitHubRepositoryHook> hooks = Arrays.asList(new GitHubRepositoryHook[] { changesetsHook, prHook });

        when(gitHubRESTClient.getHooks(any(Repository.class))).thenReturn(hooks);

        communicator.ensureHookPresent(repositoryMock, postCommitUrl);

        verify(repositoryServiceMock, never()).createHook(any(IRepositoryIdProvider.class), any(RepositoryHook.class));
    }

    @Test
    public void gettingUser_ShouldSendGETRequestToGithub_AndParseJsonResult() throws Exception
    {
        when(userServiceMock.getUser("USER-NAME")).thenReturn(githubUserMock);
        when(githubUserMock.getLogin()).thenReturn("Test GitHub user login");
        when(githubUserMock.getName()).thenReturn("Test GitHub user name");
        when(githubUserMock.getAvatarUrl()).thenReturn("https://secure.gravatar.com/avatar/gravatarId?s=60");

        DvcsUser githubUser = communicator.getUser(repositoryMock, "USER-NAME");

        assertThat(githubUser.getAvatar())  .isEqualTo("https://secure.gravatar.com/avatar/gravatarId?s=60");
        assertThat(githubUser.getUsername()).isEqualTo("Test GitHub user login");
        assertThat(githubUser.getFullName()).isEqualTo("Test GitHub user name");
    }

    @Test
    public void gettingDetailChangeset_ShouldSendGETRequestToGithub_AndParseJsonResult() throws ResponseException, IOException
    {
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryCommit repositoryCommit = mock(RepositoryCommit.class);
        when(commitServiceMock.getCommit(Matchers.<IRepositoryIdProvider>anyObject(), anyString())).thenReturn(repositoryCommit);
        Commit commit = mock(Commit.class);
        when(repositoryCommit.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("ABC-123 fix");

        Changeset detailChangeset = communicator.getChangeset(repositoryMock, "abcde");

        verify(commitServiceMock).getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString());

        assertThat(detailChangeset.getMessage()).isEqualTo("ABC-123 fix");
    }

    @Test
    public void getFileDetailsShouldFetchCommitsFromGitHub() throws Exception
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());

        final Changeset cs = mock(Changeset.class);
        when(cs.getNode()).thenReturn("some-hash");

        final CommitFile file = new CommitFile()
                .setStatus("modified")
                .setFilename("my_file")
                .setAdditions(1)
                .setDeletions(2);

        final RepositoryCommit ghCommit = mock(RepositoryCommit.class);
        when(ghCommit.getFiles()).thenReturn(ImmutableList.of(file));

        when(commitServiceMock.getCommit(repositoryId, cs.getNode())).thenReturn(ghCommit);

        ChangesetFileDetailsEnvelope changesetFileDetailsEnvelope = communicator.getFileDetails(repositoryMock, cs);

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

        communicator.startSynchronisation(repositoryMock, EnumSet.of(SynchronizationFlag.SYNC_PULL_REQUESTS), 0);

        verify(gitHubEventService).synchronize(eq(repositoryMock), eq(false), any(String[].class));
        verify(messagingService, never()).publish(any(MessageAddress.class), any(GitHubPullRequestPageMessage.class), Matchers.<String[]>anyVararg());
    }

    @Test
    public void testPRFullSyncWithoutDarkFeature()
    {
        when(syncDisabledHelper.isGitHubUsePullRequestListDisabled()).thenReturn(false);

        communicator.startSynchronisation(repositoryMock, EnumSet.of(SynchronizationFlag.SYNC_PULL_REQUESTS), 0);

        verify(gitHubEventService, never()).synchronize(eq(repositoryMock), eq(false), any(String[].class));
        verify(messagingService).publish(any(MessageAddress.class), any(GitHubPullRequestPageMessage.class), Matchers.<String[]>anyVararg());
    }
}

