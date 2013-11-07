package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class DvcsBranchServiceImpl implements DvcsBranchService
{
    private BranchService branchService;

    public DvcsBranchServiceImpl(BranchService branchService)
    {
        this.branchService = branchService;
    }

    @Override
    public List<Branch> getBranches(Repository repository)
    {
        return ImmutableList.copyOf(branchService.getForRepository(repository));
    }

    @Override
    public List<Branch> getBranches(Iterable<String> issueKeys)
    {
        return ImmutableList.copyOf(branchService.getByIssueKey(issueKeys));
    }

    @Override
    public String getBranchUrl(Repository repository, Branch branch)
    {
        return branchService.getBranchUrl(repository, branch);
    }
}
