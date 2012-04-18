package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DefaultSynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryUri;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultSynchronizer}
 */
public class TestDefaultSynchronizer
{
    @Mock
    private Communicator bitbucket;
    @Mock
    private Changeset changeset;
    @Mock
    private RepositoryManager repositoryManager;
    @Mock
    private SourceControlRepository repository;
    @Mock
    private ProgressWriter progressProvider;
    @Mock
    private IssueManager issueManager;
    @Mock
    private MutableIssue someIssue;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSynchronizeAddsSingleMapping() throws InterruptedException
    {
        RepositoryUri repositoryUri = new BitbucketRepositoryUri("https", "bitbucket.org","owner","slug");
        when(repository.getRepositoryUri()).thenReturn(repositoryUri);

        when(repository.getProjectKey()).thenReturn("PRJ");
        SynchronizationKey key = new SynchronizationKey(repository);
        SynchronisationOperation synchronisation = new DefaultSynchronisationOperation(key, repositoryManager, bitbucket, progressProvider, issueManager);
        when(repositoryManager.getSynchronisationOperation(any(SynchronizationKey.class), any(ProgressWriter.class))).thenReturn(
            synchronisation);
        when(bitbucket.getChangesets(repositoryManager, repository, null)).thenReturn(Arrays.asList(changeset));
        when(changeset.getMessage()).thenReturn("PRJ-1 Message");
        when(issueManager.getIssueObject(anyString())).thenReturn(someIssue);

        DefaultSynchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadExecutor(), repositoryManager);
        assertNull(synchronizer.getProgress(repository));

        synchronizer.synchronize(repository);

        assertNotNull(synchronizer.getProgress(repository));

        Progress progress = synchronizer.getProgress(repository);
        while (!progress.isFinished())
        {
            Thread.sleep(10);
        }
        verify(repositoryManager, times(1)).addChangeset(repository, "PRJ-1", changeset);
    }

}
