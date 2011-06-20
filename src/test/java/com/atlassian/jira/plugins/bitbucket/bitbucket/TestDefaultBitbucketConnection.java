package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.connection.DefaultBitbucketConnection;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        verify(request,never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetChangeset() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getChangeset(BitbucketAuthentication.ANONYMOUS, "owner", "slug", "1");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets/1");
        verify(request,never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetRepository() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getRepository(BitbucketAuthentication.ANONYMOUS, "owner", "slug");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug");
        verify(request,never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAnonymousGetChangesets() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getChangesets(BitbucketAuthentication.ANONYMOUS, "owner", "slug", "tip", 15);
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets");
        verify(request).addRequestParameters("limit", "15", "start", "tip");
        verify(request,never()).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetChangeset() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getChangeset(BitbucketAuthentication.basic("user","pass"), "owner", "slug", "1");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets/1");
        verify(request).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetRepository() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getRepository(BitbucketAuthentication.basic("user","pass"), "owner", "slug");
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug");
        verify(request).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetChangesets() throws Exception
    {
        new DefaultBitbucketConnection(requestFactory).getChangesets(BitbucketAuthentication.basic("user","pass"), "owner", "slug", "tip", 15);
        verify(requestFactory).createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets");
        verify(request).addRequestParameters("limit", "15", "start", "tip");
        verify(request).addBasicAuthentication("user", "pass");
    }

}
