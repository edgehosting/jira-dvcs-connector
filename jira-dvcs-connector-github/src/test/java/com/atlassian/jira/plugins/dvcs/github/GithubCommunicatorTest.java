package com.atlassian.jira.plugins.dvcs.github;

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
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.ImmutableList;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Martin Skurla
 * @author Miroslav Stencel mstencel@atlassian.com
 */
public class GithubCommunicatorTest
{
    @Mock
    private Repository repositoryMock;
    @Mock
    private GithubClientProvider githubClientProvider;
    @Mock
    private CommitService commitService;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private UserService userService;
    @Mock
    private User githubUser;

    // tested object
    private DvcsCommunicator communicator;

    private ChangesetCacheImpl changesetCache;

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

        communicator = new GithubCommunicator(changesetCache = new ChangesetCacheImpl(), mock(OAuthStore.class), githubClientProvider);
        when(githubClientProvider.getRepositoryService(repositoryMock)).thenReturn(repositoryService);
        when(githubClientProvider.getUserService(repositoryMock)).thenReturn(userService);
        when(githubClientProvider.getCommitService(repositoryMock)).thenReturn(commitService);
	}

	@Test
	public void settingUpPostcommitHook_ShouldSendPOSTRequestToGithub() throws IOException
    {
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        when(repositoryMock.getSlug())   .thenReturn("SLUG");

        communicator.setupPostcommitHook(repositoryMock, "POST-COMMIT-URL");

        verify(repositoryService).createHook(Matchers.<IRepositoryIdProvider>anyObject(),Matchers.<RepositoryHook>anyObject());
    }

    @Test
    public void gettingUser_ShouldSendGETRequestToGithub_AndParseJsonResult() throws Exception
    {
        when(userService.getUser("USER-NAME")).thenReturn(githubUser);
        when(githubUser.getLogin()).thenReturn("Test GitHub user login");
        when(githubUser.getName()).thenReturn("Test GitHub user name");
        when(githubUser.getAvatarUrl()).thenReturn("https://secure.gravatar.com/avatar/gravatarId?s=60");

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
        when(commitService.getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString())).thenReturn(repositoryCommit);
        Commit commit = mock(Commit.class);
        when(repositoryCommit.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("ABC-123 fix");

        Changeset detailChangeset = communicator.getChangeset(repositoryMock, "abcde");

        verify(commitService).getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString());

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

        when(commitService.getCommit(repositoryId, cs.getNode())).thenReturn(ghCommit);

        List<ChangesetFileDetail> fileDetails = communicator.getFileDetails(repositoryMock, cs);
        assertEquals(fileDetails, ImmutableList.of(
                new ChangesetFileDetail(CustomStringUtils.getChangesetFileAction(file.getStatus()), file.getFilename(), file.getAdditions(), file.getDeletions())
        ));
    }
}

