package com.atlassian.jira.plugins.dvcs.model;

public class Branch
{
    private String branchName;
    private String node;

    public Branch(String branchName, String node)
    {
        this.branchName = branchName;
        this.node = node;
    }

    public String getBranchName()
    {
        return branchName;
    }

    public String getNode()
    {
        return node;
    }

    public void setBranchName(String branchName)
    {
        this.branchName = branchName;
    }

    public void setNode(String node)
    {
        this.node = node;
    }

}
