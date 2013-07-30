package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;

/**
 * @since v6.1
 */
public class DvcsLinkServiceImpl implements DvcsLinkService
{
    private final OrganizationService organizationService;

    public DvcsLinkServiceImpl (OrganizationService organizationService)
    {
        this.organizationService = organizationService;
    }

    @Override
    public Organization getDvcsLink(boolean loadRepositories, int organizationId)
    {
        return organizationService.get(organizationId, loadRepositories);
    }

    @Override
    public Iterable<Organization> getDvcsLinks(boolean loadRepositories)
    {
        return organizationService.getAll(loadRepositories);
    }

    @Override
    public Iterable<Organization> getDvcsLinks(boolean loadRepositories, String applicationType)
    {
        return organizationService.getAll(loadRepositories, applicationType);
    }
}
