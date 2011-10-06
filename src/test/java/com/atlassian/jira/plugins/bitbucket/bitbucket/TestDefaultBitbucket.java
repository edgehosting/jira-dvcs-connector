package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.BitbucketChangesetIterator;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucket;
import com.atlassian.jira.plugins.bitbucket.connection.BitbucketConnection;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Bitbucket}
 */
public class TestDefaultBitbucket
{
    @Mock
    BitbucketConnection bitbucketConnection;

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

        List<BitbucketChangeset> list = new ArrayList<BitbucketChangeset>();
        Iterables.addAll(list, new DefaultBitbucket(bitbucketConnection).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector"));
        assertEquals(90, list.size());

        verifyBitbucketConnection();
    }
    @Test
    public void testIteratorCyclesOnNext() throws Exception
    {
        setupBitbucketConnection();

        Iterator<BitbucketChangeset> changesets = new DefaultBitbucket(bitbucketConnection).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector").iterator();
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
        verify(bitbucketConnection, never()).getRepository(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector");
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", null, BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "fc92e54ea14e", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "e62ad4bdd158", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "bbf518979ab2", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "551cb8f8ad63", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "e39284a71197", BitbucketChangesetIterator.PAGE_SIZE + 1);
        verifyNoMoreInteractions(bitbucketConnection);
    }

    private void setupBitbucketConnection() throws IOException
    {
        when(bitbucketConnection.getRepository(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector")).thenReturn(resource("TestBitbucket-repository.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", null, BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-tip.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "fc92e54ea14e", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-72.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "e62ad4bdd158", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-57.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "bbf518979ab2", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-42.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "551cb8f8ad63", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-27.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "e39284a71197", BitbucketChangesetIterator.PAGE_SIZE + 1)).thenReturn(resource("TestBitbucket-changesets-12.json"));
    }

    @Test
    public void testGetUser() throws Exception
    {
        when(bitbucketConnection.getUser("mjensen")).thenReturn(resource("TestBitbucket-user.json"));
        BitbucketUser user = new DefaultBitbucket(bitbucketConnection).getUser("mjensen");
        assertEquals("https://secure.gravatar.com/avatar/e0fe5875ffbe955718f93b8a364454fe?d=identicon&s=32", user.getAvatar());
        assertEquals("mjensen", user.getUsername());
        assertEquals("Matthew", user.getFirstName());
        assertEquals("Jensen", user.getLastName());
        assertEquals("/1.0/users/mjensen", user.getResourceUri());
    }

    @Test
    public void testGetChangeset() throws Exception
    {
        when(bitbucketConnection.getChangeset(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "471b0c972ba6")).
                thenReturn(resource("TestBitbucket-changeset.json"));
        BitbucketChangeset changeset = new DefaultBitbucket(bitbucketConnection).
                getChangeset(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "471b0c972ba6");
        assertEquals("471b0c972ba6", changeset.getNode());
        assertEquals("https://bitbucket.org/atlassian/jira-bitbucket-connector/changeset/471b0c972ba6", changeset.getCommitURL());
    }

    @Test
    public void testGetCachedChangeset() throws Exception
    {
        final DefaultBitbucket bitbucket = new DefaultBitbucket(bitbucketConnection);
        final BitbucketChangeset changeset = BitbucketChangesetFactory.load(bitbucket,
                BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "471b0c972ba6");

        assertEquals("471b0c972ba6", changeset.getNode());
        assertEquals("https://bitbucket.org/atlassian/jira-bitbucket-connector/changeset/471b0c972ba6", changeset.getCommitURL());
        verify(bitbucketConnection,never()).getChangeset(Matchers.<BitbucketAuthentication>anyObject(), anyString(), anyString(), anyString());
    }

    /**
     * BBC-64
     */
    @Test
    public void testGetUnknownUser()
    {
        when(bitbucketConnection.getUser("unknown")).thenThrow(new BitbucketException());
        BitbucketUser user = new DefaultBitbucket(bitbucketConnection).getUser("unknown");
        assertNotNull(user);
        assertEquals(BitbucketUser.UNKNOWN_USER,user);
    }

}
