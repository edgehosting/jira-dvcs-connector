package com.atlassian.jira.plugins.dvcs.github;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.DefaultRequestHelper;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandlerFactory;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;

import static org.mockito.Mockito.*;


/**
 * @author Martin Skurla
 */
@RunWith(MockitoJUnitRunner.class)
public class GithubCommunicatorTest
{

	@Mock
	private AuthenticationFactory authenticationFactory;

	@Mock
	private ExtendedResponseHandlerFactory extendedResponseHandlerFactory;

	@Mock
	private Request<?, ?> request;

	@Mock
	private Repository repository;

	@SuppressWarnings("rawtypes")
	@Mock
	private RequestFactory requestFactory;

	@Mock
	private ChangesetCache changesetCache;

	@Mock
	private GithubOAuth githubOAuth;//TODO pomazat mocky ktore tam nemaju byt, ...

	// tested object
	private DvcsCommunicator communicator;


	@Before
	public void initializeGithubCommunicator() {
		communicator = new GithubCommunicator(changesetCache,
                                              new DefaultRequestHelper(requestFactory, extendedResponseHandlerFactory),
                                              authenticationFactory,
                                              githubOAuth);
	}

	@Test
	public void testSetupPostcommitHook() {

		when(requestFactory.createRequest(any(Request.MethodType.class), anyString())).thenReturn(request);
		when(repository.getOrgName()).thenReturn("org");
		when(repository.getSlug()).thenReturn("slug");
		when(repository.getOrgHostUrl()).thenReturn(""); // is not important

		communicator.setupPostcommitHook(repository, "post-commit-url");

		verify(requestFactory).createRequest(eq(Request.MethodType.POST),
                                             eq("https://api.github.com/repos/org/slug/hooks"));
        verify(request).setRequestBody(contains("post-commit-url"));
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
//
//	private static String resourceAsString(String name)
//	{
//		try
//		{
//			return IOUtils.toString(GithubCommunicatorTest.class.getClassLoader().getResourceAsStream(name));
//
//		} catch (IOException e)
//		{
//			throw new RuntimeException("Can not load resource " + name, e);
//		}
//	}
}

