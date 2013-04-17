package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator.GITHUB;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class AddGithubOrganization extends CommonDvcsConfigurationAction
{
    private final Logger log = LoggerFactory.getLogger(AddGithubOrganization.class);

    private String url;
    private String organization;

    private String oauthClientId;
    private String oauthSecret;

    // sent by GH on the way back
    private String code;

    private final OrganizationService organizationService;
    private final OAuthStore oAuthStore;
    private final ApplicationProperties applicationProperties;


    public AddGithubOrganization(OrganizationService organizationService,
            OAuthStore oAuthStore, ApplicationProperties applicationProperties)
    {
        this.organizationService = organizationService;
        this.oAuthStore = oAuthStore;
        this.applicationProperties = applicationProperties;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        oAuthStore.store(Host.GITHUB, oauthClientId, oauthSecret);

        // then continue
        return redirectUserToGithub();
    }

    private String redirectUserToGithub()
    {
        String githubAuthorizeUrl = getGithubOAuthUtils().createGithubRedirectUrl("AddGithubOrganization",
                url, getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());

        return SystemUtils.getRedirect(this, githubAuthorizeUrl, true);
    }

    private GithubOAuthUtils getGithubOAuthUtils()
    {
        return new GithubOAuthUtils(applicationProperties.getBaseUrl(), oAuthStore.getClientId(GITHUB), oAuthStore.getSecret(GITHUB));
    }

    @Override
    protected void doValidation()
    {

        if (StringUtils.isBlank(url) || StringUtils.isBlank(organization))
        {
            addErrorMessage("Please provide both url and organization parameters.");
        }

        AccountInfo accountInfo = organizationService.getAccountInfo("https://github.com", organization);
        if (accountInfo == null)
        {
            addErrorMessage("Invalid user/team account.");
        }
    }

    public String doFinish()
    {
        try
        {
            return doAddOrganization(getGithubOAuthUtils().requestAccessToken(code));
        } catch (SourceControlException sce)
        {
            addErrorMessage(sce.getMessage());
            log.warn(sce.getMessage());
            if ( sce.getCause() != null )
            {
                log.warn("Caused by: " + sce.getCause().getMessage());
            }
            return INPUT;

        } catch (Exception e) {
            addErrorMessage("Error obtain access token.");
            return INPUT;
        }
    }

    private String doAddOrganization(String accessToken)
    {
        try
        {
            Organization newOrganization = new Organization();
            newOrganization.setName(organization);
            newOrganization.setHostUrl(url);
            newOrganization.setDvcsType("github");
            newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
            newOrganization.setCredential(new Credential(oAuthStore.getClientId(Host.GITHUB.id),
                    oAuthStore.getSecret(Host.GITHUB.id), accessToken));
            newOrganization.setSmartcommitsOnNewRepos(hadAutolinkingChecked());

            organizationService.save(newOrganization);

        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            return INPUT;
        }

        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
    }

    public static String encode(String url)
    {
        return CustomStringUtils.encode(url);
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getOrganization()
    {
        return organization;
    }

    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    public String getOauthClientId()
    {
        return oauthClientId;
    }

    public void setOauthClientId(String oauthClientId)
    {
        this.oauthClientId = oauthClientId;
    }

    public String getOauthSecret()
    {
        return oauthSecret;
    }

    public void setOauthSecret(String oauthSecret)
    {
        this.oauthSecret = oauthSecret;
    }
}
