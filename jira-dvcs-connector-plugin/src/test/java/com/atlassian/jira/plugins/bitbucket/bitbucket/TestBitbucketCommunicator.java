package com.atlassian.jira.plugins.bitbucket.bitbucket;


import static junit.framework.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.impl.BasicAuthentication;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultRequestHelper;
import com.atlassian.jira.plugins.bitbucket.spi.ExtendedResponseHandler;
import com.atlassian.jira.plugins.bitbucket.spi.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.bitbucket.spi.ExtendedResponseHandlerFactory;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketCommunicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryUri;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;

public class TestBitbucketCommunicator
{
    @SuppressWarnings("rawtypes")
    @Mock
    private RequestFactory requestFactory;
    @Mock
    private AuthenticationFactory authenticationFactory;
    @Mock
    private SourceControlRepository repository;
    @Mock
    private Request<?, ?> request;
    @Mock
    private RepositoryUri repositoryUri;
    @Mock
    private ExtendedResponseHandlerFactory responseHandlerFactory;
    @Mock
    private ExtendedResponseHandler extendedResponseHandler;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        RepositoryUri repositoryUri = new BitbucketRepositoryUri("https", "bitbucket.org","atlassian","jira-bitbucket-connector");
        when(repository.getRepositoryUri()).thenReturn(repositoryUri);
    }

    private String resource(String name) throws IOException
    {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(name));
    }

    @Test
    public void testUnknownUser() throws Exception
    {
        when(requestFactory.createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/users/mjensen")).thenReturn(request);
        when(request.execute()).thenReturn("");

        BitbucketCommunicator communicator = new BitbucketCommunicator(authenticationFactory, new DefaultRequestHelper(requestFactory, responseHandlerFactory));
        SourceControlUser user = communicator.getUser(repository, "mjensen");
        assertNotNull(user);
        assertEquals(SourceControlUser.UNKNOWN_USER, user);
    }

    @Test
    public void testGetUser() throws Exception
    {
        when(requestFactory.createRequest(Request.MethodType.GET, "https://api.bitbucket.org/1.0/users/mjensen")).thenReturn(request);
        when(request.execute()).thenReturn(resource("TestBitbucket-user.json"));

        BitbucketCommunicator communicator = new BitbucketCommunicator(authenticationFactory, new DefaultRequestHelper(requestFactory, responseHandlerFactory));
        SourceControlUser user = communicator.getUser(repository, "mjensen");

        verify(request).setSoTimeout(60000);
        verify(request).execute();
        verifyNoMoreInteractions(request);
        assertEquals("https://secure.gravatar.com/avatar/e0fe5875ffbe955718f93b8a364454fe?d=identicon&s=32", user.getAvatar());
        assertEquals("mjensen", user.getUsername());
        assertEquals("Matthew", user.getFirstName());
        assertEquals("Jensen", user.getLastName());
        assertEquals("/1.0/users/mjensen", user.getResourceUri());
    }

    @Test
    public void testAuthenticationAndException() throws Exception
    {
        when(authenticationFactory.getAuthentication(repository)).thenReturn(new BasicAuthentication("user", "pass"));
        when(
            requestFactory.createRequest(Request.MethodType.GET,
                "https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/changesets/aaaaa")).thenReturn(request);
        when(request.execute()).thenReturn("{I am invalid json}");
        when(
            requestFactory.createRequest(Request.MethodType.GET,
                "https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/changesets/aaaaa/diffstat?limit=5")).thenReturn(request);
        when(request.execute()).thenReturn("{I am invalid json}");

        BitbucketCommunicator communicator = new BitbucketCommunicator(authenticationFactory, new DefaultRequestHelper(requestFactory, responseHandlerFactory));
        try
        {
            communicator.getChangeset(repository, "aaaaa");
            fail("Expected exception when parsing invalid response");
        } catch (SourceControlException e)
        {
            assertEquals("Could not parse json result", e.getMessage());
        }
        verify(request, times(2)).addBasicAuthentication("user", "pass");

    }

    @Test
    public void setupPostcommitHook()
    {
        when(repository.getAdminUsername()).thenReturn("user");
        when(repository.getAdminPassword()).thenReturn("pass");
        when(
            requestFactory.createRequest(Request.MethodType.POST,
                "https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/services")).thenReturn(request);

        String postCommitUrl = "http://this.jira.server:1234/jira/rest/postcommithandler";
        BitbucketCommunicator communicator = new BitbucketCommunicator(authenticationFactory, new DefaultRequestHelper(requestFactory, responseHandlerFactory));

        communicator.setupPostcommitHook(repository, postCommitUrl);

        verify(requestFactory).createRequest(Request.MethodType.POST,
            "https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/services");
        verify(request).addBasicAuthentication("user", "pass");
        verify(request).setRequestBody("type=post;URL=" + postCommitUrl);
    }

    @Test
    public void testPublicRepositoryValid() throws Exception
    {
        ExtendedResponse extendedResponse = new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-repository.json"));
        when(responseHandlerFactory.create()).thenReturn(extendedResponseHandler);
        when(extendedResponseHandler.getExtendedResponse()).thenReturn(extendedResponse);
        when(requestFactory.createRequest(any(Request.MethodType.class), any(String.class))).thenReturn(request);

        DefaultRequestHelper requestHelper = new DefaultRequestHelper(requestFactory, responseHandlerFactory);
        Boolean repositoryIsPrivate = requestHelper.isRepositoryPrivate1(repositoryUri);

        assertNotNull(repositoryIsPrivate);
        assertFalse(repositoryIsPrivate);
    }

    @Test
    public void testPrivateRepositoryValid() throws Exception
    {
        final ExtendedResponse extendedResponse = new ExtendedResponse(false, HttpStatus.SC_UNAUTHORIZED, "blah");
        when(responseHandlerFactory.create()).thenReturn(extendedResponseHandler);
        when(extendedResponseHandler.getExtendedResponse()).thenReturn(extendedResponse);
        when(requestFactory.createRequest(any(Request.MethodType.class), any(String.class))).thenReturn(request);

        DefaultRequestHelper requestHelper = new DefaultRequestHelper(requestFactory, responseHandlerFactory);
        Boolean repositoryIsPrivate = requestHelper.isRepositoryPrivate1(repositoryUri);

        assertNotNull(repositoryIsPrivate);
        assertTrue(repositoryIsPrivate);
    }

    @Test
    public void testRepositoryInvalid() throws Exception
    {

        final ExtendedResponse extendedResponse = new ExtendedResponse(false, HttpStatus.SC_NOT_FOUND, "blah");

        when(responseHandlerFactory.create()).thenReturn(extendedResponseHandler);
        when(extendedResponseHandler.getExtendedResponse()).thenReturn(extendedResponse);
        when(requestFactory.createRequest(any(Request.MethodType.class), any(String.class))).thenReturn(request);

        DefaultRequestHelper requestHelper = new DefaultRequestHelper(requestFactory, responseHandlerFactory);
        Boolean repositoryIsPrivate = requestHelper.isRepositoryPrivate1(repositoryUri);

        assertNull(repositoryIsPrivate);
    }
}
