package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Bitbucket}
 */
public class TestBitbucket
{
    @Mock
    RequestFactory requestFactory;
    @Mock
    Request request;
    private JSONObject repositoryJSON;
    private JSONObject changesetJSON;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        repositoryJSON = new JSONObject();
        repositoryJSON.put("website", "");
        repositoryJSON.put("name", "");
        repositoryJSON.put("followers_count", "0");
        repositoryJSON.put("owner", "");
        repositoryJSON.put("logo", "");
        repositoryJSON.put("resource_uri", "");
        repositoryJSON.put("slug", "");
        repositoryJSON.put("description", "");

        changesetJSON = new JSONObject();
        changesetJSON.put("node", "");
        changesetJSON.put("raw_author", "");
        changesetJSON.put("author", "");
        changesetJSON.put("timestamp", "");
        changesetJSON.put("raw_node", "");
        changesetJSON.put("branch", "");
        changesetJSON.put("message", "");
        changesetJSON.put("revision", "0");
    }

    @Test
    public void getAnonymousGetRepository() throws Exception
    {
        when(requestFactory.createRequest(
                Request.MethodType.GET,
                "https://api.bitbucket.org/1.0/repositories/owner/slug"))
                .thenReturn(request);
        when(request.execute()).thenReturn(repositoryJSON.toString());
        new Bitbucket(requestFactory).getRepository("owner", "slug");
        verify(request, never()).addBasicAuthentication(any(String.class),any(String.class));
    }

    @Test
    public void getAnonymousGetChangeset() throws Exception
    {
        when(requestFactory.createRequest(
                Request.MethodType.GET,
                "https://api.bitbucket.org/1.0/repositories/owner/slug"))
                .thenReturn(request);
        when(requestFactory.createRequest(
                Request.MethodType.GET,
                "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets/1234"))
                .thenReturn(request);
        when(request.execute())
                .thenReturn(repositoryJSON.toString())
                .thenReturn(changesetJSON.toString());
        new Bitbucket(requestFactory).getRepository("owner", "slug").changeset("1234");
        verify(request, never()).addBasicAuthentication(any(String.class),any(String.class));
    }

    @Test
    public void getAuthenticatedGetChangeset() throws Exception
    {
        when(requestFactory.createRequest(
                Request.MethodType.GET,
                "https://api.bitbucket.org/1.0/repositories/owner/slug"))
                .thenReturn(request);
        when(requestFactory.createRequest(
                Request.MethodType.GET,
                "https://api.bitbucket.org/1.0/repositories/owner/slug/changesets/1234"))
                .thenReturn(request);
        when(request.execute())
                .thenReturn(repositoryJSON.toString())
                .thenReturn(changesetJSON.toString());
        new Bitbucket(requestFactory, "user", "pass").getRepository("owner", "slug").changeset("1234");
        verify(request, times(2)).addBasicAuthentication("user", "pass");
    }

    @Test
    public void getAuthenticatedGetPublicRepositoryUrl() throws Exception
    {
        when(requestFactory.createRequest(
                Request.MethodType.GET,
                "https://api.bitbucket.org/1.0/repositories/owner/slug"))
                .thenReturn(request);
        when(request.execute()).thenReturn(repositoryJSON.toString());
        new Bitbucket(requestFactory,"user","pass").getRepository("owner", "slug");
        verify(request).addBasicAuthentication("user", "pass");
    }
}
