package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import static com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator.GITHUB_ENTERPRISE;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException.InvalidResponseException;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.GithubOAuthUtils;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class AddGithubEnterpriseOrganization extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = 3680766022095591693L;

    private final Logger log = LoggerFactory.getLogger(AddGithubEnterpriseOrganization.class);

    private String organization;

    private String oauthClientIdGhe;
    private String oauthSecretGhe;

    // sent by GH on the way back
    private String code;
    private String url;

    private final OrganizationService organizationService;
    private final OAuthStore oAuthStore;
    private final ApplicationProperties applicationProperties;

    public AddGithubEnterpriseOrganization(OrganizationService organizationService,
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
        oAuthStore.store(new Host(GITHUB_ENTERPRISE, url), oauthClientIdGhe, oauthSecretGhe);

        // then continue
        return redirectUserToGithub();
    }

    private GithubOAuthUtils getGithubOAuthUtils()
    {
        return new GithubOAuthUtils(applicationProperties.getBaseUrl(), oAuthStore.getClientId(GITHUB_ENTERPRISE), oAuthStore.getSecret(GITHUB_ENTERPRISE));
    }

    private String redirectUserToGithub()
    {
        String githubAuthorizeUrl = getGithubOAuthUtils().createGithubRedirectUrl("AddGithubEnterpriseOrganization",
                url, getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());

        return SystemUtils.getRedirect(this, githubAuthorizeUrl, true);
    }

    @Override
    protected void doValidation()
    {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(organization))
        {
            addErrorMessage("Please provide both url and organization parameters.");
        }

        if (!SystemUtils.isValid(url))
        {
            addErrorMessage("Please provide valid GitHub host URL.");
        }

        if (url.endsWith("/"))
        {
            url = StringUtils.chop(url);

        }

//TODO validation of account is disabled because of private mode
//        AccountInfo accountInfo = organizationService.getAccountInfo(url, organization);
//        if (accountInfo == null)
//        {
//            addErrorMessage("Invalid user/team account.");
//        }

    }

    public String doFinish()
    {
        try
        {
            return doAddOrganization(getGithubOAuthUtils().requestAccessToken(url, code));
        } catch (InvalidResponseException ire)
        {
            addErrorMessage(ire.getMessage() + " Possibly bug in releases of GitHub Enterprise prior to 11.10.290.");
            return INPUT;

        } catch (SourceControlException sce)
        {
            addErrorMessage(sce.getMessage());
            log.warn(sce.getMessage());
            if ( sce.getCause() != null )
            {
                log.warn("Caused by: " + sce.getCause().getMessage());
            }
            return INPUT;

        } catch (Exception e)
        {
            addErrorMessage("Error obtaining access token.");
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
            newOrganization.setDvcsType(GithubEnterpriseCommunicator.GITHUB_ENTERPRISE);
            newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
            newOrganization.setCredential(new Credential(oAuthStore.getClientId(GITHUB_ENTERPRISE), oAuthStore.getSecret(GITHUB_ENTERPRISE), accessToken));
            newOrganization.setSmartcommitsOnNewRepos(hadAutolinkingChecked());

            organizationService.save(newOrganization);

        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            e.printStackTrace();
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

    public String getOrganization()
    {
        return organization;
    }

    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    public String getOauthClientIdGhe()
    {
        return oAuthStore.getClientId(GITHUB_ENTERPRISE);
    }

    public void setOauthClientIdGhe(String oauthClientIdGhe)
    {
        this.oauthClientIdGhe = oauthClientIdGhe;
    }

    public String getOauthSecretGhe()
    {
        return oAuthStore.getSecret(GITHUB_ENTERPRISE);
    }

    public void setOauthSecretGhe(String oauthSecretGhe)
    {
        this.oauthSecretGhe = oauthSecretGhe;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
}