package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BranchCreatedTransformer
{
    public static final Logger log = LoggerFactory.getLogger(BranchCreatedTransformer.class);

    public Branch transform(BranchMapping branchMapping)
    {
        if (branchMapping == null)
        {
            return null;
        }

        Branch branch = new Branch(branchMapping.getName());
        branch.setRepositoryId(branchMapping.getRepository().getID());
        branch.setId(branchMapping.getID());

        // todo: get branch heads?
        branch.setHeads(null);
        return branch;
    }
}
