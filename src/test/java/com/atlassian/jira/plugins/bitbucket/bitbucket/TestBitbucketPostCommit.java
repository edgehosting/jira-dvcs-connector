package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultChangeset;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.webwork.BitbucketPostCommit;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link BitbucketPostCommit}
 */
@SuppressWarnings("deprecation")
public class TestBitbucketPostCommit
{
    @Mock
    Synchronizer synchronizer;
    @Mock
    RepositoryManager repositoryManager;
    @Mock
    RepositoryUri repositoryUri;
    @Mock
    Communicator communicator;


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
    public void testPostCommit() throws Exception
    {
        String projectKey = "PRJ";
        String repositoryUrl = "https://bitbucket.org/mjensen/test";
        String payload = resource("TestBitbucketPostCommit-payload.json");
        SourceControlRepository repo = new DefaultSourceControlRepository(0, "Pretty Name", "bitbucket", repositoryUri, projectKey, null, null, null, null, null);

        when(repositoryManager.getRepositories(projectKey)).thenReturn(Arrays.asList(repo));
        when(repositoryUri.getRepositoryUrl()).thenReturn(repositoryUrl);

        BitbucketPostCommit bitbucketPostCommit = new BitbucketPostCommit(repositoryManager, synchronizer);
        bitbucketPostCommit.setProjectKey(projectKey);
        bitbucketPostCommit.setPayload(payload);
        bitbucketPostCommit.execute();
        verify(repositoryManager).parsePayload(repo, payload);
    }

    @Test
    public void testParsePayload() throws Exception
    {
        final String projectKey = "PRJ";
        final String payload = resource("TestBitbucketPostCommit-payload.json");
        final String node = "f2851c9f1db8";
        final DefaultSourceControlRepository repo = new DefaultSourceControlRepository(0, "Pretty Name", "bitbucket", repositoryUri, projectKey, null, null, null, null, null);

        BitbucketRepositoryManager brm = new BitbucketRepositoryManager(null, communicator, null, null, null, null);

        when(communicator.getChangeset(eq(repo), anyString())).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return new DefaultChangeset(0, (String) invocation.getArguments()[1], "");
            }
        });

        List<Changeset> changesets = brm.parsePayload(repo, payload);

        ArgumentMatcher<List<Changeset>> matcher = new ArgumentMatcher<List<Changeset>>()
        {
            @Override
            public boolean matches(Object o)
            {
                //noinspection unchecked
                @SuppressWarnings("unchecked")
                List<Changeset> list = (List<Changeset>) o;
                Changeset changeset = list.get(0);
                return list.size() == 1 && changeset.getNode().equals(node);
            }
        };
        Assert.assertTrue(matcher.matches(changesets));
        verify(communicator).getChangeset(repo, node);
    }

}
