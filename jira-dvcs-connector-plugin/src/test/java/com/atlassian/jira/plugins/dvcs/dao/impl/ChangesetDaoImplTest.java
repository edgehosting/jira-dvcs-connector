package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.ImmutableSet;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class ChangesetDaoImplTest
{
    final Set<String> issues = ImmutableSet.of("ISSUE-1");

    @Mock
    ActiveObjects activeObjects;

    @Mock
    QueryHelper queryHelper;

    @Mock
    Changeset changeset;

    @InjectMocks
    ChangesetDaoImpl changesetDao;

    @BeforeMethod
    public void setUp() throws Exception
    {
        when(changeset.getNode()).thenReturn("e4798f084a6cf7e9aff6e8d540414ef364042a40");
        when(changeset.getRawNode()).thenReturn("e4798f084a6cf7e9aff6e8d540414ef364042a40");
        when(changeset.getRepositoryId()).thenReturn(100);
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).then(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                return ((TransactionCallback) invocation.getArguments()[0]).doInTransaction();
            }
        });
        when(activeObjects.create(ChangesetMapping.class)).then(new Answer<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping answer(final InvocationOnMock invocation) throws Throwable
            {
                return createMapping();
            }
        });
    }

    @Test
    public void createOrAssociateShouldReturnTrueForNewChangeset() throws Exception
    {
        when(activeObjects.find(eq(ChangesetMapping.class), anyString(), anyString(), anyString(), anyString())).thenReturn(new ChangesetMapping[0]);

        boolean isNew = changesetDao.createOrAssociate(changeset, issues);
        assertThat(isNew, equalTo(true));
    }

    @Test
    public void createOrAssociateShouldReturnFalseForExistingChangeset() throws Exception
    {
        ChangesetMapping mapping = createMapping();

        when(mapping.getID()).thenReturn(1);
        when(activeObjects.find(eq(ChangesetMapping.class), anyString(), anyString(), anyString(), anyString())).thenReturn(new ChangesetMapping[] { mapping });

        boolean isNew = changesetDao.createOrAssociate(changeset, issues);
        assertThat(isNew, equalTo(false));
    }

    private ChangesetMapping createMapping()
    {
        ChangesetMapping mapping = mock(ChangesetMapping.class);
        when(mapping.<ChangesetMapping>getEntityType()).thenReturn(ChangesetMapping.class);

        return mapping;
    }
}
