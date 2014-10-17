package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import javax.annotation.Nonnull;

import static java.util.Collections.unmodifiableList;

@ExportAsService (DvcsLinkService.class)
@Component
public class DvcsLinkServiceImpl implements DvcsLinkService
{
    private final OrganizationService organizationService;

    @Autowired
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
