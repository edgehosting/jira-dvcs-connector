package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Date;

import net.java.ao.Polymorphic;
import net.java.ao.schema.NotNull;

/**
 * Base class of all repositories activities.
 */
@Polymorphic
public interface RepositoryActivityMapping extends RepositoryDomainMapping
{

    String ENTITY_TYPE = "ENTITY_TYPE";

    String LAST_UPDATED_ON = "LAST_UPDATED_ON";
    String REPOSITORY_ID = "REPOSITORY_ID";
    String AUTHOR = "AUTHOR";
    String RAW_AUTHOR = "RAW_AUTHOR";

    @NotNull
    Date getLastUpdatedOn();

    @NotNull
    int getRepositoryId();

    @NotNull
    String getAuthor();

    String getRawAuthor();

    void setLastUpdatedOn(Date date);

    void setRepositoryId(int repositoryId);

    void setAuthor(String username);

    void setRawAuthor(String rawAuthor);

}
