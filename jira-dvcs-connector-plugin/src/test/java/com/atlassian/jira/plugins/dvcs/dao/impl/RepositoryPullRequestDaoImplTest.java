package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.event.impl.RepositoryPullRequestMappingCreated;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.beust.jcommander.internal.Maps;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryPullRequestDaoImplTest
{
    @Mock
    ActiveObjects activeObjects;

    @Mock
    ThreadEvents threadEvents;

    @InjectMocks
    RepositoryPullRequestDaoImpl repositoryPullRequestDao;

    @Mock
    Repository repository;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void savePullRequestRaisesEvent() throws Exception
    {
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).thenReturn(mock(RepositoryPullRequestMapping.class));

        repositoryPullRequestDao.savePullRequest(repository, Maps.<String, Object>newHashMap());
        verify(threadEvents).broadcast(argThat(instanceOf(RepositoryPullRequestMappingCreated.class)));
    }
}
