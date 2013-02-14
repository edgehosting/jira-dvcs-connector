package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;

/**
 * AO representation of the {@link GitHubRepository}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubRepository")
public interface GitHubRepositoryMapping extends Entity
{

    /**
     * @return {@link GitHubRepository#getGitHubId()}
     */
    @Unique
    @NotNull
    long getGitHubId();

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    void setGitHubId(long gitHubId);

    /**
     * @return {@link GitHubRepository#getName()}
     */
    String getName();

    /**
     * @param name
     *            {@link #getName()}
     */
    void setName(String name);

    /**
     * @return {@link GitHubRepository#getUrl()}
     */
    String getUrl();

    /**
     * @param url
     *            {@link #getUrl()}
     */
    void setUrl(String url);

}
