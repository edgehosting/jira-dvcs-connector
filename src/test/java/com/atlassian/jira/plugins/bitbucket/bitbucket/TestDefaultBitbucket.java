package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.api.AuthenticationFactory;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketChangesetIterator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketCommunicator;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Iterables;

/**
 * Unit tests for {@link com.atlassian.jira.plugins.bitbucket.spi.Communicator}
 */
public class TestDefaultBitbucket
{

    @Mock
    private RequestFactory requestFactory;
    @Mock
    private AuthenticationFactory authenticationFactory;
    @Mock
    SourceControlRepository repository;
    @Mock
    private Request<?, ?> request;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    private String resource(String name) throws IOException
    {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(name));
    }

    private void setupRequest(String queryParams) throws ResponseException, IOException
    {
        when(
                requestFactory.createRequest(Request.MethodType.GET,
                        "https://api.bitbucket.org/1.0/repositories/atlassian/jira-bitbucket-connector/changesets?"
                                + queryParams)).thenReturn(request);
    }

    private void setupBitbucketConnection() throws IOException, ResponseException
    {
        when(repository.getUrl()).thenReturn("https://bitbucket.org/atlassian/jira-bitbucket-connector");

        setupRequest("limit=16");
        setupRequest("limit=16&start=fc92e54ea14e");
        setupRequest("limit=16&start=e62ad4bdd158");
        setupRequest("limit=16&start=bbf518979ab2");
        setupRequest("limit=16&start=551cb8f8ad63");
        setupRequest("limit=16&start=e39284a71197");

        when(request.execute()).thenReturn(resource("TestBitbucket-changesets-tip.json"))
                .thenReturn(resource("TestBitbucket-changesets-72.json"))
                .thenReturn(resource("TestBitbucket-changesets-57.json"))
                .thenReturn(resource("TestBitbucket-changesets-42.json"))
                .thenReturn(resource("TestBitbucket-changesets-27.json"))
                .thenReturn(resource("TestBitbucket-changesets-12.json"));
    }

    @Test
    public void testGetChangesetsLargeFromTip() throws Exception
    {
        setupBitbucketConnection();
        BitbucketCommunicator bitbucketCommunicator = new BitbucketCommunicator(requestFactory, authenticationFactory);

        final BitbucketChangesetIterator changesetIterator = new BitbucketChangesetIterator(bitbucketCommunicator,
                repository);
        Iterable<Changeset> iterable = new Iterable<Changeset>()
        {
            public Iterator<Changeset> iterator()
            {
                return changesetIterator;
            }
        };

        List<Changeset> list = new ArrayList<Changeset>();
        Iterables.addAll(list, iterable);

        assertEquals(90, list.size());

    }

    @Test
    public void testIteratorCyclesOnNext() throws Exception
    {
        setupBitbucketConnection();
        BitbucketCommunicator bitbucketCommunicator = new BitbucketCommunicator(requestFactory, authenticationFactory);

        final BitbucketChangesetIterator changesetIterator = new BitbucketChangesetIterator(bitbucketCommunicator,
                repository);

        for (int i = 0; i < 90; i++)
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
