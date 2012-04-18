package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultRequestHelper;
import com.atlassian.jira.plugins.bitbucket.spi.ExtendedResponseHandler;
import com.atlassian.jira.plugins.bitbucket.spi.ExtendedResponseHandler.ExtendedResponse;
import com.atlassian.jira.plugins.bitbucket.spi.ExtendedResponseHandlerFactory;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketChangesetIterator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketCommunicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryUri;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Iterables;

/**
 * Unit tests for {@link com.atlassian.jira.plugins.bitbucket.api.Communicator}
 */
public class TestDefaultBitbucket
{

    @SuppressWarnings("rawtypes")
    @Mock
    private RequestFactory requestFactory;
    @Mock
    private AuthenticationFactory authenticationFactory;
    @Mock
    SourceControlRepository repository;
    @Mock
    private Request<?, ?> request;
    @Mock
    private ExtendedResponseHandlerFactory extendedResponseHandlerFactory;
    @Mock
    private ExtendedResponseHandler responseHandler;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    private String resource(String name) throws IOException
    {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(name));
    }

    private void setupBitbucketConnection() throws IOException, ResponseException
    {
        when(requestFactory.createRequest(any(Request.MethodType.class), anyString())).thenReturn(request);
        RepositoryUri repositoryUri = new BitbucketRepositoryUri("https", "bitbucket.org","atlassian","jira-bitbucket-connector");
        when(repository.getRepositoryUri()).thenReturn(repositoryUri);
        when(extendedResponseHandlerFactory.create()).thenReturn(responseHandler);
        
        when(responseHandler.getExtendedResponse())
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesets-tip.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesetFiles.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesets-72.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesetFiles.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesets-57.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesetFiles.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesets-42.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesetFiles.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesets-27.json")))
//                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesetFiles.json")))
                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesets-12.json")))
                .thenReturn(new ExtendedResponse(true, HttpStatus.SC_OK, resource("TestBitbucket-changesetFiles.json")));

    }

    @Test
    public void testGetChangesetsLargeFromTip() throws Exception
    {
        setupBitbucketConnection();
        
        BitbucketCommunicator bitbucketCommunicator = new BitbucketCommunicator(authenticationFactory, new DefaultRequestHelper(requestFactory, extendedResponseHandlerFactory));

        final BitbucketChangesetIterator changesetIterator = new BitbucketChangesetIterator(bitbucketCommunicator, repository, null);
        Iterable<Changeset> iterable = new Iterable<Changeset>()
        {
            @Override
            public Iterator<Changeset> iterator()
            {
                return changesetIterator;
            }
        };

        List<Changeset> list = new ArrayList<Changeset>();
        Iterables.addAll(list, iterable);

        assertEquals(15, list.size());

    }

    @Test
    public void testIteratorCyclesOnNext() throws Exception
    {
        setupBitbucketConnection();
        
        BitbucketCommunicator bitbucketCommunicator = new BitbucketCommunicator(authenticationFactory, new DefaultRequestHelper(requestFactory, extendedResponseHandlerFactory));
        final BitbucketChangesetIterator changesetIterator = new BitbucketChangesetIterator(bitbucketCommunicator,
                repository, null);

        for (int i = 0; i < 15; i++)
        {
            try
            {
                changesetIterator.next();
            } catch (Exception e)
            {
                fail("next() failed at index [ " + i + " ]");
            }
        }
    }

}
