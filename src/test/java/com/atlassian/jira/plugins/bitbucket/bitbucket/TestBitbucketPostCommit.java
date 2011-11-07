package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.webwork.BitbucketPostCommit;

/**
 * Unit test for {@link BitbucketPostCommit}
 */
public class TestBitbucketPostCommit
{
    @Mock
    Synchronizer synchronizer;
    @Mock
	private RepositoryManager repositoryManager;


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
    	SourceControlRepository repo = new DefaultSourceControlRepository(0, repositoryUrl, projectKey, null, null, null, null, "bitbucket");
    	when(repositoryManager.getRepositories(projectKey)).thenReturn(Arrays.asList(repo));

		BitbucketPostCommit bitbucketPostCommit = new BitbucketPostCommit(repositoryManager, synchronizer);
		bitbucketPostCommit.setProjectKey(projectKey);
		bitbucketPostCommit.setPayload(payload);
        bitbucketPostCommit.execute();
		verify(repositoryManager).parsePayload(repo, payload);
    }

    @Test
    public void testParsePayload() throws Exception
	{
    	String projectKey = "PRJ";
    	String repositoryUrl = "https://bitbucket.org/mjensen/test";
    	String payload = resource("TestBitbucketPostCommit-payload.json");
    	DefaultSourceControlRepository repo = new DefaultSourceControlRepository(0, repositoryUrl, projectKey, null, null, null, null, "bitbucket");

    	BitbucketRepositoryManager brm = new BitbucketRepositoryManager(null, null, null, null);
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
		        return list.size()==1 && changeset.getNode().equals("f2851c9f1db8");
		    }
		};
		assertTrue(matcher.matches(changesets));
    }

}
