package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.event.BranchCreatedEvent;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class BranchServiceImplTest
{
    @Mock
    private BranchDao branchDao;

    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Mock
    private Repository repository;

    @Mock
    private ThreadEvents threadEvents;

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
    public void testRemovingOldBranches()
    {
        List<Branch> newBranches = Lists.newArrayList(
                new Branch(0, "branch3", repository.getId())
        );

        List<Branch> oldBranches = Lists.newArrayList(
                new Branch(1, "branch1", repository.getId()),
                new Branch(2, "branch2", repository.getId())
        );

        when(branchDao.getBranches(repository.getId())).thenReturn(oldBranches);

        branchService.updateBranches(repository, newBranches);

        verify(branchDao, times(2)).removeBranch(eq(repository.getId()), removeBranchArgumentCaptor.capture());
        verify(branchDao).createBranch(eq(repository.getId()), branchArgumentCaptor.capture(), anySetOf(String.class));

        assertThat(removeBranchArgumentCaptor.getAllValues(), Matchers.containsInAnyOrder(oldBranches.toArray(new Branch[] { })));
        assertEquals(branchArgumentCaptor.getValue().getName(), "branch3");
    }

    @Test
    public void testUpdateBranches()
    {
        List<Branch> newBranches = Lists.newArrayList(
                new Branch(0, "branch1", repository.getId()),
                new Branch(0, "branch3", repository.getId())
        );

        List<Branch> oldBranches = Lists.newArrayList(
                new Branch(1, "branch1", repository.getId()),
                new Branch(2, "branch2", repository.getId())
        );

        when(branchDao.getBranches(repository.getId())).thenReturn(oldBranches);

        branchService.updateBranches(repository, newBranches);

        verify(branchDao).removeBranch(eq(repository.getId()), removeBranchArgumentCaptor.capture());
        verify(branchDao).createBranch(eq(repository.getId()), branchArgumentCaptor.capture(), anySetOf(String.class));

        assertEquals(removeBranchArgumentCaptor.getValue().getName(), "branch2");
        assertEquals(branchArgumentCaptor.getValue().getName(), "branch3");
    }

    @Test
    public void testUpdateBranchesShouldBroadcastEvent()
    {
        List<Branch> newBranches = Lists.newArrayList(
                new Branch(0, "branch3", repository.getId())
        );

        when(branchDao.getBranches(repository.getId())).thenReturn(new ArrayList<Branch>());
        branchService.updateBranches(repository, newBranches);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(threadEvents).broadcast(eventCaptor.capture());

        assertThat(eventCaptor.getValue(), instanceOf(BranchCreatedEvent.class));
        BranchCreatedEvent event = (BranchCreatedEvent) eventCaptor.getValue();
        assertThat(event.getBranch().getName(), equalTo("branch3"));
    }

    @Test
    public void testUpdateBranchesWithNullName()
    {
        List<Branch> newBranches = Lists.newArrayList(
                new Branch(0, "branch1", repository.getId())
        );

        List<Branch> oldBranches = Lists.newArrayList(
                new Branch(1, null, repository.getId())
        );

        when(branchDao.getBranches(repository.getId())).thenReturn(oldBranches);

        branchService.updateBranches(repository, newBranches);

        verify(branchDao).removeBranch(eq(repository.getId()), removeBranchArgumentCaptor.capture());
        verify(branchDao).createBranch(eq(repository.getId()), branchArgumentCaptor.capture(), anySetOf(String.class));

        assertNull(removeBranchArgumentCaptor.getValue().getName());
        assertEquals(branchArgumentCaptor.getValue().getName(), "branch1");
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

    @Test
    public void testRemovingOldBranchHeads()
    {
        List<Branch> newBranches = Lists.newArrayList(
                createBranchWithHead("branch", "node3")
        );

        List<BranchHead> oldBranchHeads = Lists.newArrayList(
                new BranchHead("branch", "node1"),
                new BranchHead("branch", "node2")
        );

        branchService.updateBranchHeads(repository, newBranches, oldBranchHeads);

        verify(branchDao, times(2)).removeBranchHead(eq(repository.getId()), removeBranchHeadArgumentCaptor.capture());
        verify(branchDao).createBranchHead(eq(repository.getId()), branchHeadArgumentCaptor.capture());

        assertThat(removeBranchHeadArgumentCaptor.getAllValues(), Matchers.containsInAnyOrder(oldBranchHeads.toArray(new BranchHead[] { })));
        assertEquals(branchHeadArgumentCaptor.getValue().getName(), "branch");
        assertEquals(branchHeadArgumentCaptor.getValue().getHead(), "node3");
    }

    @Test
    public void testUpdateBranchHeads()
    {
        List<Branch> newBranches = Lists.newArrayList(
                createBranchWithHead("branch", "node1", "node3")
        );

        List<BranchHead> oldBranchHeads = Lists.newArrayList(
                new BranchHead("branch", "node1"),
                new BranchHead("branch", "node2")
        );

        branchService.updateBranchHeads(repository, newBranches, oldBranchHeads);

        verify(branchDao).removeBranchHead(eq(repository.getId()), removeBranchHeadArgumentCaptor.capture());
        verify(branchDao).createBranchHead(eq(repository.getId()), branchHeadArgumentCaptor.capture());

        assertEquals(removeBranchHeadArgumentCaptor.getValue().getName(), "branch");
        assertEquals(removeBranchHeadArgumentCaptor.getValue().getHead(), "node2");
        assertEquals(branchHeadArgumentCaptor.getValue().getName(), "branch");
        assertEquals(branchHeadArgumentCaptor.getValue().getHead(), "node3");
    }

    @Test
    public void testUpdateBranchHeadWithNullHead()
    {
        List<Branch> newBranches = Lists.newArrayList(
                createBranchWithHead("branch", "node1")
        );

        List<BranchHead> oldBranchHeads = Lists.newArrayList(
                new BranchHead("branch", null)
        );

        branchService.updateBranchHeads(repository, newBranches, oldBranchHeads);

        verify(branchDao).removeBranchHead(eq(repository.getId()), removeBranchHeadArgumentCaptor.capture());
        verify(branchDao).createBranchHead(eq(repository.getId()), branchHeadArgumentCaptor.capture());

        assertEquals(removeBranchHeadArgumentCaptor.getValue().getName(), "branch");
        assertNull(removeBranchHeadArgumentCaptor.getValue().getHead());
        assertEquals(branchHeadArgumentCaptor.getValue().getName(), "branch");
        assertEquals(branchHeadArgumentCaptor.getValue().getHead(), "node1");
    }

    private Branch createBranchWithHead(String name, String... nodes)
    {
        Branch branch = new Branch(0, "branch", repository.getId());
        List<BranchHead> branchHeads = new ArrayList<BranchHead>();
        for (String node : nodes)
        {
            branchHeads.add(new BranchHead(name, node));
        }
        branch.setHeads(branchHeads);
        return branch;
    }
}
