package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("BranchHeadMapping")
public interface BranchHeadMapping extends Entity
{
    public static final String REPOSITORY = "REPOSITORY";
    public static final String REPOSITORY_ID = "REPOSITORY_ID";
    public static final String HEAD = "HEAD";
    public static final String BRANCH_NAME = "BRANCH_NAME";
    
    RepositoryMapping getRepository();
    String getHead();
    String getBranchName();

    void setRepository(RepositoryMapping repository);
    void setHead(String node);
    void setBranchName(String name);
}
