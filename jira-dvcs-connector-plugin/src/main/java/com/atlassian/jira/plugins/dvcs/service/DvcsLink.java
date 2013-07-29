package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Organization;

/**
 * An externally exposed class that represent a dvcs link
 *
 * @since v6.1
 */
public class DvcsLink extends Organization
{
    public DvcsLink(Organization organization)
    {
        super(
            organization.getId(),
            organization.getHostUrl(),
            organization.getName(),
            organization.getDvcsType(),
            organization.isAutolinkNewRepos(),
            organization.getCredential(),
            organization.getOrganizationUrl(),
            organization.isSmartcommitsOnNewRepos(),
            organization.getDefaultGroups()
        );
    }
}
