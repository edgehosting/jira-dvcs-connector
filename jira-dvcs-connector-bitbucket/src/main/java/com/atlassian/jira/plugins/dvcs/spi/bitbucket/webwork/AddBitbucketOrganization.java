package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_SOURCECONTROL;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_TOKEN;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_OAUTH_UNAUTH;
import static com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigAddEndedAnalyticsEvent.FAILED_REASON_VALIDATION;

/**
 * Webwork action used to configure the bitbucket organization.
 */
@Scanned
public class AddBitbucketOrganization extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = 4366205447417138381L;

    private final static Logger log = LoggerFactory.getLogger(AddBitbucketOrganization.class);

    public static final String EVENT_TYPE_BITBUCKET = "bitbucket";
    public static final String SESSION_KEY_REQUEST_TOKEN = "requestToken";

    private String url;
    private String organization;
    private String adminUsername;
    private String adminPassword;

    private String oauthBbClientId;
    private String oauthBbSecret;

    private final OrganizationService organizationService;
    private final HttpClientProvider httpClientProvider;

    private final com.atlassian.sal.api.ApplicationProperties ap;

    private String accessToken = "";

    private final OAuthStore oAuthStore;

    public AddBitbucketOrganization(@ComponentImport ApplicationProperties ap,
            @ComponentImport EventPublisher eventPublisher,
            OAuthStore oAuthStore,
            OrganizationService organizationService,
            HttpClientProvider httpClientProvider)
    {
        super(eventPublisher);
        this.ap = ap;
        this.organizationService = organizationService;
        this.oAuthStore = oAuthStore;
        this.httpClientProvider = httpClientProvider;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        triggerAddStartedEvent(EVENT_TYPE_BITBUCKET);

        storeLatestOAuth();

        // then continue
        return redirectUserToBitbucket();
    }

    private String redirectUserToBitbucket()
    {
        try
        {
            OAuthService service = createOAuthScribeService();
            Token requestToken = service.getRequestToken();
            String authUrl = service.getAuthorizationUrl(requestToken);

            request.getSession().setAttribute(SESSION_KEY_REQUEST_TOKEN, requestToken);

            return SystemUtils.getRedirect(this, authUrl, true);
        }
        catch (Exception e)
        {
            log.warn("Error redirect user to bitbucket server.", e);
            addErrorMessage("The authentication with Bitbucket has failed. Please check your OAuth settings.");
            triggerAddFailedEvent(FAILED_REASON_OAUTH_TOKEN);
            return INPUT;
        }
    }

    OAuthService createOAuthScribeService()
    {
        // param "t" is holding information where to redirect from "wainting screen" (AddBitbucketOrganization, AddGithubOrganization ...)
        String redirectBackUrl = ap.getBaseUrl()
                + "/secure/admin/AddOrganizationProgressAction!default.jspa?organization="
                + organization + "&autoLinking=" + getAutoLinking()
                + "&url=" + url + "&autoSmartCommits="
                + getAutoSmartCommits() + "&atl_token=" + getXsrfToken() + "&t=1"
                + getSourceAsUrlParam();

        return createBitbucketOAuthScribeService(redirectBackUrl);
    }

    private OAuthService createBitbucketOAuthScribeService(String callbackUrl)
    {
        ServiceBuilder sb = new ServiceBuilder().apiKey(oAuthStore.getClientId(Host.BITBUCKET.id))
                .signatureType(SignatureType.Header)
                .apiSecret(oAuthStore.getSecret(Host.BITBUCKET.id))
                .provider(new Bitbucket10aScribeApi(url, httpClientProvider))
                .debugStream(new DebugOutputStream(log));

        if (!StringUtils.isBlank(callbackUrl))
        {
            sb.callback(callbackUrl);
        }

        return sb.build();
    }

    private void storeLatestOAuth()
    {
        oAuthStore.store(Host.BITBUCKET, oauthBbClientId, oauthBbSecret);
    }

    public String doFinish()
    {
        // now get the access token
        Verifier verifier = new Verifier(request.getParameter("oauth_verifier"));
        Token requestToken = (Token) request.getSession().getAttribute(SESSION_KEY_REQUEST_TOKEN);

        if (requestToken == null)
        {
            log.debug("Request token is NULL. It has been removed in the previous attempt of adding organization. Now we will stop.");
            return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
        }

        request.getSession().removeAttribute(SESSION_KEY_REQUEST_TOKEN);

        OAuthService service = createOAuthScribeService();
        Token accessTokenObj = service.getAccessToken(requestToken, verifier);
        accessToken = BitbucketOAuthAuthentication.generateAccessTokenString(accessTokenObj);

        httpClientProvider.closeIdleConnections();

        return doAddOrganization();
    }

    private String doAddOrganization()
    {

        try
        {
            Organization newOrganization = new Organization();
            newOrganization.setName(organization);
            newOrganization.setHostUrl(url);
            newOrganization.setDvcsType(BitbucketCommunicator.BITBUCKET);
            newOrganization.setCredential(new Credential(oAuthStore.getClientId(Host.BITBUCKET.id), oAuthStore.getSecret(Host.BITBUCKET.id), accessToken));
            newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
            newOrganization.setSmartcommitsOnNewRepos(hadAutoSmartCommitsChecked());
            organizationService.save(newOrganization);
        }
        catch (SourceControlException.UnauthorisedException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            triggerAddFailedEvent(FAILED_REASON_OAUTH_UNAUTH);
            return INPUT;
        }
        catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            triggerAddFailedEvent(FAILED_REASON_OAUTH_SOURCECONTROL);
            return INPUT;
        }

        triggerAddSucceededEvent(EVENT_TYPE_BITBUCKET);

        // go back to main DVCS configuration page
        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken())
                + getSourceAsUrlParam());
    }

    @Override
    protected void doValidation()
    {
        if (StringUtils.isBlank(organization) || StringUtils.isBlank(url))
        {
            addErrorMessage("Invalid request, missing url or organization/account information.");
        }

        if (StringUtils.isNotBlank(organization))
        {
            Organization integratedAccount = organizationService.findIntegratedAccount();
            if (integratedAccount != null && organization.trim().equalsIgnoreCase(integratedAccount.getName()))
            {
                addErrorMessage("It is not possible to add the same account as the integrated one.");
            }
        }

        AccountInfo accountInfo = organizationService.getAccountInfo(url, organization, BitbucketCommunicator.BITBUCKET);
        // Bitbucket REST API to determine existence of accountInfo accepts valid email associated with BB account, but
        // it is not possible to create an account containing the '@' character.
        // [https://confluence.atlassian.com/display/BITBUCKET/account+Resource#accountResource-GETtheaccountprofile]
        if (accountInfo == null || organization.contains("@"))
        {
            addErrorMessage("Invalid user/team account.");
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

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
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

    public String getAdminUsername()
    {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername)
    {
        this.adminUsername = adminUsername;
    }


    public String getOauthBbClientId()
    {
        return oauthBbClientId;
    }

    public void setOauthBbClientId(String oauthBbClientId)
    {
        this.oauthBbClientId = oauthBbClientId;
    }

    public String getOauthBbSecret()
    {
        return oauthBbSecret;
    }

    public void setOauthBbSecret(String oauthBbSecret)
    {
        this.oauthBbSecret = oauthBbSecret;
    }

    private void triggerAddFailedEvent(String reason)
    {
        super.triggerAddFailedEvent(EVENT_TYPE_BITBUCKET, reason);
    }
}
