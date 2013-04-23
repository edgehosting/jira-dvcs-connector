package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("BranchMapping")
public interface BranchMapping extends Entity
{
    public static final String REPOSITORY_ID = "REPOSITORY_ID";
    public static final String NODE = "NODE";
    public static final String NAME = "NAME";
    
    int getRepositoryId();
    String getNode();
    String getName();

    void setRepositoryId(int repositoryId);
    void setNode(String node);
    void setName(String name);
}
