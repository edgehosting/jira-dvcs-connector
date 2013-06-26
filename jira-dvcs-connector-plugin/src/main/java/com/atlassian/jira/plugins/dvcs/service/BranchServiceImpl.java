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
    public void removeAllBranchHeadsInRepository(int repositoryId)
    {
        branchDao.removeAllBranchHeadsInRepository(repositoryId);
    }

    @Override
    public List<BranchHead> getListOfBranchHeads(Repository repository)
    {
        List<BranchHead> branchHeads = null;
        branchHeads = branchDao.getBranchHeads(repository.getId());
        return branchHeads;
    }

    @Override
    public void updateBranchHeads(Repository repository, List<BranchHead> newBranchHeads, List<BranchHead> oldBranchHeads)
    {
        if (newBranchHeads != null)
        {
            for (BranchHead branchHead : newBranchHeads)
            {
                if (oldBranchHeads == null || !oldBranchHeads.contains(branchHead))
                {
                    branchDao.createBranchHead(repository.getId(), branchHead);
                }
            }

            // Removing old branch heads
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
}
