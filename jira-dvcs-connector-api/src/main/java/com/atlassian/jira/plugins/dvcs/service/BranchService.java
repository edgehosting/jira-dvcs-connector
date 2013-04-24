package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface BranchService
{
    public List<BranchHead> getListOfBranchHeads(Repository repository, boolean softSync);
    
    public void updateBranchHeads(Repository repository, List<BranchHead> newBranchHeads, List<BranchHead> oldBranchHeads);
}
