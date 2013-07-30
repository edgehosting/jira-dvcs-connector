package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.model.Organization;

/**
 * A subset/partial service of {@link com.atlassian.jira.plugins.dvcs.service.OrganizationService}, only exposing basic information that can be
 * consumed externally
 *
 * @since v6.1
 */
@PublicApi
public interface DvcsLinkService
{
    /**
     * returns Organization by ID.
     *
     * @param organizationId id
     * @param loadRepositories the load repositories
     * @return Organization
     */
    Organization getDvcsLink(boolean loadRepositories, int organizationId);

    /**
     * returns all organizations.
     *
     * @param loadRepositories the load repositories
     * @return list of Organization
     */
    Iterable<Organization> getDvcsLinks(boolean loadRepositories);

    /**
     * returns all organizations.
     *
     * @param loadRepositories the load repositories
     * @param applicationType type of dvcs application to get
     * @return list of Organization
     */
    Iterable<Organization> getDvcsLinks(boolean loadRepositories, String applicationType);
}
