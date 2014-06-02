package com.atlassian.jira.plugins.dvcs.service.remote;

import com.atlassian.cache.memory.MemoryCacheManager;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CachingCommunicatorTest
{
    @Mock private DvcsCommunicator mockDvcsCommunicator;
    @Mock private DvcsUser mockDvcsUser;
    @Mock private Repository mockRepository;
    private CachingCommunicator cachingCommunicator;

    @BeforeTest
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        cachingCommunicator = new CachingCommunicator(new MemoryCacheManager());
        cachingCommunicator.setDelegate(mockDvcsCommunicator);
    }

    @Test
    public void shouldGetUserByRepositoryAndUsername()
    {
        // Set up
        final String username = "bob";
        Mockito.when(mockDvcsCommunicator.getUser(mockRepository, username)).thenReturn(mockDvcsUser);

        // Invoke
        final DvcsUser user = cachingCommunicator.getUser(mockRepository, username);

        // Check
        assertEquals(mockDvcsUser, user);
    }
}
