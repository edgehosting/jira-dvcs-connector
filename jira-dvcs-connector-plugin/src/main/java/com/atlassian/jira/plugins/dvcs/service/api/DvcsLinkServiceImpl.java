package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import javax.annotation.Nonnull;

import static java.util.Collections.unmodifiableList;

public class DvcsLinkServiceImpl implements DvcsLinkService
{
    private final OrganizationService organizationService;

    public DvcsLinkServiceImpl(OrganizationService organizationService)
    {
        this.organizationService = organizationService;
    }

    @Override
    public Organization getDvcsLink(boolean loadRepositories, int organizationId)
    {
        return organizationService.get(organizationId, loadRepositories);
    }

    @Override
    public List<Organization> getDvcsLinks(boolean loadRepositories)
    {
        return unmodifiableList(organizationService.getAll(loadRepositories));
    }

    @Override
    public List<Organization> getDvcsLinks(boolean loadRepositories, @Nonnull String applicationType)
    {
        if (StringUtils.isEmpty(applicationType))
        {
            throw new IllegalArgumentException("'applicationType' is null or empty");
        }
        return unmodifiableList(organizationService.getAll(loadRepositories, applicationType));
    }
}
