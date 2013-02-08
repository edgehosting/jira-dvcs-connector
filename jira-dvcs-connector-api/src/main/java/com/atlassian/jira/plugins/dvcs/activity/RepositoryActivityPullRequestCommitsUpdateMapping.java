package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Date;

import net.java.ao.schema.Table;

/**
 * Update done via commits.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("PrCommitsUpdate")
public interface RepositoryActivityPullRequestCommitsUpdateMapping extends RepositoryActivityPullRequestMapping
{

    /**
     * @return Date of update.
     */
    Date getCreatedAt();

    /**
     * Author(username) of the update.
     */
    String getAuthor();

    /**
     * @see #getAuthor()
     */
    void setAuthor(String author);

    /**
     * @return Appropriate commits.
     */
    RepositoryActivityCommitMapping[] getCommits();

}
