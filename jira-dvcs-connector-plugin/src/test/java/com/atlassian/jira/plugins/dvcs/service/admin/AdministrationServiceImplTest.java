package com.atlassian.jira.plugins.dvcs.service.admin;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.event.EventService;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.google.common.collect.ImmutableSet;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class AdministrationServiceImplTest
{
    private static final int TOTAL_NUMBER_OF_ISSUE_KEYS = 1000;
    private static final String ISSUE_KEY = "TEST-1";
    private static final ImmutableSet<String> ISSUE_KEYS = ImmutableSet.of(ISSUE_KEY);

    @Mock
    private ChangesetDao changesetDao;

    @Mock
    private EventService eventService;

    @Mock
    private DevSummaryCachePrimingStatus status;

    @InjectMocks
    private AdministrationServiceImpl administrationService;

    @BeforeMethod
    public void setup()
    {
        when(changesetDao.getNumberOfDistinctIssueKeysToCommit()).thenReturn(TOTAL_NUMBER_OF_ISSUE_KEYS);
    }

    @Test
    public void testNotRunning()
    {
        when(status.startExclusively(anyInt(), anyInt())).thenReturn(true);

        assertThat(administrationService.primeDevSummaryCache(), is(true));
    }

    @Test
    public void testExceptionFails()
    {
        when(status.startExclusively(anyInt(), anyInt())).thenReturn(true);
        final RuntimeException expectedException = new RuntimeException("foo");
        when(changesetDao.forEachIssueToCommitMapping(any(ChangesetDao.ForEachIssueToCommitMappingClosure.class))).thenThrow(expectedException);

        assertThat(administrationService.primeDevSummaryCache(), is(false));
        verify(status).failed(eq(expectedException), any(String.class));
    }

    @Test
    public void testRunning()
    {
        when(status.startExclusively(anyInt(), anyInt())).thenReturn(false);
        assertThat(administrationService.primeDevSummaryCache(), is(false));
    }

    @Test
    public void testClosureReturnsFalseWhenStopped()
    {
        AdministrationServiceImpl.PrimeCacheClosure closure = new AdministrationServiceImpl.PrimeCacheClosure(eventService, status);
        when(status.isStopped()).thenReturn(true);

        assertThat(closure.execute("bitbucket", 1, ISSUE_KEYS), is(false));
    }

    @Test
    public void testClosureReturnsTrueWhenRunning()
    {
        AdministrationServiceImpl.PrimeCacheClosure closure = new AdministrationServiceImpl.PrimeCacheClosure(eventService, status);
        when(status.isStopped()).thenReturn(false);

        assertThat(closure.execute("bitbucket", 1, ISSUE_KEYS), is(true));
    }
}
