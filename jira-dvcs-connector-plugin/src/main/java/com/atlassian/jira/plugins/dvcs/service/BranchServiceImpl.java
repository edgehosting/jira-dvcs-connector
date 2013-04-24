package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;

public class BranchServiceImpl implements BranchService
{
    
    private final BranchDao branchDao;
    
    public BranchServiceImpl(BranchDao branchDao)
    {
        this.branchDao = branchDao;
    }

    @Override
    public List<BranchHead> getListOfBranchHeads(Repository repository, boolean softSync)
    {
        List<BranchHead> branchHeads = null;
        
        if (softSync)
        {
            branchHeads = branchDao.getBranchHeads(repository.getId());
        } else
        {
            branchDao.removeAllBranchHeadsInRepository(repository.getId());
        }
        return branchHeads;
    }

    @Override
    public void updateBranchHeads(Repository repository, List<BranchHead> newBranchHeads, List<BranchHead> oldBranchHeads)
    {
        for (BranchHead branchHead : newBranchHeads)
        {
            branchDao.saveOrUpdateBranchHead(repository.getId(), branchHead);
        }
        
        if (oldBranchHeads != null)
        {
            for (BranchHead oldBranchHead : oldBranchHeads)
            {
                if (!newBranchHeads.contains(oldBranchHead))
                {
                    branchDao.removeBranchHead(repository.getId(), oldBranchHead);
                }
            }
        }
    }
}
