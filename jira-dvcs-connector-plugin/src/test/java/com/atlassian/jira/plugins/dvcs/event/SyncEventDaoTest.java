package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.dao.ao.EntityBeanGenerator;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@Listeners
public class SyncEventDaoTest
{
    @Mock
    ActiveObjects activeObjects;

    @InjectMocks
    SyncEventDao dao;

    @BeforeMethod
    public void setUp() throws Exception
    {
        dao = new SyncEventDao(activeObjects, new EntityBeanGenerator());
    }

    @Test
    public void createShouldReturnANewSyncEventMapping() throws Exception
    {
        assertThat(dao.create(), notNullValue());
    }
}
