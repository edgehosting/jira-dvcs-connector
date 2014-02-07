package com.atlassian.jira.plugins.dvcs.github;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
    private User githubUserMock;
    @Mock
    private ApplicationProperties applicationPropertiesMock;
    @Captor
    private ArgumentCaptor<RepositoryHook> hookCaptor;

    // tested object
    private DvcsCommunicator communicator;

    
    @Test
    public void testSetupPostHookShouldDeleteOrphan () throws IOException
    {
        when(repositoryMock.getOrgName()).thenReturn("owner");
        when(repositoryMock.getSlug()).thenReturn("slug");
        
        RepositoryId repoId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        when(repositoryServiceMock.getHooks(repoId)).thenReturn(sampleHooks());
        
        when(applicationPropertiesMock.getBaseUrl()).thenReturn("http://jira.example.com");
        
        String hookUrl = "http://jira.example.com" + DvcsCommunicator.POST_HOOK_SUFFIX + "5/sync";
        communicator.setupPostcommitHook(repositoryMock, hookUrl);
        
        verify(repositoryServiceMock, times(1)).createHook(isA(IRepositoryIdProvider.class), hookCaptor.capture());
        verify(repositoryServiceMock, times(1)).deleteHook(repoId, 111);
        verify(repositoryServiceMock, times(1)).deleteHook(repoId, 101);
        
        assertEquals(hookCaptor.getValue().getConfig().get("url"), hookUrl);
        assertEquals(hookCaptor.getValue().getName(), "web");
    }
    
    @Test
    public void testSetupPostHookAlreadySetUpShouldDeleteOrphan () throws IOException
    {
        when(repositoryMock.getOrgName()).thenReturn("owner");
        when(repositoryMock.getSlug()).thenReturn("slug");
        
        RepositoryId repoId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        List<RepositoryHook> hooks = sampleHooks();
        hooks.add(sampleHook("http://jira.example.com/rest/bitbucket/1.0/repository/5/sync", 1L));
        when(repositoryServiceMock.getHooks(repoId)).thenReturn(hooks);
        
        when(applicationPropertiesMock.getBaseUrl()).thenReturn("http://jira.example.com");
        
        String hookUrl = "http://jira.example.com" + DvcsCommunicator.POST_HOOK_SUFFIX + "5/sync";
        communicator.setupPostcommitHook(repositoryMock, hookUrl);
        
        verify(repositoryServiceMock, never()).createHook(isA(IRepositoryIdProvider.class), isA(RepositoryHook.class));
        verify(repositoryServiceMock, times(1)).deleteHook(repoId, 111);
        verify(repositoryServiceMock, times(1)).deleteHook(repoId, 101);
        
    }

    private List<RepositoryHook> sampleHooks()
    {
        List<RepositoryHook> hooks = Lists.newArrayList();
        hooks.add(sampleHook("http://jira.example.com/rest/bitbucket/1.0/repository/55/sync", 111L));
        hooks.add(sampleHook("http://jira.example.com/rest/bitbucket/1.0/repository/45/sync", 101L));
        return hooks;
    }

    protected RepositoryHook sampleHook(String url, long id)
    {
        RepositoryHook hook = new RepositoryHook();
        hook.setId(id);
        hook.setName("web");
        hook.setConfig(ImmutableMap.of("url", url));
        return hook;
    }


    private class ChangesetCacheImpl implements ChangesetCache
    {

        private final List<String> cache = new ArrayList<String>();

        @Override
        public boolean isCached(int repositoryId, String changesetNode)
        {
            return cache.contains(changesetNode);
        }

        public void add(String node)
        {
            cache.add(node);
        }

        @Override
        public boolean isEmpty(int repositoryId)
        {
            return cache.isEmpty();
        }
    }

	@BeforeMethod
	public void initializeMocksAndGithubCommunicator()
    {
        MockitoAnnotations.initMocks(this);

        communicator = new GithubCommunicator(new ChangesetCacheImpl(), mock(OAuthStore.class), githubClientProviderMock);
        ReflectionTestUtils.setField(communicator, "applicationProperties", applicationPropertiesMock);
        
        when(githubClientProviderMock.getRepositoryService(repositoryMock)).thenReturn(repositoryServiceMock);
        when(githubClientProviderMock.getUserService(repositoryMock)).thenReturn(userServiceMock);
        when(githubClientProviderMock.getCommitService(repositoryMock)).thenReturn(commitServiceMock);
	}

	@Test
	public void settingUpPostcommitHook_ShouldSendPOSTRequestToGithub() throws IOException
    {
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        when(repositoryMock.getSlug())   .thenReturn("SLUG");

        communicator.setupPostcommitHook(repositoryMock, "POST-COMMIT-URL");

        verify(repositoryServiceMock).createHook(Matchers.<IRepositoryIdProvider>anyObject(),Matchers.<RepositoryHook>anyObject());
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
        when(commitServiceMock.getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString())).thenReturn(repositoryCommit);
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

        List<ChangesetFileDetail> fileDetails = communicator.getFileDetails(repositoryMock, cs);
        assertEquals(fileDetails, ImmutableList.of(
                new ChangesetFileDetail(CustomStringUtils.getChangesetFileAction(file.getStatus()), file.getFilename(), file.getAdditions(), file.getDeletions())
        ));
    }
}

