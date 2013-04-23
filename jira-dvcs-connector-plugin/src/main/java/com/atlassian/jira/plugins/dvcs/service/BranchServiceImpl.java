package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Branch;

public class BranchServiceImpl implements BranchService
{
    
    private final BranchDao branchDao;
    
    public BranchServiceImpl(BranchDao branchDao)
    {
        this.branchDao = branchDao;
    }

    @Override
    public List<Branch> getListOfBranches(int repositoryId)
    {
        return branchDao.getForRepository(repositoryId);
    }

    @Override
    public void addBranch(int repositoryId, String name, String sha)
    {
        
    }

    @Override
    public void saveBranches(List<Branch> branches)
    {
        // TODO Auto-generated method stub
        
    }

}
