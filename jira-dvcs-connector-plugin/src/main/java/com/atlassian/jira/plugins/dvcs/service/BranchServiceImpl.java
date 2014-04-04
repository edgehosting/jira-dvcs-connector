package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.sync.impl.IssueKeyExtractor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;

public class BranchServiceImpl implements BranchService
{
    @Resource
    private BranchDao branchDao;

    @Resource
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

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
        // to remove possible branch duplicates
        Set<Branch> newBranchSet = new HashSet<Branch>(newBranches);

        List<Branch> oldBranches = branchDao.getBranches(repository.getId());
        removeDuplicatesIfNeeded(repository, oldBranches);

        for (Branch branch : newBranchSet)
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
                if (!newBranchSet.contains(oldBranch))
                {
                    branchDao.removeBranch(repository.getId(), oldBranch);
                }
                else
                {
                    // removing existing branch to remove all subsequent duplicate branches
                    newBranchSet.remove(oldBranch);
                }
            }
        }
    }

    private void removeDuplicatesIfNeeded(final Repository repository, final List<Branch> oldBranches)
    {
        Set<Branch> oldBranchesSet = new HashSet<Branch>(oldBranches);
        if (oldBranches.size() != oldBranchesSet.size())
        {
            Set<Branch> duplicates = findDuplicates(oldBranches, oldBranchesSet);

            for (Branch branch : duplicates)
            {
                branchDao.removeBranch(repository.getId(), branch);
            }
            oldBranches.removeAll(duplicates);
        }

    }

    private void removeDuplicateBranchHeadIfNeeded(final Repository repository, final List<BranchHead> oldBranchHeads)
    {
        Set<BranchHead> oldBranchHeadsSet = new HashSet<BranchHead>(oldBranchHeads);
        if (oldBranchHeads.size() != oldBranchHeadsSet.size())
        {
            Set<BranchHead> duplicates = findDuplicates(oldBranchHeads, oldBranchHeadsSet);

            for (BranchHead branchHead : duplicates)
            {
                branchDao.removeBranchHead(repository.getId(), branchHead);
            }
            oldBranchHeads.removeAll(duplicates);
        }
    }

    private <T> Set<T> findDuplicates(List<T> list, Set<T> set)
    {
        Set<T> duplicates = new HashSet<T>();

        // removing duplicates
        for (T element : list)
        {
            if (set.contains(element))
            {
                set.remove(element);
            }
            else
            {
                // we found duplicate
                duplicates.add(element);
            }
        }

        return duplicates;
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
            removeDuplicateBranchHeadIfNeeded(repository, oldBranchHeads);

            Set<BranchHead> headAlreadyThere = new HashSet<BranchHead>();
            for (Branch branch : new HashSet<Branch>(newBranches))
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

    @Override
    public List<Branch> getByIssueKey(Iterable<String> issueKeys)
    {
        return branchDao.getBranchesForIssue(issueKeys);
    }

    @Override
    public List<Branch> getByIssueKey(Iterable<String> issueKeys, String dvcsType)
    {
        return branchDao.getBranchesForIssue(issueKeys, dvcsType);
    }

    @Override
    public List<Branch> getForRepository(Repository repository)
    {
        return branchDao.getBranchesForRepository(repository.getId());
    }

    @Override
    public String getBranchUrl(Repository repository, Branch branch)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getBranchUrl(repository, branch);
    }
}
