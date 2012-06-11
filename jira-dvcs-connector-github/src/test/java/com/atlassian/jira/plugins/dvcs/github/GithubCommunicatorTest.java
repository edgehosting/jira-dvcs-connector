package com.atlassian.jira.plugins.dvcs.github;


import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.DefaultRequestHelper;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandlerFactory;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;


/**
 * @author Martin Skurla
 */
@RunWith(MockitoJUnitRunner.class)
public class GithubCommunicatorTest
{
    private static final String GITHUB_SHOW_USER_NAME_RESPONSE_RESOURCE    = "github-showUserName-response.json";
    private static final String GITHUB_GET_SINGLE_COMMIT_RESPONSE_RESOURCE = "github-getSingleCommit-response.json";


	@Mock
	private AuthenticationFactory authenticationFactoryMock;

	@Mock
	private ExtendedResponseHandlerFactory extendedResponseHandlerFactoryMock;

	@Mock
	private Request<?, ?> requestMock;

	@Mock
	private Repository repositoryMock;

	@SuppressWarnings("rawtypes")
	@Mock
	private RequestFactory requestFactoryMock;

	@Mock
	private ChangesetCache changesetCacheMock;

	@Mock
	private GithubOAuth githubOAuthMock;//TODO pomazat mocky ktore tam nemaju byt, ...

	// tested object
	private DvcsCommunicator communicator;


	@Before
	public void initializeGithubCommunicator()
    {
		communicator = new GithubCommunicator(changesetCacheMock,
                                              new DefaultRequestHelper(requestFactoryMock, extendedResponseHandlerFactoryMock),
                                              authenticationFactoryMock,
                                              githubOAuthMock);
	}

	@Test
	public void settingUpPostcommitHook_ShouldSendPOSTRequestToGithub()
    {
		when(requestFactoryMock.createRequest(any(Request.MethodType.class), anyString())).thenReturn(requestMock);

        when(repositoryMock.getOrgName()).thenReturn("ORG");
		when(repositoryMock.getSlug())   .thenReturn("SLUG");

		communicator.setupPostcommitHook(repositoryMock, "POST-COMMIT-URL");

		verify(requestFactoryMock).createRequest(eq(Request.MethodType.POST),
                                             eq("https://api.github.com/repos/ORG/SLUG/hooks"));
        verify(requestMock).setRequestBody(contains("POST-COMMIT-URL"));
	}

    @Test
    public void gettingUser_ShouldSendGETRequestToGithub_AndParseJsonResult() throws ResponseException
    {
		when(requestFactoryMock.createRequest(any(Request.MethodType.class), anyString())).thenReturn(requestMock);
		when(repositoryMock.getOrgHostUrl()).thenReturn("HOST-URL");

        when(requestMock.execute()).thenReturn(resourceAsString(GITHUB_SHOW_USER_NAME_RESPONSE_RESOURCE));

        DvcsUser githubUser = communicator.getUser(repositoryMock, "USER-NAME");

        verify(requestFactoryMock).createRequest(eq(Request.MethodType.GET),
                                             eq("HOST-URL/api/v2/json/user/show/USER-NAME"));

        assertThat(githubUser.getUsername(), is("Test GitHub user login"));
        assertThat(githubUser.getLastName(), is("Test GitHub user name"));
    }

    @Test
    public void gettingDetailChangeset_ShouldSendGETRequestToGithub_AndParseJsonResult() throws ResponseException {
        Changeset changesetMock = mock(Changeset.class);

		when(requestFactoryMock.createRequest(any(Request.MethodType.class), anyString())).thenReturn(requestMock);

        when(repositoryMock.getOrgName()).thenReturn("ORG");
        when(repositoryMock.getSlug())   .thenReturn("SLUG");

        when(changesetMock.getNode()).thenReturn("SHA");

        when(requestMock.execute()).thenReturn(resourceAsString(GITHUB_GET_SINGLE_COMMIT_RESPONSE_RESOURCE));

        Changeset changeset = communicator.getDetailChangeset(repositoryMock, changesetMock);

        verify(requestFactoryMock).createRequest(eq(Request.MethodType.GET),
                                             eq("https://api.github.com/repos/ORG/SLUG/commits/SHA"));

        assertThat(changeset.getMessage(), is("Test GitHub commit message"));
        assertThat(changeset.getAuthor(),  is("Test GitHub author login"));
    }

//	private Organization createSampleOrganization()
//	{
//		Organization organization = new Organization();
//
//        organization.setDvcsType("github");
//		organization.setHostUrl ("https://github.com");
//		organization.setName    ("doesnotmatter");
//
//        organization.setCredential(new Credential("doesnotmatter_u", "doesnotmatter_p", null));
//
//        return organization;
//	}

	private static String resourceAsString(String relativeResourcePath)
	{
		try
		{
			return IOUtils.toString(GithubCommunicatorTest.class.getClassLoader()
                                                                .getResourceAsStream(relativeResourcePath));

		} catch (IOException e)
		{
			throw new RuntimeException("Can not load resource " + relativeResourcePath, e);
		}
	}
}

