package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Branch;

public interface BranchService
{
    public List<Branch> getListOfBranches(int repositoryId);
    
    public void addBranch(int repositoryId, String name, String sha);

    public void saveBranches(List<Branch> branches);
}
