package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.model.Organization;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Gets the accounts for one or more organisations.
 *
 * @since 1.4.1
 */
// a subset/partial service of {@link com.atlassian.jira.plugins.dvcs.service.OrganizationService},
// only exposing basic information that can be consumed externally.
@PublicApi
public interface DvcsLinkService
{
    /**
     * Finds an organization by its ID.
     *
     * @param organizationId ID of the organisation to find
     * @param loadRepositories whether to load the repositories associated with the organisation
     * @return the matching {@link Organization} or {@code null} if there is no match
     */
    Organization getDvcsLink(boolean loadRepositories, int organizationId);

    /**
     * Finds all the organizations.
     *
     * @param loadRepositories whether to load the repositories associated with each organisation
     * @return a list of {@link Organization}
     */
    List<Organization> getDvcsLinks(boolean loadRepositories);

    /**
     * Finds all the organizations matching a specific application (like BitBucket).
     *
     * @param loadRepositories whether to load the repositories associated with each organisation
     * @param applicationType type of application the organisation must belong to
     * @return a list of {@link Organization}
     */
    List<Organization> getDvcsLinks(boolean loadRepositories, @Nonnull String applicationType);
}
