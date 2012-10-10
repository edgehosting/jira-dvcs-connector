package com.atlassian.jira.plugins.dvcs.github;


import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryHook;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.sal.api.net.ResponseException;


/**
 * @author Martin Skurla
 */
@RunWith(MockitoJUnitRunner.class)
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

	@Before
	public void initializeGithubCommunicator()
    {
        communicator = new GithubCommunicator(mock(ChangesetCache.class), mock(GithubOAuth.class), githubClientProvider);
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
        when(githubUser.getGravatarId()).thenReturn("gravatarId");
        
        DvcsUser githubUser = communicator.getUser(repositoryMock, "USER-NAME");
        
        assertThat(githubUser.getAvatar(), is("https://secure.gravatar.com/avatar/gravatarId?s=60"));
        assertThat(githubUser.getUsername(), is("Test GitHub user login"));
        assertThat(githubUser.getFullName(), is("Test GitHub user name"));
    }

    @Test
    public void gettingDetailChangeset_ShouldSendGETRequestToGithub_AndParseJsonResult() throws ResponseException, IOException
    {
        Changeset changesetMock = mock(Changeset.class);
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        RepositoryCommit repositoryCommit = mock(RepositoryCommit.class);
        when(commitService.getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString())).thenReturn(repositoryCommit);
        Commit commit = mock(Commit.class);
        when(repositoryCommit.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("ABC-123 fix");

        Changeset detailChangeset = communicator.getDetailChangeset(repositoryMock, changesetMock);
        
        verify(commitService).getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString());

        assertThat(detailChangeset.getMessage(), is("ABC-123 fix"));
    }
}

