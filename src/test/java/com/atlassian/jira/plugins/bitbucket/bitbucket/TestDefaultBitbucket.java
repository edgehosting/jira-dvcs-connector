package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.BitbucketChangesetIterator;
import com.atlassian.jira.plugins.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucket;
import com.google.common.collect.Iterables;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
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
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", BitbucketChangesetIterator.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 72, BitbucketChangesetIterator.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 57, BitbucketChangesetIterator.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 42, BitbucketChangesetIterator.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 27, BitbucketChangesetIterator.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 12, 12);
        verifyNoMoreInteractions(bitbucketConnection);
    }

    private void setupBitbucketConnection() throws IOException
    {
        when(bitbucketConnection.getRepository(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector")).thenReturn(resource("TestBitbucket-repository.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", BitbucketChangesetIterator.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-tip.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 72, BitbucketChangesetIterator.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-72.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 57, BitbucketChangesetIterator.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-57.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 42, BitbucketChangesetIterator.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-42.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 27, BitbucketChangesetIterator.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-27.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", 12, 12)).thenReturn(resource("TestBitbucket-changesets-12.json"));
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

}
