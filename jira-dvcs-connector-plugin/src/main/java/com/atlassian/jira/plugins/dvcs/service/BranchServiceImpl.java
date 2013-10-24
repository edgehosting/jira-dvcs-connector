package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping;
import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.impl.IssueKeyExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BranchServiceImpl implements BranchService
{

    private final BranchDao branchDao;

    public BranchServiceImpl(BranchDao branchDao)
    {
        this.branchDao = branchDao;
    }

    @Override
    public void removeAllBranchesInRepository(int repositoryId)
    {
        branchDao.removeAllBranchesInRepository(repositoryId);
    }

    @Override
    public void removeAllBranchHeadsInRepository(int repositoryId)
    {
        branchDao.removeAllBranchHeadsInRepository(repositoryId);
    }

    @Override
    public void updateBranches(final Repository repository, final List<Branch> newBranches)
    {
        List<Branch> oldBranches = branchDao.getBranches(repository.getId());
        for (Branch branch : newBranches)
        {
            if (oldBranches == null || !oldBranches.contains(branch))
            {
                Set<String> issueKeys = IssueKeyExtractor.extractIssueKeys(branch.getName());
                branchDao.createBranch(repository.getId(), branch, issueKeys);
            }
        }

        // Removing closed branches
        if (oldBranches != null)
        {
            for (Branch oldBranch : oldBranches)
            {
                if (!newBranches.contains(oldBranch))
                {
                    branchDao.removeBranch(repository.getId(), oldBranch);
                }
            }
        }
    }

    @Override
    public List<BranchHead> getListOfBranchHeads(Repository repository)
    {
        List<BranchHead> branchHeads = null;
        branchHeads = branchDao.getBranchHeads(repository.getId());
        return branchHeads;
    }

    @Override
    public void updateBranchHeads(Repository repository, List<Branch> newBranches, List<BranchHead> oldBranchHeads)
    {
        if (newBranches != null)
        {
            List<BranchHead> headAlreadyThere = new ArrayList<BranchHead>();
            for (Branch branch : newBranches)
            {
                for (BranchHead branchHead : branch.getHeads())
                {
                    if (oldBranchHeads == null || !oldBranchHeads.contains(branchHead))
                    {
                        branchDao.createBranchHead(repository.getId(), branchHead);
                    } else
                    {
                        headAlreadyThere.add(branchHead);
                    }
                }
            }
            // Removing old branch heads
            if (oldBranchHeads != null)
            {
                for (BranchHead oldBranchHead : oldBranchHeads)
                {
                    if (!headAlreadyThere.contains(oldBranchHead))
                    {
                        branchDao.removeBranchHead(repository.getId(), oldBranchHead);
                    }
                }
            }
        }
    }
}
