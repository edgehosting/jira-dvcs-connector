package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;

/**
 * Gets the branches for one or more issue keys or repository from connected dvcs account
 *
 *
 */
public interface DvcsBranchService
{
    List<Branch> getBranches(Repository repository);

    List<Branch> getBranches(Iterable<String> issueKeys);
}
