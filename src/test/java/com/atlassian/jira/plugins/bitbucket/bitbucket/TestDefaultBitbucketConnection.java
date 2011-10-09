package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.BitbucketChangesetIterator;
import com.atlassian.jira.plugins.bitbucket.connection.impl.DefaultBitbucketConnection;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultBitbucketConnection}.
 */
public class TestDefaultBitbucketConnection
{
    @Mock
    RequestFactory requestFactory;
    @Mock
    Request request;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(requestFactory.createRequest(eq(Request.MethodType.GET), anyString())).thenReturn(request);
    }

    @Test
    public void getUser() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getUser("fred");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/users/fred");
        verify(request, never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetChangeset() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getChangeset(Authentication.ANONYMOUS, "owner", "slug", "1");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets/1");
        verify(request, never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetRepository() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getRepository(Authentication.ANONYMOUS, "owner", "slug");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug");
        verify(request, never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetChangesets() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getChangesets(Authentication.ANONYMOUS, "owner", "slug", null, BitbucketChangesetIterator.PAGE_SIZE);
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets?limit=15");
        verify(request, never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetChangeset() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getChangeset(Authentication.basic("user", "pass"), "owner", "slug", "1");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets/1");
        verify(request).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetRepository() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getRepository(Authentication.basic("user", "pass"), "owner", "slug");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug");
        verify(request).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetChangesets() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getChangesets(Authentication.basic("user", "pass"), "owner", "slug", null, BitbucketChangesetIterator.PAGE_SIZE);
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets?limit=15");
        verify(request).addBasicAuthentication("user", "pass");
    }

}
