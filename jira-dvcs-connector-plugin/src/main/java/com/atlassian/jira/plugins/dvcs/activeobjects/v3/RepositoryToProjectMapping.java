package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;


@Preload
@Table ("RepoToProject")
public interface RepositoryToProjectMapping extends Entity
{
    public static String PROJECT_KEY = "PROJECT_KEY";
    public static String REPOSITORY_ID = "REPOSITORY_ID";

    void setRepository(RepositoryMapping repo);

    void setProjectKey(String ProjectKey);

    String getProjectKey();

    @Indexed
    RepositoryMapping getRepository();

}
