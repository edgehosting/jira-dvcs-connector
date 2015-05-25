package com.atlassian.jira.plugins.dvcs.spi.gitlab.webwork;

import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_GENERIC;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_SOURCECONTROL;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_VALIDATION;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.gitlab.GitlabCommunicator;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.ApplicationProperties;

@Scanned
public class AddGitlabOrganization extends CommonDvcsConfigurationAction
{
    private final Logger log = LoggerFactory.getLogger(AddGitlabOrganization.class);

    public static final String EVENT_TYPE_GITLAB = "gitlab";

    private String urlGl;
    private String organization;

    private String apiKeyGl;
    
    private final OrganizationService organizationService;
    private final OAuthStore oAuthStore;
    private final ApplicationProperties applicationProperties;

    public AddGitlabOrganization(ApplicationProperties applicationProperties,
                                 EventPublisher eventPublisher,
                                 OAuthStore oAuthStore,
                                 OrganizationService organizationService)
    {
        super(eventPublisher);
        this.organizationService = organizationService;
        this.oAuthStore = oAuthStore;
        this.applicationProperties = applicationProperties;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        triggerAddStartedEvent(EVENT_TYPE_GITLAB);

        //oAuthStore.store(new Host(GitlabCommunicator.GITLAB, url), oauthClientId, oauthSecret);

        // then continue
        return doFinish();
    }

    @Override
    protected void doValidation()
    {

        if (StringUtils.isBlank(urlGl) || StringUtils.isBlank(organization) || StringUtils.isBlank(apiKeyGl))
        {
            addErrorMessage("Please provide url, API key and organization parameters.");
        }

        /**
         * TODO Recheck
         * 
        AccountInfo accountInfo = organizationService.getAccountInfo("https://github.com", organization, GitlabCommunicator.GITLAB);
        if (accountInfo == null)
        {
            addErrorMessage("Invalid user/team account.");
        }
        **/

        if (organizationService.getByHostAndName(urlGl, organization) != null)
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
            return doAddOrganization(apiKeyGl);
        } catch (SourceControlException sce)
        {
            addErrorMessage(sce.getMessage());
            log.warn(sce.getMessage());
            if (sce.getCause() != null)
            {
                log.warn("Caused by: " + sce.getCause().getMessage());
            }
            triggerAddFailedEvent(FAILED_REASON_OAUTH_SOURCECONTROL);
            return INPUT;
        } catch (Exception e)
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
            newOrganization.setHostUrl(urlGl);
            newOrganization.setDvcsType(GitlabCommunicator.GITLAB);
            newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
            newOrganization.setCredential(new Credential(null, null, apiKeyGl));
            newOrganization.setSmartcommitsOnNewRepos(hadAutolinkingChecked());

            organizationService.save(newOrganization);

        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            triggerAddFailedEvent(FAILED_REASON_OAUTH_SOURCECONTROL);
            return INPUT;
        }

        triggerAddSucceededEvent(EVENT_TYPE_GITLAB);
        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()) +
                            getSourceAsUrlParam());
    }

    public static String encode(String url)
    {
        return CustomStringUtils.encode(url);
    }

    public String getUrlGl()
    {
        return urlGl;
    }

    public void setUrlGl(String url)
    {
        this.urlGl = url;
    }

    public String getOrganization()
    {
        return organization;
    }

    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    public String getApiKeyGl() {
		return apiKeyGl;
	}

	public void setApiKeyGl(String apiKey) {
		this.apiKeyGl = apiKey;
	}

	private void triggerAddFailedEvent(String reason)
    {
        super.triggerAddFailedEvent(EVENT_TYPE_GITLAB, reason);
    }
}
