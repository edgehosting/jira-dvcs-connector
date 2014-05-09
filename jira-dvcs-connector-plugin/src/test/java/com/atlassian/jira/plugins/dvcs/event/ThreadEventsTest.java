package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ThreadEventsTest
{
    ThreadEvents threadEvents;
    ThreadEventsCaptor threadEventsCaptor;

    @Mock
    ChangesetMapping changeset;

    @Mock
    RepositoryPullRequestMapping pullRequest;

    @Mock
    EventPublisher eventPublisher;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        threadEvents = new ThreadEvents(eventPublisher);

        threadEventsCaptor = threadEvents.startCapturing();
    }

    @AfterMethod
    public void tearDown() throws Exception
    {
        threadEventsCaptor.stopCapturing();
    }

    @Test
    public void listenersShouldGetEventsAfterTheyArePublished() throws Exception
    {
        threadEvents.broadcast(changeset);
        threadEvents.broadcast(pullRequest);

        // no events have been published yet
        verifyZeroInteractions(eventPublisher);

        ArgumentCaptor<Object> publishedEvents = ArgumentCaptor.forClass(Object.class);
        threadEventsCaptor.sendToEventPublisher();
        verify(eventPublisher, times(2)).publish(publishedEvents.capture());

        assertThat(publishedEvents.getAllValues(), Matchers.<Object>hasItems(changeset, pullRequest));
    }

    @Test
    public void listenersShouldNotReceiveEventsRaisedAfterTheyHaveStoppedListening() throws Exception
    {
        threadEventsCaptor.stopCapturing();
        threadEvents.broadcast(pullRequest);

        threadEventsCaptor.sendToEventPublisher();
        verifyZeroInteractions(eventPublisher);
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

        threadEventsCaptor.sendToEventPublisher();
        verifyZeroInteractions(eventPublisher);
    }
}
