package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator.GITHUB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.RegenerateOauthTokenAction;
import com.atlassian.sal.api.ApplicationProperties;

public class RegenerateGithubOauthToken extends RegenerateOauthTokenAction
{
    private static final long serialVersionUID = 5153475610903119473L;

    private final Logger log = LoggerFactory.getLogger(RegenerateGithubOauthToken.class);
    // sent by GH on the way back
    private String code;
    protected final String baseUrl;

    public RegenerateGithubOauthToken(OrganizationService organizationService, RepositoryService repositoryService,ApplicationProperties applicationProperties, OAuthStore oAuthStore)
    {
        super(organizationService, repositoryService, oAuthStore);
        this.baseUrl = applicationProperties.getBaseUrl();
    }

    @Override
    protected String redirectUserToGrantAccess()
    {
        String organizationUrl = organizationService.get(Integer.parseInt(organization), false).getHostUrl();
        String githubAuthorizeUrl = getOAuthUtils().createGithubRedirectUrl(getRedirectAction(),
                organizationUrl, getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());
        return SystemUtils.getRedirect(this, githubAuthorizeUrl, true);
    }

    protected String getRedirectAction()
    {
        return "RegenerateGithubOauthToken";
    }

    protected GithubOAuthUtils getOAuthUtils()
    {
        return new GithubOAuthUtils(baseUrl, oAuthStore.getClientId(GITHUB), oAuthStore.getSecret(GITHUB));
    }

    @Override
    protected String getAccessToken()
    {
        return getOAuthUtils().requestAccessToken(organizationService.get(Integer.parseInt(organization), false).getHostUrl(), code);
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
