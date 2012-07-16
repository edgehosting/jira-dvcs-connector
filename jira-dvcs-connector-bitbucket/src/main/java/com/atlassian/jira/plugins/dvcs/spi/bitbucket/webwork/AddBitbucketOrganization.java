package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.apache.commons.lang.StringUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.SignatureType;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.InvalidCredentialsException;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuth;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;

/**
 * Webwork action used to configure the bitbucket organization.
 */
public class AddBitbucketOrganization extends CommonDvcsConfigurationAction
{
	private static final long serialVersionUID = 4366205447417138381L;

	private final Logger log = LoggerFactory.getLogger(AddBitbucketOrganization.class);

	private String url;
	private String organization;
	private String adminUsername;
	private String adminPassword;

	private String oauthBbClientId;
	private String oauthBbSecret;
	private String oauthBbRequired;

	private final OrganizationService organizationService;

	private final BitbucketOAuth oauth;

	private final com.atlassian.sal.api.ApplicationProperties ap;
	
	private String accessToken = "";

	public AddBitbucketOrganization(com.atlassian.sal.api.ApplicationProperties ap,
			OrganizationService organizationService, BitbucketOAuth oauth)
	{
		this.ap = ap;
		this.organizationService = organizationService;
		this.oauth = oauth;
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception
	{

		if (isOAuthConfigurationRequired())
		{
			configureOAuth();
		}

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

			request.getSession().setAttribute("requestToken", requestToken);
			
			return getRedirect(authUrl);

		} catch (Exception e)
		{
			addErrorMessage("Cannot proceed authentication, check your OAuth credentials!");
			return INPUT;
		}
	}

	private OAuthService createOAuthScribeService()
	{
		String redirectBackUrl = ap.getBaseUrl() + "/secure/admin/AddBitbucketOrganization!finish.jspa?organization="
				+ organization + "&autoLinking=" + getAutoLinking() + "&url=" + url + "&atl_token=" + getXsrfToken();

		return createBitbucketOAuthScribeService(redirectBackUrl);
	}

	private OAuthService createBitbucketOAuthScribeService(String callbackUrl)
	{
		ServiceBuilder sb = new ServiceBuilder().apiKey(oauth.getClientId()).signatureType(SignatureType.Header)
				.apiSecret(oauth.getClientSecret()).provider(new Bitbucket10aScribeApi(url)).debugStream(System.out);
		
		if (!StringUtils.isBlank(callbackUrl))
		{
			sb.callback(callbackUrl);
		}

		return sb.build();
	}

	private void configureOAuth()
	{
		oauth.setClient(oauthBbClientId, oauthBbSecret);
	}

	private boolean isOAuthConfigurationRequired()
	{
		return StringUtils.isNotBlank(oauthBbRequired);
	}

	public String doFinish()
	{

		// now get the access token

		Verifier verifier = new Verifier(request.getParameter("oauth_verifier"));
		Token requestToken = (Token) request.getSession().getAttribute("requestToken");
		request.getSession().removeAttribute("requestToken");

		OAuthService service = createOAuthScribeService();
		Token accessTokenObj = service.getAccessToken(requestToken, verifier);
		accessToken = BitbucketOAuthAuthentication.generateAccessTokenString(accessTokenObj);
	
		return doAddOrganization();
	}

	private String doAddOrganization()
	{
		try
		{
			Organization newOrganization = new Organization();
			newOrganization.setName(organization);
			newOrganization.setHostUrl(url);
			newOrganization.setDvcsType("bitbucket");
			newOrganization.setCredential(new Credential(null, null, accessToken));
			newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
			newOrganization.setSmartcommitsOnNewRepos(true);

			organizationService.save(newOrganization);

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
		} catch (InvalidCredentialsException e)
		{
			addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
			log.debug("Invalid credentials : Failed adding the account: [" + e.getMessage() + "]");
			return INPUT;
		}

		// go back to main DVCS configuration page
		return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + getXsrfToken());
	}

	@Override
	protected void doValidation()
	{
		if (StringUtils.isBlank(organization) || StringUtils.isBlank(url))
		{
			addErrorMessage("Invalid request, missing url or organization/account information.");
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

	public String getOauthBbRequired()
	{
		return oauthBbRequired;
	}

	public void setOauthBbRequired(String oauthBbRequired)
	{
		this.oauthBbRequired = oauthBbRequired;
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

}
