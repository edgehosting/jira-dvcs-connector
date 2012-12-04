package com.atlassian.jira.plugins.dvcs.service.remote;

public class BranchTip
{
    
    private final String branchName;
    private final String node;

    public String getBranchName()
    {
        return branchName;
    }

    public String getNode()
    {
        return node;
    }

    public BranchTip(String branchName, String node)
    {
        this.branchName = branchName;
        this.node = node;
    }

}
