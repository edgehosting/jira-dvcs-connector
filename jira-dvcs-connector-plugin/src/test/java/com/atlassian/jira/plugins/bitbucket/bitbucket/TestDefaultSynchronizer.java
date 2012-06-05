package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Communicator;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.api.impl.SynchronizationKey;

/**
 * Unit tests for {@link DefaultSynchronizer}
 */
@Ignore
@Deprecated // TO BE DELETED SOON
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
        when(repository.getProjectKey()).thenReturn("PRJ");
        SynchronizationKey key = new SynchronizationKey(repository);
        SynchronisationOperation synchronisation = new DefaultSynchronisationOperation(key, repositoryManager, bitbucket, progressProvider, issueManager);
        when(repositoryManager.getSynchronisationOperation(any(SynchronizationKey.class), any(ProgressWriter.class))).thenReturn(
            synchronisation);
        when(bitbucket.getChangesets(repository, null)).thenReturn(Arrays.asList(changeset));
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
