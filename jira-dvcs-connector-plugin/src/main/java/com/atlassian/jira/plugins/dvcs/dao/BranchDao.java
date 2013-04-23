package com.atlassian.jira.plugins.dvcs.dao;

import java.util.List;
import com.atlassian.jira.plugins.dvcs.model.Branch;

/**
 *
 */
public interface BranchDao
{
    /**
     * Saves list of branches for given repository
     * 
     * @param repositoryId
     * @param branch
     */
    void saveBranch(int repositoryId, Branch branch);
    
    /**
     * Returns list of branches for repository
     * 
     * @param repositoryId
     */
    List<Branch> getForRepository(int repositoryId);

    /**
     * Removes branches for given repository
     * 
     * @param repositoryId
     */
    void removeBranches(int repositoryId);
}
