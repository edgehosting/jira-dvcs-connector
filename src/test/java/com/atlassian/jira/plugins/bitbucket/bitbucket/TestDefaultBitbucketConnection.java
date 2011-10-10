package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.api.Authentication;
import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketChangesetIterator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.DefaultBitbucketConnection;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;

/**
 * Unit tests for {@link DefaultBitbucketConnection}.
 */
public class TestDefaultBitbucketConnection
{
    @Mock
    RequestFactory requestFactory;
    @Mock
    Request request;
    @Mock 
    AuthenticationFactory authenticationFactory;
    @Mock
    SourceControlRepository repository;
    
    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(requestFactory.createRequest(eq(Request.MethodType.GET), anyString())).thenReturn(request);
    }

    @Test
    public void getUser() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory, authenticationFactory).getUser("fred");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/users/fred");
        verify(request, never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetChangeset() throws Exception
    {
    	when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
    	when(repository.getUsername()).thenReturn("");
    	when(repository.getPassword()).thenReturn("");

        new DefaultBitbucketConnection(requestFactory, authenticationFactory).getChangeset(repository, "1");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets/1");
        verify(request, never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetRepository() throws Exception
    {
    	when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
    	when(repository.getUsername()).thenReturn("");
    	when(repository.getPassword()).thenReturn("");

    	new DefaultBitbucketConnection(requestFactory, authenticationFactory).getRepository(repository);
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug");
        verify(request, never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetChangesets() throws Exception
    {
    	when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
    	when(repository.getUsername()).thenReturn("");
    	when(repository.getPassword()).thenReturn("");
    	
    	new DefaultBitbucketConnection(requestFactory, authenticationFactory).getChangesets(repository, null, BitbucketChangesetIterator.PAGE_SIZE);
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets?limit=15");
        verify(request, never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetChangeset() throws Exception
    {
    	when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
    	when(repository.getUsername()).thenReturn("user");
    	when(repository.getPassword()).thenReturn("pass");
    	when(authenticationFactory.getAuthentication(repository)).thenReturn(Authentication.basic("user", "pass"));
    	
    	new DefaultBitbucketConnection(requestFactory, authenticationFactory).getChangeset(repository, "1");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets/1");
        verify(request).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetRepository() throws Exception
    {
    	when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
    	when(repository.getUsername()).thenReturn("user");
    	when(repository.getPassword()).thenReturn("pass");
    	when(authenticationFactory.getAuthentication(repository)).thenReturn(Authentication.basic("user", "pass"));
    	
        new DefaultBitbucketConnection(requestFactory, authenticationFactory).getRepository(repository);
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug");
        verify(request).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetChangesets() throws Exception
    {
    	when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
    	when(repository.getUsername()).thenReturn("user");
    	when(repository.getPassword()).thenReturn("pass");
    	when(authenticationFactory.getAuthentication(repository)).thenReturn(Authentication.basic("user", "pass"));
    	
        new DefaultBitbucketConnection(requestFactory, authenticationFactory).getChangesets(repository, null, BitbucketChangesetIterator.PAGE_SIZE);
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets?limit=15");
        verify(request).addBasicAuthentication("user", "pass");
    }

}
