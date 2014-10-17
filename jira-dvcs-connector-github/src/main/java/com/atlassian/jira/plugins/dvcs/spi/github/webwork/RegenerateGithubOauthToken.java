package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.RegenerateOauthTokenAction;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegenerateGithubOauthToken extends RegenerateOauthTokenAction
{
    private final Logger log = LoggerFactory.getLogger(RegenerateGithubOauthToken.class);

    // sent by GH on the way back
    private String code;

    protected final String baseUrl;

    @Autowired
    public RegenerateGithubOauthToken(@ComponentImport EventPublisher eventPublisher,
            OrganizationService organizationService, RepositoryService repositoryService,
            @ComponentImport ApplicationProperties applicationProperties)
    {
        super(eventPublisher, organizationService, repositoryService);
        this.baseUrl = applicationProperties.getBaseUrl();
    }

    @Override
    protected String redirectUserToGrantAccess()
    {
        Organization organizationObject = getOrganizationObject();
        String organizationUrl = organizationObject.getHostUrl();
        String githubAuthorizeUrl = getOAuthUtils(organizationObject).createGithubRedirectUrl(getRedirectActionAndCommand(),
                organizationUrl, getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());
        return SystemUtils.getRedirect(this, githubAuthorizeUrl, true);
    }

    private Organization getOrganizationObject()
    {
        return organizationService.get(Integer.parseInt(organization), false);
    }

    protected String getRedirectActionAndCommand()
    {
        return "RegenerateGithubOauthToken!finish";
    }

    protected GithubOAuthUtils getOAuthUtils(Organization org)
    {
        return new GithubOAuthUtils(baseUrl, org.getCredential().getOauthKey(), org.getCredential().getOauthSecret());
    }

    @Override
    protected String getAccessToken()
    {
        return getOAuthUtils(getOrganizationObject()).requestAccessToken(getOrganizationObject().getHostUrl(), code);
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }
}
