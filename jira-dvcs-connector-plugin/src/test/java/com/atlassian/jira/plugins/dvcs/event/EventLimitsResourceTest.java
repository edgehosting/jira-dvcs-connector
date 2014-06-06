package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ImmutableMap;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;
import javax.ws.rs.WebApplicationException;

import static com.atlassian.jira.plugins.dvcs.event.EventLimit.BRANCH;
import static com.atlassian.jira.plugins.dvcs.event.EventLimit.COMMIT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.testng.Assert.fail;

@Listeners (MockitoTestNgListener.class)
public class EventLimitsResourceTest
{
    static final int COMMIT_OVERRIDE = 3;

    final Map<EventLimit, Integer> limitOverrides = Maps.newHashMap();

    @Mock
    SyncConfig syncConfig;

    @InjectMocks
    EventLimitsResource eventLimitsResource;

    @BeforeMethod
    public void setUp() throws Exception
    {
        // set up a mock EventLimiter that behaves like the real thing
        limitOverrides.put(COMMIT, COMMIT_OVERRIDE);
        doAnswer(new GetLimitAnswer()).when(syncConfig).getEffectiveLimit(any(EventLimit.class));
        doAnswer(new SetLimitAnswer()).when(syncConfig).setEffectiveLimit(any(EventLimit.class), any(Integer.class));
    }

    @Test
    public void getReturnsEffectiveLimits() throws Exception
    {
        Map<String, Integer> all = eventLimitsResource.getAll();
        assertThat(all, equalTo((Map) ImmutableMap.of(
                BRANCH.name(), BRANCH.getDefaultLimit(),
                COMMIT.name(), COMMIT_OVERRIDE
        )));
    }

    @Test
    public void setRejectsBadLimitName() throws Exception
    {
        final String noSuchLimit = "no_such_limit";
        try
        {
            eventLimitsResource.put(ImmutableMap.of(noSuchLimit, 123));
            fail("set should not allow setting limit: " + noSuchLimit);
        }
        catch (WebApplicationException e)
        {
            assertThat(e.getResponse().getStatus(), equalTo(400));
        }
    }

    @Test
    public void setOverridesDefaultLimits() throws Exception
    {
        final int newLimit = 123;
        Map<String, Integer> branchUpdated = eventLimitsResource.put(ImmutableMap.of(BRANCH.name(), newLimit));
        assertThat("put should set override to " + newLimit, limitOverrides.get(BRANCH), equalTo(newLimit));
        assertThat(branchUpdated, equalTo((Map) ImmutableMap.of(
                BRANCH.name(), newLimit,
                COMMIT.name(), COMMIT_OVERRIDE
        )));

        Map<String, Integer> commitRemoved = eventLimitsResource.put(Collections.<String, Integer>singletonMap(COMMIT.name(), null));
        assertThat("passing null should remove override", commitRemoved, equalTo((Map) ImmutableMap.of(
                BRANCH.name(), newLimit,
                COMMIT.name(), COMMIT.getDefaultLimit()
        )));
    }

    private class GetLimitAnswer implements Answer<Integer>
    {
        @Override
        @SuppressWarnings ("SuspiciousMethodCalls")
        public Integer answer(InvocationOnMock invocation) throws Throwable
        {
            EventLimit eventLimit = (EventLimit) invocation.getArguments()[0];
            if (limitOverrides.containsKey(eventLimit))
            {
                return limitOverrides.get(eventLimit);
            }

            return eventLimit.getDefaultLimit();
        }
    }

    private class SetLimitAnswer implements Answer<Void>
    {
        @Override
        @SuppressWarnings ("SuspiciousMethodCalls")
        public Void answer(InvocationOnMock invocation) throws Throwable
        {
            EventLimit eventLimit = (EventLimit) invocation.getArguments()[0];
            Integer newLimit = (Integer) invocation.getArguments()[1];

            if (newLimit == null)
            {
                limitOverrides.remove(eventLimit);
            }
            else
            {
                limitOverrides.put(eventLimit, newLimit);
            }

            return null;
        }
    }
}
