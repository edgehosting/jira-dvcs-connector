package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.bitbucket.connection.BitbucketConnection;
import com.atlassian.jira.plugins.bitbucket.bitbucket.remote.RemoteBitbucket;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Bitbucket}
 */
public class TestRemoteBitbucket
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
        when(bitbucketConnection.getRepository(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector")).thenReturn(resource("TestBitbucket-repository.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "tip", RemoteBitbucket.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-tip.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "72", RemoteBitbucket.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-72.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "57", RemoteBitbucket.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-57.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "42", RemoteBitbucket.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-42.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "27", RemoteBitbucket.PAGE_SIZE)).thenReturn(resource("TestBitbucket-changesets-27.json"));
        when(bitbucketConnection.getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "12", 12)).thenReturn(resource("TestBitbucket-changesets-12.json"));
        new RemoteBitbucket(bitbucketConnection).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector");
        verify(bitbucketConnection, never()).getRepository(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector");
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector","tip",RemoteBitbucket.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "72", RemoteBitbucket.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "57", RemoteBitbucket.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "42", RemoteBitbucket.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "27", RemoteBitbucket.PAGE_SIZE);
        verify(bitbucketConnection, times(1)).getChangesets(BitbucketAuthentication.ANONYMOUS, "atlassian", "jira-bitbucket-connector", "12", 12);
        verifyNoMoreInteractions(bitbucketConnection);
    }

}
