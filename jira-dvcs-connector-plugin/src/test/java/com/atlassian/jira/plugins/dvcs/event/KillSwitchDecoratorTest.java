package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class KillSwitchDecoratorTest
{
    @Mock
    Repository repo1;

    @Mock
    EventsFeature eventsFeature;

    @Mock
    EventService eventService;

    @InjectMocks
    KillSwitchDecorator decorator;

    @BeforeMethod
    public void setUp() throws Exception
    {
        decorator = new KillSwitchDecorator(eventsFeature, eventService);
        when(eventsFeature.isEnabled()).thenReturn(false);
    }

    @Test
    public void storeRespectsKillSwitchOn() throws Exception
    {
        when(eventsFeature.isEnabled()).thenReturn(false);

        final TestEvent event = new TestEvent(new Date(0), "my-data");
        final boolean scheduled = true;
        decorator.storeEvent(repo1, event, scheduled);
        decorator.storeEvent(repo1, event);

        verifyZeroInteractions(eventService);

    }

    @Test
    public void storeRespectsKillSwitchOff() throws Exception
    {
        when(eventsFeature.isEnabled()).thenReturn(true);

        final TestEvent event = new TestEvent(new Date(0), "my-data");
        final boolean scheduled = true;

        decorator.storeEvent(repo1, event, scheduled);
        verify(eventService).storeEvent(repo1, event, scheduled);

        decorator.storeEvent(repo1, event);
        verify(eventService).storeEvent(repo1, event);
    }

    @Test
    public void dispatchRespectsKillSwitchOn() throws Exception
    {
        when(eventsFeature.isEnabled()).thenReturn(false);

        decorator.dispatchEvents(repo1);
        verifyZeroInteractions(eventService);
    }

    @Test
    public void dispatchRespectsKillSwitchOff() throws Exception
    {
        when(eventsFeature.isEnabled()).thenReturn(true);

        decorator.dispatchEvents(repo1);
        verify(eventService).dispatchEvents(repo1);
    }

    @Test
    public void discardRespectsKillSwitchOn() throws Exception
    {
        when(eventsFeature.isEnabled()).thenReturn(false);

        decorator.discardEvents(repo1);
        verifyZeroInteractions(eventService);
    }

    @Test
    public void discardRespectsKillSwitchOff() throws Exception
    {
        when(eventsFeature.isEnabled()).thenReturn(true);

        decorator.discardEvents(repo1);
        verify(eventService).discardEvents(repo1);
    }
}
