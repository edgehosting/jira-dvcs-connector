package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.event.EventLimit.BRANCH;
import static com.atlassian.jira.plugins.dvcs.event.EventLimit.COMMIT;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class EventLimiterTest
{
    static final Long SYNC_INTERVAL = HOURS.toMillis(1);

    /**
     * A limited event raised during a webhook-initiated sync.
     */
    SyncEvent limitedWebhookEvent;

    /**
     * A limited event raised during a scheduled sync.
     */
    SyncEvent limitedScheduledEvent;

    @Mock
    SyncConfig syncConfig;

    @InjectMocks
    EventLimiter eventLimiter;

    @BeforeMethod
    public void setUp() throws Exception
    {
        when(syncConfig.scheduledSyncIntervalMillis()).thenReturn(SYNC_INTERVAL);
        limitedWebhookEvent = new LimitedTestEvent(BRANCH, false);
        limitedScheduledEvent = new LimitedTestEvent(BRANCH, true);
    }

    @Test
    public void unlimitedEventShouldNotHaveAnyLimits() throws Exception
    {
        for (int i = 0; i < 100; i++)
        {
            assertThat(eventLimiter.isLimitExceeded(new TestEvent()), equalTo(false));
        }
    }

    @Test
    public void limitShouldBeUsedAsIsForWebHookTriggeredSync() throws Exception
    {
        assertThat(eventLimiter, limitsEventTo(limitedWebhookEvent, BRANCH.getDefaultLimit()));
    }

    @Test
    public void limitShouldIncreaseProportionallyToScheduleIntervalForScheduledSync() throws Exception
    {
        final int syncIntervalMinutes = 30;
        when(syncConfig.scheduledSyncIntervalMillis()).thenReturn(MINUTES.toMillis(syncIntervalMinutes));

        assertThat(eventLimiter, limitsEventTo(limitedScheduledEvent, BRANCH.getDefaultLimit() * syncIntervalMinutes));
    }

    @Test
    public void limitShouldNotBeHigherThanOneHoursWorthOfEventsForScheduledSync() throws Exception
    {
        final int syncIntervalMinutes = 120;
        when(syncConfig.scheduledSyncIntervalMillis()).thenReturn(MINUTES.toMillis(syncIntervalMinutes));

        // allow at most one hour of events per sync
        assertThat(eventLimiter, limitsEventTo(limitedScheduledEvent, BRANCH.getDefaultLimit() * 60));
    }

    @Test
    public void limiterShouldKeepTrackOfLimitExceededCount() throws Exception
    {
        final int branchLimit = BRANCH.getDefaultLimit();
        final int commitLimit = COMMIT.getDefaultLimit();
        final int extra = 2;
        final int tries = Math.max(branchLimit, commitLimit) + extra;

        // try 2 extra events of each type
        for (int i = 0; i < tries; i++) { eventLimiter.isLimitExceeded(limitedWebhookEvent); }
        for (int i = 0; i < tries; i++) { eventLimiter.isLimitExceeded(new LimitedTestEvent(COMMIT, false)); }

        assertThat(eventLimiter.getLimitExceededCount(), equalTo((tries - branchLimit) + (tries - commitLimit)));
    }

    /**
     * @return a matcher that returns true if the event limiter limits {@code limit} events
     */
    private TypeSafeMatcher<EventLimiter> limitsEventTo(final SyncEvent syncEvent, final int limit)
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
                    boolean limitExceeded = eventLimiter.isLimitExceeded(syncEvent);
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
