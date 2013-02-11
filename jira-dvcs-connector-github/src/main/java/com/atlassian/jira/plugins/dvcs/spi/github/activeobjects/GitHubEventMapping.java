package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;

/**
 * AO representation of the {@link GitHubEvent}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubEvent")
public interface GitHubEventMapping extends Entity
{

    /**
     * @return {@link GitHubEvent#getGitHubId()}
     */
    @NotNull
    @Unique
    String getGitHubId();

    /**
     * @param gitHubId
     */
    void setGitHubId(String gitHubId);

    /**
     * @return {@link GitHubEvent#getRepository()}
     */
    @NotNull
    GitHubRepositoryMapping getRepository();

    /**
     * @param gitHubRepository
     *            {@link #getRepository()}
     */
    void setReository(GitHubRepositoryMapping gitHubRepository);

    /**
     * @return {@link GitHubEvent#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubEvent#isSavePoint()}
     */
    @NotNull
    boolean isSavePoint();

    /**
     * @param savePoint
     *            {@link #isSavePoint()}
     */
    void setSavePoint(boolean savePoint);

}
