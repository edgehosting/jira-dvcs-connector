package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketConnection;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketException;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketChangesetIterator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.DefaultBitbucket;
import com.google.common.collect.Iterables;

/**
 * Unit tests for {@link BitbucketCommunicator}
 */
public class TestDefaultBitbucket
{
    @Mock
    BitbucketConnection bitbucketConnection;
    @Mock
    SourceControlRepository repository;


    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    private String resource(String name) throws IOException
    {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(name));
    }

    @Test
    public void testGetChangesetsLargeFromTip() throws Exception
    {
        setupBitbucketConnection();

        List<Changeset> list = new ArrayList<Changeset>();
        Iterables.addAll(list, new DefaultBitbucket(bitbucketConnection).getChangesets(repository));
        assertEquals(90, list.size());

        verifyBitbucketConnection();
    }
    @Test
    public void testIteratorCyclesOnNext() throws Exception
    {
        setupBitbucketConnection();

        Iterator<Changeset> changesets = new DefaultBitbucket(bitbucketConnection).getChangesets(repository).iterator();
        for(int i=0;i<90;i++) {
            try
            {
                changesets.next();
            }
            catch (Exception e)
            {
                fail("next() failed at index [ "+i+" ]");
            }
        }

        verifyBitbucketConnection();
    }

    private void verifyBitbucketConnection()
    {
        verify(bitbucketConnection, never()).getRepository(repository);
        verify(bitbucketConnection, times(1)).getChangesets(repository, null, BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(repository, "fc92e54ea14e", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(repository, "e62ad4bdd158", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(repository, "bbf518979ab2", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(repository, "551cb8f8ad63", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(repository, "e39284a71197", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verifyNoMoreInteractions(bitbucketConnection);
    }

    private void setupBitbucketConnection() throws IOException
    {
    	when(repository.getUrl()).thenReturn("https://bitbucket.org/atlassian/jira-bitbucket-connector");
        when(bitbucketConnection.getRepository(repository)).thenReturn(resource("TestBitbucket-repository.json"));
        when(bitbucketConnection.getChangesets(repository, null, BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-tip.json"));
        when(bitbucketConnection.getChangesets(repository, "fc92e54ea14e", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-72.json"));
        when(bitbucketConnection.getChangesets(repository, "e62ad4bdd158", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-57.json"));
        when(bitbucketConnection.getChangesets(repository, "bbf518979ab2", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-42.json"));
        when(bitbucketConnection.getChangesets(repository, "551cb8f8ad63", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-27.json"));
        when(bitbucketConnection.getChangesets(repository, "e39284a71197", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-12.json"));
    }

    @Test
    public void testGetUser() throws Exception
    {
        when(bitbucketConnection.getUser("mjensen")).thenReturn(resource("TestBitbucket-user.json"));
        SourceControlUser user = new DefaultBitbucket(bitbucketConnection).getUser("mjensen");
        assertEquals("https://secure.gravatar.com/avatar/e0fe5875ffbe955718f93b8a364454fe?d=identicon&s=32", user.getAvatar());
        assertEquals("mjensen", user.getUsername());
        assertEquals("Matthew", user.getFirstName());
        assertEquals("Jensen", user.getLastName());
        assertEquals("/1.0/users/mjensen", user.getResourceUri());
    }

    @Test
    public void testGetChangeset() throws Exception
    {
    	setupBitbucketConnection();
        when(bitbucketConnection.getChangeset(repository, "471b0c972ba6")).
                thenReturn(resource("TestBitbucket-changeset.json"));
        Changeset changeset = new DefaultBitbucket(bitbucketConnection).getChangeset(repository, "471b0c972ba6");
        assertEquals("471b0c972ba6", changeset.getNode());
        assertEquals("https://bitbucket.org/atlassian/jira-bitbucket-connector/changeset/471b0c972ba6", changeset.getCommitURL());
    }

    @Test
    public void testGetCachedChangeset() throws Exception
    {
    	setupBitbucketConnection();
        final DefaultBitbucket bitbucket = new DefaultBitbucket(bitbucketConnection);
        final Changeset changeset = BitbucketChangesetFactory.load(bitbucket, repository, "471b0c972ba6");

        assertEquals("471b0c972ba6", changeset.getNode());
        assertEquals("https://bitbucket.org/atlassian/jira-bitbucket-connector/changeset/471b0c972ba6", changeset.getCommitURL());
        verify(bitbucketConnection,never()).getChangeset(Matchers.<SourceControlRepository>anyObject(), anyString());
    }

    /**
     * BBC-64
     */
    @Test
    public void testGetUnknownUser()
    {
        when(bitbucketConnection.getUser("unknown")).thenThrow(new BitbucketException());
        SourceControlUser user = new DefaultBitbucket(bitbucketConnection).getUser("unknown");
        assertNotNull(user);
        assertEquals(SourceControlUser.UNKNOWN_USER,user);
    }

}
