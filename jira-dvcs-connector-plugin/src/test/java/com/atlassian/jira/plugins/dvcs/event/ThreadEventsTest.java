package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.google.common.collect.ImmutableList;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ThreadEventsTest
{
    ThreadEvents threadEvents;
    ThreadEventsCapture threadEventsCapture;

    @Mock
    ChangesetMapping changeset;

    @Mock
    RepositoryPullRequestMapping pullRequest;

    ChangesetTestListener changesetListener;
    PullRequestTestListener pullRequestListener;
    AnythingTestListener anythingListener;
    ImmutableList<TestListener<?>> listeners;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        threadEvents = new ThreadEvents();

        threadEventsCapture = threadEvents.startCapturingEvents();
        listeners = ImmutableList.of(
                anythingListener = new AnythingTestListener(),
                changesetListener = new ChangesetTestListener(),
                pullRequestListener = new PullRequestTestListener()
        );
    }

    @AfterMethod
    public void tearDown() throws Exception
    {
        threadEventsCapture.stopCapturing();
    }

    @Test
    public void listenersShouldGetEventsAfterTheyArePublished() throws Exception
    {
        threadEvents.broadcast(changeset);
        threadEvents.broadcast(pullRequest);

        // no events have been published yet
        assertThat(changesetListener.created, equalTo(EMPTY_LIST));
        assertThat(pullRequestListener.created, equalTo(EMPTY_LIST));
        assertThat(anythingListener.created, equalTo(EMPTY_LIST));

        threadEventsCapture.publishTo(listeners);

        assertThat(changesetListener.created, equalTo(singletonList(changeset)));
        assertThat(pullRequestListener.created, equalTo(singletonList(pullRequest)));

        final List<Object> bothEvents = ImmutableList.<Object>of(changeset, pullRequest);
        assertThat(anythingListener.created, equalTo(bothEvents));
    }

    @Test
    public void listenersShouldNotReceiveEventsRaisedAfterTheyHaveStoppedListening() throws Exception
    {
        threadEventsCapture.stopCapturing();
        threadEvents.broadcast(pullRequest);
        threadEventsCapture.publishTo(listeners);

        assertThat(pullRequestListener.created, equalTo(EMPTY_LIST));
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

        threadEventsCapture.publishTo(listeners);
        assertThat(changesetListener.created, equalTo(Collections.<ChangesetMapping>emptyList()));
    }


}
