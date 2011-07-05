package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.mapper.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.webwork.BitbucketPostCommit;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link BitbucketPostCommit}
 */
public class TestBitbucketPostCommit
{
    @Mock
    Synchronizer synchronizer;

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
        BitbucketPostCommit bitbucketPostCommit = new BitbucketPostCommit(synchronizer);
        bitbucketPostCommit.setBranch("default");
        bitbucketPostCommit.setProjectKey("PRJ");
        bitbucketPostCommit.setPayload(resource("TestBitbucketPostCommit-payload.json"));
        bitbucketPostCommit.execute();

        verify(synchronizer, times(1)).synchronize(eq("PRJ"), eq(RepositoryUri.parse("mjensen/test/default")),
                argThat(new ArgumentMatcher<List<BitbucketChangeset>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        List<BitbucketChangeset> list = (List<BitbucketChangeset>) o;
                        BitbucketChangeset changeset = list.get(0);
                        return list.size()==1 && changeset.getNode().equals("f2851c9f1db8");
                    }
                }));
    }


}
