package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import net.java.ao.Entity;
import net.java.ao.Polymorphic;
import net.java.ao.schema.NotNull;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEntity;

/**
 * AO mapping of the {@link GitHubEntity}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Polymorphic
public interface GitHubEntityMapping extends Entity
{

    /**
     * @return {@link GitHubEntity#getRepository()}
     */
    @NotNull
    GitHubRepositoryMapping getRepository();

    /**
     * @param repository
     *            {@link #getRepository()}
     */
    void setRepository(GitHubRepositoryMapping repository);

}
