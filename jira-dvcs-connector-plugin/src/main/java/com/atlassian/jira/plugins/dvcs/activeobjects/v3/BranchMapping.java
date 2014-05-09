package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Preload
@Table("Branch")
public interface BranchMapping extends Entity
{
    public static final String NAME = "NAME";
    public static final String REPOSITORY_ID = "REPOSITORY_ID";
    
    String getName();
    RepositoryMapping getRepository();

    void setName(String name);
    void setRepository(RepositoryMapping repository);
}
