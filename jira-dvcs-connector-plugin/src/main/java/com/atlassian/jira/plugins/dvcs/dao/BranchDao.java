package com.atlassian.jira.plugins.dvcs.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;

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
}
