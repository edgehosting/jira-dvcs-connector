package com.atlassian.jira.plugins.dvcs.bitbucket;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.net.DefaultRequestHelper;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.dvcs.net.ExtendedResponseHandlerFactory;
import com.atlassian.jira.plugins.dvcs.net.RequestHelper;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.RequestFactory;

public class BitbucketCommunicatorTest
{

	@Mock
	private BitbucketLinker bitbucketLinker;

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
	private PluginAccessor pluginAccessor;

	// tested object
	private DvcsCommunicator communicator;

	
	public BitbucketCommunicatorTest()
	{
		super();
	}

	@Before
	public void setup() {
		
		MockitoAnnotations.initMocks(this);
		
        RequestHelper requestHelper = new DefaultRequestHelper(
                requestFactory, extendedResponseHandlerFactory);
        communicator = new BitbucketCommunicator(authenticationFactory, requestHelper,
                bitbucketLinker, pluginAccessor)
        {
            @Override
            protected String getPluginVersion(PluginAccessor pluginAccessor)
            {
                return "123";
            }
        };
		
		when(extendedResponseHandlerFactory.create()).thenReturn(responseHandler);
	}
	
	@Test
	public void testGetRepositories() {
		
		when(requestFactory.createRequest(any(Request.MethodType.class), anyString())).thenReturn(request);
		when(responseHandler.getExtendedResponse()).thenReturn(
				new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-get-repositories.json")));
		
		List<Repository> repositories = communicator.getRepositories(createSampleOrganization());
		
		Assert.assertEquals(repositories.size(), 3);
		Assert.assertEquals(repositories.get(0).getSlug(), "public-hg-repo");
		Assert.assertEquals(repositories.get(0).getName(), "public-hg-repo");
	
	}
	
	@Test
	public void testSetupPostcommitHook () {
		
		when(requestFactory.createRequest(any(Request.MethodType.class), anyString())).thenReturn(request);
		when(repository.getOrgName()).thenReturn("org");
		when(repository.getSlug()).thenReturn("slug");
		when(repository.getOrgHostUrl()).thenReturn("https://bitbucket.org");
	
		communicator.setupPostcommitHook(repository, "post-commit-url");
		
		verify(bitbucketLinker).linkRepository(repository);
		verify(requestFactory).createRequest(Mockito.eq(MethodType.POST), eq("https://bitbucket.org/!api/1.0/repositories/org/slug/services"));
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

