package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Communicator;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.webwork.BitbucketPostCommit;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

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
        // TODO: now after BBC-77 recatoring we don't parse payload. we do request for all changesets since lastCommitDate. we have to refactor this test



//        String projectKey = "PRJ";
//        String repositoryUrl = "https://bitbucket.org/mjensen/test";
//        String payload = resource("TestBitbucketPostCommit-payload.json");
//        SourceControlRepository repo = new DefaultSourceControlRepository(0, "Pretty Name", "bitbucket", repositoryUri, projectKey, null, null, null);
//
//        when(repositoryManager.getRepositories(projectKey)).thenReturn(Arrays.asList(repo));
//        when(repositoryUri.getRepositoryUrl()).thenReturn(repositoryUrl);
//
//        BitbucketPostCommit bitbucketPostCommit = new BitbucketPostCommit(repositoryManager, synchronizer);
//        bitbucketPostCommit.setProjectKey(projectKey);
//        bitbucketPostCommit.setPayload(payload);
//        bitbucketPostCommit.execute();
//        verify(repositoryManager).parsePayload(repo, payload);
    }


}
