package com.atlassian.jira.plugins.dvcs.spi.github.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Provides GitHub related services.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public interface GitHubRepositoryService
{

    /**
     * Synchronize.
     * 
     * @param repository
     *            which will be synchronized
     */
    void synchronize(Repository repository);

}
