package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketChangesetFactory;

public class TestBitbucketChansetFactory
{
    @Mock
    private SourceControlRepository repository;
    @Mock
    private Communicator communicator;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(repository.getUrl()).thenReturn("https://bitbucket.org/atlassian/jira-bitbucket-connector");
    }

    @Test
    public void testLazyLoadedChangeset() throws Exception
    {
        final Changeset changeset = BitbucketChangesetFactory.load(communicator, repository, "471b0c972ba6");

        assertEquals("471b0c972ba6", changeset.getNode());
        assertEquals("https://bitbucket.org/atlassian/jira-bitbucket-connector/changeset/471b0c972ba6",
                changeset.getCommitURL(repository));
        verify(communicator, never()).getChangeset(Matchers.<SourceControlRepository> anyObject(), anyString());
    }
}
