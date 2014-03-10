package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;

import java.util.List;
import java.util.Set;

/**
 *
 */
public interface BranchDao
{
    /**
     * Saves new branch head for given repository
     *
     * @param repositoryId
     * @param branchHead
     */
    void createBranchHead(int repositoryId, BranchHead branchHead);

    /**
     * Returns list of branch heads for repository
     *
     * @param repositoryId
     */
    List<BranchHead> getBranchHeads(int repositoryId);

    /**
     * Removes branch head for given repository
     *
     * @param repositoryId
     * @param branchHead
     */
    void removeBranchHead(int repositoryId, BranchHead branchHead);

    /**
     * Removes branch heads for given repository
     *
     * @param repositoryId
     */
    void removeAllBranchHeadsInRepository(int repositoryId);

    /**
     * Returns branches for the issue
     *
     * @param issueKeys
     * @return
     */
    List<Branch> getBranchesForIssue(Iterable<String> issueKeys);

    List<Branch> getBranches(int repositoryId);

    void createBranch(int repositoryId, Branch branch, Set<String> issueKeys);

    void removeBranch(int repositoryId, Branch branch);

    void removeAllBranchesInRepository(int repositoryId);

    List<Branch> getBranchesForRepository(int repositoryId);

    List<Branch> getBranchesForIssue(Iterable<String> issueKeys, String dvcsType);
}
