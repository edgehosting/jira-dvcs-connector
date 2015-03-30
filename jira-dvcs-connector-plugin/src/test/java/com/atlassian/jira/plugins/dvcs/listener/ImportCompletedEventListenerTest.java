package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.fugue.Option;
import com.atlassian.jira.bc.dataimport.ImportCompletedEvent;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith (MockitoJUnitRunner.class)
public class ImportCompletedEventListenerTest
{
    @Mock
    private OrganizationDao organizationDao;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ImportCompletedEventListener listener;

    @Before
    public void setUp() throws Exception
    {
    }

    @Test
    public void testRegisterListener() throws Exception
    {
        listener.registerListener();
        verify(eventPublisher).register(listener);
    }

    @Test
    public void testUnregisterListener() throws Exception
    {
        listener.unregisterListener();
        verify(eventPublisher).unregister(listener);
    }

    @Test
    public void testOnImportCompleted() throws Exception
    {
        listener.onImportCompleted(new ImportCompletedEvent(true, Option.none(Long.class)));
        verify(organizationDao).clearCache();
    }
}