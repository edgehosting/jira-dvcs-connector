package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationManagementService;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.util.DebugOutputStream;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.google.common.collect.Sets;

/**
 * Webwork action used to configure the bitbucket organization.
 */
public class AddBitbucketOrganization extends CommonDvcsConfigurationAction
{
    private final static Logger log = LoggerFactory.getLogger(AddBitbucketOrganization.class);

    public static final String DEFAULT_INVITATION_GROUP = "developers";

    private String url;
    private String organization;
    private String adminUsername;
    private String adminPassword;

    private String oauthBbClientId;
    private String oauthBbSecret;

    private final OrganizationService organizationService;


    private final com.atlassian.sal.api.ApplicationProperties ap;

    private String accessToken = "";

    private final OAuthStore oAuthStore;

	private final OrganizationManagementService managementService;

    public AddBitbucketOrganization(com.atlassian.sal.api.ApplicationProperties ap,
            OrganizationService organizationService, OAuthStore oAuthStore, OrganizationManagementService managementService)
    {
        this.ap = ap;
        this.organizationService = organizationService;
        this.oAuthStore = oAuthStore;
		this.managementService = managementService;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
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

            this.request.getSession().setAttribute("requestToken", requestToken);

            return SystemUtils.getRedirect(this, authUrl, true);
        } catch (Exception e)
        {
            log.warn("Error redirect user to bitbucket server.", e);
            addErrorMessage("The authentication with Bitbucket has failed. Please check your OAuth settings.");
            return INPUT;
        }
    }

    private OAuthService createOAuthScribeService()
	{
		String redirectBackUrl = this.ap.getBaseUrl()
		        + "/secure/admin/AddOrganizationProgressAction!default.jspa?organization="
		        + this.organization + "&autoLinking=" + getAutoLinking()
		        + "&url=" + this.url + "&autoSmartCommits="
		        + getAutoSmartCommits() + "&atl_token=" + getXsrfToken() + "&t=1";

        return createBitbucketOAuthScribeService(redirectBackUrl);
    }

    private OAuthService createBitbucketOAuthScribeService(String callbackUrl)
    {
        ServiceBuilder sb = new ServiceBuilder().apiKey(this.oAuthStore.getClientId(Host.BITBUCKET.id))
                                                .signatureType(SignatureType.Header)
                                                .apiSecret(this.oAuthStore.getSecret(Host.BITBUCKET.id))
                                                .provider(new Bitbucket10aScribeApi(this.url))
                                                .debugStream(new DebugOutputStream(log));

        if (!StringUtils.isBlank(callbackUrl))
        {
            sb.callback(callbackUrl);
        }

        return sb.build();
    }

    private void storeLatestOAuth()
    {
        this.oAuthStore.store(Host.BITBUCKET, this.oauthBbClientId, this.oauthBbSecret);
    }

    public String doFinish()
    {
        // now get the access token
        Verifier verifier = new Verifier(this.request.getParameter("oauth_verifier"));
        Token requestToken = (Token) this.request.getSession().getAttribute("requestToken");

        if (requestToken == null) {
            log.debug("Request token is NULL. It has been removed in the previous attempt of adding organization. Now we will stop.");
            return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
        }

        this.request.getSession().removeAttribute("requestToken");

        OAuthService service = createOAuthScribeService();
        Token accessTokenObj = service.getAccessToken(requestToken, verifier);
        this.accessToken = BitbucketOAuthAuthentication.generateAccessTokenString(accessTokenObj);

        return doAddOrganization();
    }

    private String doAddOrganization()
    {
    	
        try
        {
            Organization newOrganization = new Organization();
            newOrganization.setName(this.organization);
            newOrganization.setHostUrl(this.url);
            newOrganization.setDvcsType("bitbucket");
            newOrganization.setCredential(new Credential(this.oAuthStore.getClientId(Host.BITBUCKET.id), this.oAuthStore.getSecret(Host.BITBUCKET.id), this.accessToken));
            newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
            newOrganization.setSmartcommitsOnNewRepos(hadAutoSmartCommitsChecked());
            newOrganization.setDefaultGroups(Sets.newHashSet(new Group(DEFAULT_INVITATION_GROUP)));
            this.organizationService.save(newOrganization);

        } catch (SourceControlException.UnauthorisedException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            return INPUT;
        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
            return INPUT;
        }

        // go back to main DVCS configuration page
        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
    }

    @Override
    protected void doValidation()
    {
        if (StringUtils.isBlank(this.organization) || StringUtils.isBlank(this.url))
        {
            addErrorMessage("Invalid request, missing url or organization/account information.");
        }

        if (StringUtils.isNotBlank(this.organization))
        {
            Organization integratedAccount = this.organizationService.findIntegratedAccount();
            if (integratedAccount != null && this.organization.trim().equalsIgnoreCase(integratedAccount.getName()))
            {
                addErrorMessage("It is not possible to add the same account as the integrated one.");
            }
        }

        AccountInfo accountInfo = this.organizationService.getAccountInfo("https://bitbucket.org", this.organization);
        // Bitbucket REST API to determine existence of accountInfo accepts valid email associated with BB account, but
        // it is not possible to create an account containing the '@' character.
        // [https://confluence.atlassian.com/display/BITBUCKET/account+Resource#accountResource-GETtheaccountprofile]
        if (accountInfo == null || this.organization.contains("@"))
        {
            addErrorMessage("Invalid user/team account.");
        }
    }

    public String getAdminPassword()
    {
        return this.adminPassword;
    }

    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
    }

    public String getUrl()
    {
        return this.url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getOrganization()
    {
        return this.organization;
    }

    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

    public String getAdminUsername()
    {
        return this.adminUsername;
    }

    public void setAdminUsername(String adminUsername)
    {
        this.adminUsername = adminUsername;
    }


    public String getOauthBbClientId()
    {
        return this.oauthBbClientId;
    }

    public void setOauthBbClientId(String oauthBbClientId)
    {
        this.oauthBbClientId = oauthBbClientId;
    }

    public String getOauthBbSecret()
    {
        return this.oauthBbSecret;
    }

    public void setOauthBbSecret(String oauthBbSecret)
    {
        this.oauthBbSecret = oauthBbSecret;
    }

}
