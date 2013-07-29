package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.annotations.PublicApi;

/**
 * A subset/partial service of {@link OrganizationService}, only exposing basic information that can be
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
     * @return DvcsLink
     */
    DvcsLink getDvcsLink(boolean loadRepositories, int organizationId);

    /**
     * returns all organizations.
     *
     * @param loadRepositories the load repositories
     * @return list of DvcsLink
     */
    Iterable<DvcsLink> getDvcsLinks(boolean loadRepositories);

    /**
     * returns all organizations.
     *
     * @param loadRepositories the load repositories
     * @param applicationType type of dvcs application to get
     * @return list of DvcsLink
     */
    Iterable<DvcsLink> getDvcsLinks(boolean loadRepositories, String applicationType);
}
