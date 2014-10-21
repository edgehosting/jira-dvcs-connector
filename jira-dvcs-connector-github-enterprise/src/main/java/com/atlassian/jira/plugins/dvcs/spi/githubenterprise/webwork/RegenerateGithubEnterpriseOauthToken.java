package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.RegenerateGithubOauthToken;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;

@Scanned
public class RegenerateGithubEnterpriseOauthToken extends RegenerateGithubOauthToken
{
    public RegenerateGithubEnterpriseOauthToken(@ComponentImport EventPublisher eventPublisher,
            OrganizationService organizationService, RepositoryService repositoryService,
            @ComponentImport ApplicationProperties applicationProperties)
    {
        super(eventPublisher, organizationService, repositoryService, applicationProperties);
    }

    @Override
    protected String getRedirectActionAndCommand()
    {
        return "RegenerateGithubEnterpriseOauthToken!finish";
    }
}
