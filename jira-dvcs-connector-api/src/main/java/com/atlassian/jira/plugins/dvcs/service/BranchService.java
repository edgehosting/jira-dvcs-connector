package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;

public interface BranchService
{
    public List<BranchHead> getListOfBranchHeads(Repository repository);

    public void removeAllBranchHeadsInRepository(int repositoryId);

    void updateBranches(Repository repository, List<Branch> newBranches);

    void removeAllBranchesInRepository(int repositoryId);

    void updateBranchHeads(Repository repository, List<Branch> newBranches, List<BranchHead> oldBranchHeads);

    List<Branch> getByIssueKey(Iterable<String> issueKeys);

    List<Branch> getForRepository(Repository repository);

    String getBranchUrl(Repository repository, Branch branch);

    List<Branch> getByIssueKey(Iterable<String> issueKeys, String dvcsType);
}
