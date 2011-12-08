package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryUri;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

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
        RepositoryUri repositoryUri = new BitbucketRepositoryUri("https", "bitbucket.org","atlassian","jira-bitbucket-connector");
        when(repository.getRepositoryUri()).thenReturn(repositoryUri);
    }

}
