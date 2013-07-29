package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * @since v6.1
 */
public class DvcsLinkServiceImpl implements DvcsLinkService
{
    private static final Function<Organization, DvcsLink> ORGANIZATION_TO_DVCSLINK =
            new Function<Organization, DvcsLink>()
                {
                    @Override
                    public DvcsLink apply(Organization org)
                    {
                        if (org == null)
                        {
                            return null;
                        }
                        return new DvcsLink(org);
                    }
                };

    private final OrganizationService organizationService;

    public DvcsLinkServiceImpl (OrganizationService organizationService)
    {
        this.organizationService = organizationService;
    }

    @Override
    public DvcsLink getDvcsLink(boolean loadRepositories, int organizationId)
    {
        return ORGANIZATION_TO_DVCSLINK.apply(organizationService.get(organizationId, loadRepositories));
    }

    @Override
    public Iterable<DvcsLink> getDvcsLinks(boolean loadRepositories)
    {
        return Lists.transform(organizationService.getAll(loadRepositories), ORGANIZATION_TO_DVCSLINK);
    }

    @Override
    public Iterable<DvcsLink> getDvcsLinks(boolean loadRepositories, String applicationType)
    {
        return Lists.transform(organizationService.getAll(loadRepositories, applicationType), ORGANIZATION_TO_DVCSLINK);
    }
}
