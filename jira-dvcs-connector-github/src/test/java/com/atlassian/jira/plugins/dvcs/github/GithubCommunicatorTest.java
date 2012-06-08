package com.atlassian.jira.plugins.dvcs.github;

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.DefaultRequestHelper;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandlerFactory;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;

public class GithubCommunicatorTest
{

	@Mock
	private AuthenticationFactory authenticationFactory;
	
	@Mock
	private ExtendedResponseHandlerFactory extendedResponseHandlerFactory;

	@Mock
	private ExtendedResponseHandler responseHandler;
	
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
	private GithubOAuth githubOAuth;
	
	// tested object
	private DvcsCommunicator communicator;

	
	
	public GithubCommunicatorTest()
	{
		super();
	}

	@Before
	public void setup() {
		
		MockitoAnnotations.initMocks(this);
		
		communicator = new GithubCommunicator(changesetCache, new DefaultRequestHelper(requestFactory, extendedResponseHandlerFactory), authenticationFactory, githubOAuth);
		
		when(extendedResponseHandlerFactory.create()).thenReturn(responseHandler);
	}
	
	@Test
	public void testGetRepositories() {
		
	
	}
	
	@Test
	public void testSetupPostcommitHook () {
		
	}
	
	private Organization createSampleOrganization()
	{
		Organization organization = new Organization();
		organization.setDvcsType("bitbucket");
		organization.setHostUrl("https://bitbucket.org");
		organization.setName("doesnotmatter");
		organization.setCredential(new Credential("doesnotmatter_u", "doesnotmatter_p", null));
		return organization;
	}
	
	private String resource(String name)
	{
		try
		{
			
			return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(name));
			
		} catch (IOException e)
		{
			throw new RuntimeException("Can not load resource " + name, e);
		}
	}
}

