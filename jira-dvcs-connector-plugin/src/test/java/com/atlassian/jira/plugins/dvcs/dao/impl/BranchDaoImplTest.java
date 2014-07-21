package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToBranchMapping;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import org.hamcrest.Matchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.isSelect;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.withWhereParamsThat;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.withWhereThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class BranchDaoImplTest
{

    @Mock
    ActiveObjects activeObjects;


    @InjectMocks
    BranchDaoImpl branchDao;

    @Test
    public void testDeleteSetsRepository()
    {

        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).then(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                return ((TransactionCallback) invocation.getArguments()[0]).doInTransaction();
            }
        });

        String branchName = "someBranch";
        branchDao.removeBranch(1, new Branch(branchName));

        verify(activeObjects).stream(eq(IssueToBranchMapping.class), argThat(Matchers.<Query>allOf(
                isSelect(),
                withWhereThat(containsString(BranchMapping.NAME)),
                withWhereThat(containsString(BranchMapping.REPOSITORY_ID)),
                withWhereParamsThat(Matchers.<Object>contains(branchName, 1))
        )), (EntityStreamCallback) anyObject());
    }
}
