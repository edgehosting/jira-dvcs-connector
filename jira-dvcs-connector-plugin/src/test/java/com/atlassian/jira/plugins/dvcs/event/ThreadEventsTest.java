package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.beust.jcommander.internal.Lists;
import org.hamcrest.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import javax.annotation.Nonnull;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ThreadEventsTest
{
    ThreadEvents threadEvents;
    ThreadEventsCaptor threadEventsCaptor;
    CollectEventsClosure closure;

    @Mock
    ChangesetMapping changeset;

    @Mock
    RepositoryPullRequestMapping pullRequest;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        threadEvents = new ThreadEvents();

        threadEventsCaptor = threadEvents.startCapturing();
        closure = new CollectEventsClosure();
    }

    @AfterMethod
    public void tearDown() throws Exception
    {
        threadEventsCaptor.stopCapturing();
    }

    @Test
    public void processEachShouldProcessAllPublishedEvents() throws Exception
    {
        threadEvents.broadcast(changeset);
        threadEvents.broadcast(pullRequest);

        threadEventsCaptor.processEach(closure);
        assertThat(closure.events, Matchers.<Object>hasItems(changeset, pullRequest));
    }

    @Test
    public void processEachShouldFilterByClassType() throws Exception
    {
        threadEvents.broadcast(changeset);
        threadEvents.broadcast(pullRequest);

        CollectEventsClosure closure2 = new CollectEventsClosure();
        threadEventsCaptor.processEach(RepositoryPullRequestMapping.class, closure);
        threadEventsCaptor.processEach(closure2);

        assertThat("1st call to processEach should process PR event only", closure.events, Matchers.<Object>hasItems(pullRequest));
        assertThat("2nd call to processEach should process remaining events", closure2.events, Matchers.<Object>hasItems(changeset));
    }

    @Test
    public void listenersShouldNotReceiveEventsRaisedAfterTheyHaveStoppedListening() throws Exception
    {
        threadEventsCaptor.stopCapturing();
        threadEvents.broadcast(pullRequest);

        threadEventsCaptor.processEach(closure);
        assertThat(closure.events.size(), equalTo(0));
    }

    @Test
    public void listenersShouldNotGetEventsRaisedInOtherThreads() throws Exception
    {
        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                threadEvents.broadcast(changeset);
            }
        };
        thread.start();
        thread.join();

        threadEventsCaptor.processEach(closure);
        assertThat(closure.events.size(), equalTo(0));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void startingTwoCapturesOnTheSameThreadThrowsException() throws Exception
    {
        threadEvents.startCapturing();
    }

    private static class CollectEventsClosure implements ThreadEventsCaptor.Closure<Object>
    {
        final List<Object> events = Lists.newArrayList();

        @Override
        public void process(@Nonnull Object event)
        {
            events.add(event);
        }
    }
}
