package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.event.EventLimit.BRANCH;
import static com.atlassian.jira.plugins.dvcs.event.EventLimit.COMMIT;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class EventLimiterTest
{
    static final Long SYNC_INTERVAL = HOURS.toMillis(1);

    SyncEvent branchEvent;
    SyncEvent commitEvent;

    @Mock
    SyncConfig syncConfig;

    @InjectMocks
    EventLimiter eventLimiter;

    @BeforeMethod
    public void setUp() throws Exception
    {
        when(syncConfig.scheduledSyncIntervalMillis()).thenReturn(SYNC_INTERVAL);
        when(syncConfig.getEffectiveLimit(any(EventLimit.class))).thenAnswer(new Answer<Integer>()
        {
            @Override
            public Integer answer(final InvocationOnMock invocation) throws Throwable
            {
                return ((EventLimit) invocation.getArguments()[0]).getDefaultLimit();
            }
        });

        branchEvent = new LimitedTestEvent(BRANCH);
        commitEvent = new LimitedTestEvent(COMMIT);
    }

    @Test
    public void unlimitedEventShouldNotHaveAnyLimits() throws Exception
    {
        for (int i = 0; i < 100; i++)
        {
            assertThat(eventLimiter.isLimitExceeded(new TestEvent(), false), equalTo(false));
        }
    }

    @Test
    public void limitShouldBeUsedAsIsForWebHookTriggeredSync() throws Exception
    {
        assertThat(eventLimiter, limitsEventTo(branchEvent, false, BRANCH.getDefaultLimit()));
    }

    @Test
    public void limitShouldIncreaseProportionallyToScheduleIntervalForScheduledSync() throws Exception
    {
        final int syncIntervalMinutes = 30;
        when(syncConfig.scheduledSyncIntervalMillis()).thenReturn(MINUTES.toMillis(syncIntervalMinutes));

        assertThat(eventLimiter, limitsEventTo(branchEvent, true, BRANCH.getDefaultLimit() * syncIntervalMinutes));
    }

    @Test
    public void limitShouldNotBeHigherThanOneHoursWorthOfEventsForScheduledSync() throws Exception
    {
        final int syncIntervalMinutes = 120;
        when(syncConfig.scheduledSyncIntervalMillis()).thenReturn(MINUTES.toMillis(syncIntervalMinutes));

        // allow at most one hour of events per sync
        assertThat(eventLimiter, limitsEventTo(branchEvent, true, BRANCH.getDefaultLimit() * 60));
    }

    @Test
    public void limiterShouldKeepTrackOfLimitExceededCount() throws Exception
    {
        final int branchLimit = BRANCH.getDefaultLimit();
        final int commitLimit = COMMIT.getDefaultLimit();
        final int extra = 2;
        final int tries = Math.max(branchLimit, commitLimit) + extra;

        // try 2 extra events of each type
        for (int i = 0; i < tries; i++) { eventLimiter.isLimitExceeded(branchEvent, false); }
        for (int i = 0; i < tries; i++) { eventLimiter.isLimitExceeded(commitEvent, false); }

        assertThat(eventLimiter.getLimitExceededCount(), equalTo((tries - branchLimit) + (tries - commitLimit)));
    }

    @Test
    public void limiterTakesOverridesIntoAccount() throws Exception
    {
        final int maxCommits = 1;
        when(syncConfig.getEffectiveLimit(COMMIT)).thenReturn(maxCommits);

        assertThat(eventLimiter, limitsEventTo(new LimitedTestEvent(COMMIT), false, maxCommits));
        assertThat(eventLimiter, limitsEventTo(new LimitedTestEvent(BRANCH), false, BRANCH.getDefaultLimit()));
    }

    /**
     * @return a matcher that returns true if the event limiter limits {@code limit} events
     */
    private TypeSafeMatcher<EventLimiter> limitsEventTo(final SyncEvent syncEvent, final boolean scheduledSync, final int limit)
    {
        return new TypeSafeMatcher<EventLimiter>()
        {
            int i = 0;

            @Override
            protected boolean matchesSafely(final EventLimiter item)
            {
                while (i <= limit) // <- test up to boundary
                {
                    boolean limitReached = i == limit;
                    boolean limitExceeded = eventLimiter.isLimitExceeded(syncEvent, scheduledSync);
                    if (limitExceeded != limitReached)
                    {
                        return false;
                    }

                    i++;
                }

                return true;
            }

            @Override
            public void describeTo(final Description description)
            {
                description.appendText(String.format("an EventLimiter with a limit of %d (was: %d)", limit, i));
            }
        };
    }
}
