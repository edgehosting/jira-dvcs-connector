package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@ExportAsService (DvcsBranchService.class)
@Component
public class DvcsBranchServiceImpl implements DvcsBranchService
{
    private BranchService branchService;

    @Autowired
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
    public List<Branch> getBranches(Iterable<String> issueKeys, String dvcsType)
    {
        return ImmutableList.copyOf(branchService.getByIssueKey(issueKeys, dvcsType));
    }

    @Override
    public String getBranchUrl(Repository repository, Branch branch)
    {
        return branchService.getBranchUrl(repository, branch);
    }
}
