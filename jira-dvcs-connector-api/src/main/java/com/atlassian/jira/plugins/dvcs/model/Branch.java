package com.atlassian.jira.plugins.dvcs.model;

import java.util.List;

public class Branch
{
    private int id;
    private String name;
    private List<BranchHead> heads;
    private int repositoryId;

    public Branch()
    {
    }

    public Branch(final String name)
    {
        this.name = name;
    }

    public Branch(final int id, final String name, final int repositoryId)
    {
        this.id = id;
        this.name = name;
        this.repositoryId = repositoryId;
    }

    public int getId()
    {
        return id;
    }

    public void setId(final int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public List<BranchHead> getHeads()
    {
        return heads;
    }

    public void setHeads(final List<BranchHead> heads)
    {
        this.heads = heads;
    }

    public int getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(final int repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        final Branch branch = (Branch) o;

        if (repositoryId != branch.repositoryId) { return false; }
        if (!name.equals(branch.name)) { return false; }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + repositoryId;
        return result;
    }

    @Override
    public String toString()
    {
        return "Branch{" +
                "name='" + name + '\'' +
                ", repositoryId=" + repositoryId +
                '}';
    }
}
