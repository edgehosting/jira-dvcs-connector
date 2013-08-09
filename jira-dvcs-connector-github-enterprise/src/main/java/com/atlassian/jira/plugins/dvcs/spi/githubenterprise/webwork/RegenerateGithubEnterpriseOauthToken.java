package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.RegenerateGithubOauthToken;
import com.atlassian.sal.api.ApplicationProperties;

public class RegenerateGithubEnterpriseOauthToken extends RegenerateGithubOauthToken
{
    public RegenerateGithubEnterpriseOauthToken(EventPublisher eventPublisher, OrganizationService organizationService,
            RepositoryService repositoryService, ApplicationProperties applicationProperties)
    {
        super(eventPublisher, organizationService, repositoryService, applicationProperties);
    }

    @Override
    protected String getRedirectActionAndCommand()
    {
        return "RegenerateGithubEnterpriseOauthToken!finish";
    }
}