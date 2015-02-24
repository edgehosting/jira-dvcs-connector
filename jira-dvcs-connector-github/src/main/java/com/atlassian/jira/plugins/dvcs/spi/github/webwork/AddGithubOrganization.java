package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_GENERIC;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_SOURCECONTROL;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_VALIDATION;
import static com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator.GITHUB;

@Scanned
public class AddGithubOrganization extends CommonDvcsConfigurationAction
{
    private final Logger log = LoggerFactory.getLogger(AddGithubOrganization.class);

    public static final String EVENT_TYPE_GITHUB = "github";
    public static final String DISABLE_USERNAME_VALIDATION = "dvcs.connector.github.user.validation.disabled";

    private String url;
    private String organization;

    private String oauthClientId;
    private String oauthSecret;

    // sent by GH on the way back
    private String code;

    private final OrganizationService organizationService;
    private final OAuthStore oAuthStore;
    private final ApplicationProperties applicationProperties;
    private final FeatureManager featureManager;

    public AddGithubOrganization(@ComponentImport ApplicationProperties applicationProperties,
            @ComponentImport EventPublisher eventPublisher,
            OAuthStore oAuthStore,
            OrganizationService organizationService, @ComponentImport FeatureManager featureManager)
    {
        super(eventPublisher);
        this.organizationService = organizationService;
        this.oAuthStore = oAuthStore;
        this.applicationProperties = applicationProperties;
        this.featureManager = featureManager;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        triggerAddStartedEvent(EVENT_TYPE_GITHUB);

        oAuthStore.store(Host.GITHUB, oauthClientId, oauthSecret);

        // then continue
        return redirectUserToGithub();
    }

    private String redirectUserToGithub()
    {
        String githubAuthorizeUrl = getGithubOAuthUtils().createGithubRedirectUrl("AddOrganizationProgressAction!default",
                url, getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());

        // param "t" is holding information where to redirect from "wainting screen" (AddBitbucketOrganization, AddGithubOrganization ...)
        return SystemUtils.getRedirect(this, githubAuthorizeUrl + urlEncode("&t=2"), true);
    }

    GithubOAuthUtils getGithubOAuthUtils()
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

        if (!featureManager.isEnabled(DISABLE_USERNAME_VALIDATION))
        {
            AccountInfo accountInfo = organizationService.getAccountInfo("https://github.com", organization, GithubCommunicator.GITHUB);
            if (accountInfo == null)
            {
                addErrorMessage("Invalid user/team account.");
            }
        }

        if (organizationService.getByHostAndName(url, organization) != null)
        {
            addErrorMessage("Account is already integrated with JIRA.");
        }

        if (invalidInput())
        {
            triggerAddFailedEvent(FAILED_REASON_VALIDATION);
        }
    }

    public String doFinish()
    {
        try
        {
            return doAddOrganization(getGithubOAuthUtils().requestAccessToken(code));
        }
        catch (SourceControlException sce)
        {
            addErrorMessage(sce.getMessage());
            log.warn(sce.getMessage());
            if (sce.getCause() != null)
            {
                log.warn("Caused by: " + sce.getCause().getMessage());
            }
            triggerAddFailedEvent(FAILED_REASON_OAUTH_SOURCECONTROL);
            return INPUT;
        }
        catch (Exception e)
        {
            addErrorMessage("Error obtain access token.");
            triggerAddFailedEvent(FAILED_REASON_OAUTH_GENERIC);
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
            newOrganization.setDvcsType(GithubCommunicator.GITHUB);
            newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
            newOrganization.setCredential(new Credential(oAuthStore.getClientId(Host.GITHUB.id),
                    oAuthStore.getSecret(Host.GITHUB.id), accessToken));
            newOrganization.setSmartcommitsOnNewRepos(hadAutolinkingChecked());

            organizationService.save(newOrganization);

        }
        catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            triggerAddFailedEvent(FAILED_REASON_OAUTH_SOURCECONTROL);
            return INPUT;
        }

        triggerAddSucceededEvent(EVENT_TYPE_GITHUB);
        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()) +
                getSourceAsUrlParam());
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

    private void triggerAddFailedEvent(String reason)
    {
        super.triggerAddFailedEvent(EVENT_TYPE_GITHUB, reason);
    }
}
