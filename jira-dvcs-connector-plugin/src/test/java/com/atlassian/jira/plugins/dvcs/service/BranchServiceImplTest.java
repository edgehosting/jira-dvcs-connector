package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.collect.Lists;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class BranchServiceImplTest
{
    @Mock
    private BranchDao branchDao;

    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Mock
    private Repository repository;

    @InjectMocks
    private BranchServiceImpl branchService = new BranchServiceImpl();

    @Captor
    private ArgumentCaptor<Branch> branchArgumentCaptor;

    @Captor
    private ArgumentCaptor<Branch> removeBranchArgumentCaptor;

    @Captor
    private ArgumentCaptor<BranchHead> branchHeadArgumentCaptor;

    @Captor
    private ArgumentCaptor<BranchHead> removeBranchHeadArgumentCaptor;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDuplicateNewBranches()
    {
        List<Branch> newBranches = Lists.newArrayList(
                new Branch(0, "branch", repository.getId()),
                new Branch(0, "branch", repository.getId())
        );

        when(branchDao.getBranches(repository.getId())).thenReturn(new ArrayList<Branch>());

        branchService.updateBranches(repository, newBranches);

        verify(branchDao).createBranch(eq(repository.getId()), branchArgumentCaptor.capture(), anySetOf(String.class));

        assertEquals(branchArgumentCaptor.getValue().getName(), "branch");
    }

    @Test
    public void testDuplicateOldBranches()
    {
        List<Branch> newBranches = Lists.newArrayList(
                new Branch(0, "branch", repository.getId())
        );

        List<Branch> oldBranches = Lists.newArrayList(
                new Branch(1, "branch", repository.getId()),
                new Branch(2, "branch", repository.getId())
        );

        when(branchDao.getBranches(repository.getId())).thenReturn(oldBranches);

        branchService.updateBranches(repository, newBranches);

        verify(branchDao).removeBranch(eq(repository.getId()), removeBranchArgumentCaptor.capture());
        verify(branchDao).createBranch(eq(repository.getId()), branchArgumentCaptor.capture(), anySetOf(String.class));

        assertEquals(removeBranchArgumentCaptor.getValue().getName(), "branch");
        assertEquals(branchArgumentCaptor.getValue().getName(), "branch");
    }

    @Test
    public void testDuplicateNewBranchHeads()
    {
        List<Branch> newBranches = Lists.newArrayList(
                createBranchWithHead("branch", "node"),
                createBranchWithHead("branch", "node")
        );

        branchService.updateBranchHeads(repository, newBranches, new ArrayList<BranchHead>());

        verify(branchDao).createBranchHead(eq(repository.getId()), branchHeadArgumentCaptor.capture());

        assertEquals(branchHeadArgumentCaptor.getValue().getName(), "branch");
        assertEquals(branchHeadArgumentCaptor.getValue().getHead(), "node");
    }

    @Test
    public void testDuplicateOldBranchHeads()
    {
        List<Branch> newBranches = Lists.newArrayList(
                createBranchWithHead("branch", "node")
        );

        List<BranchHead> oldBranchHeads = Lists.newArrayList(
                new BranchHead("branch", "node"),
                new BranchHead("branch", "node")
        );

        branchService.updateBranchHeads(repository, newBranches, oldBranchHeads);

        verify(branchDao).removeBranchHead(eq(repository.getId()), removeBranchHeadArgumentCaptor.capture());
        verify(branchDao).createBranchHead(eq(repository.getId()), branchHeadArgumentCaptor.capture());

        assertEquals(removeBranchHeadArgumentCaptor.getValue().getName(), "branch");
        assertEquals(removeBranchHeadArgumentCaptor.getValue().getHead(), "node");
        assertEquals(branchHeadArgumentCaptor.getValue().getName(), "branch");
        assertEquals(branchHeadArgumentCaptor.getValue().getHead(), "node");
    }

    private Branch createBranchWithHead(String name, String node)
    {
        Branch branch = new Branch(0, "branch", repository.getId());
        branch.setHeads(Lists.newArrayList(new BranchHead(name, node)));
        return branch;
    }
}
