package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("RepoToChangeset")
public interface RepositoryToChangesetMapping extends Entity
{

    public static final String REPOSITORY_ID = "REPOSITORY_ID";
    public static final String CHANGESET_ID = "CHANGESET_ID";


    RepositoryMapping getRepository();
    void setRepository(RepositoryMapping repository);

    ChangesetMapping getChangeset();
    void setChangeset(ChangesetMapping changeset);
}
