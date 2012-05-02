package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class AddGithubOrganization extends CommonDvcsConfigurationAction
{
	private static final long serialVersionUID = -2316358416248237835L;

	private final Logger log = LoggerFactory.getLogger(AddGithubOrganization.class);

	private String url;
	private String organization;

	private String oauthClientId;
	private String oauthSecret;
	private String oauthRequired;

	// sent by GH on the way back
	private String code;

	private final ApplicationProperties applicationProperties;
	private String accessToken = "";

	private final PluginSettingsFactory pluginSettingsFactory;

	private final GithubOAuth githubOAuth;

	private final OrganizationService organizationService;

	public AddGithubOrganization(OrganizationService organizationService,
								GithubOAuth githubOAuth,
								ApplicationProperties applicationProperties, PluginSettingsFactory pluginSettingsFactory)
	{
		this.organizationService = organizationService;
		this.githubOAuth = githubOAuth;
		this.applicationProperties = applicationProperties;
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception
	{

		if (isOAuthConfigurationRequired()) {
			configureOAuth();
		}
		
		// then continue
		return redirectUserToGithub();

	}

	private void configureOAuth()
	{
		githubOAuth.setClient(oauthClientId, oauthSecret);
	}

	private String redirectUserToGithub()
	{

		String encodedRepositoryUrl = encode(url);

		// Redirect back URL
		String redirectBackUrl = applicationProperties.getBaseUrl() + "/secure/admin/AddGithubRepository!finish.jspa?url="
				+ encodedRepositoryUrl + "&atl_token=" + getXsrfToken() + "&organization=" + organization + "&autoLinking=" + getAutoLinking();
		String encodedRedirectBackUrl = encode(redirectBackUrl);
		//
		// build URL to github
		//
		String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id="
				+ githubOAuth.getClientId() + "&redirect_uri=" + encodedRedirectBackUrl;


		return getRedirect(githubAuthorizeUrl);
	}

	/**
	 * TODO add detailed comment what is this for.
	 * 
	 * @param redirectBackUrl
	 */
	private void fixBackwardCompatibility()
	{

		String encodedRepositoryUrl = encode(url);

		String parameters = "repositoryUrl=" + encodedRepositoryUrl + "&atl_token=" + getXsrfToken();
		String redirectBackUrl = applicationProperties.getBaseUrl() + "/secure/admin/GitHubOAuth2.jspa?" + parameters;
		String encodedRedirectBackUrl = encode(redirectBackUrl);

		String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id="
				+ githubOAuth.getClientId() + "&redirect_uri=" + encodedRedirectBackUrl;

		pluginSettingsFactory.createGlobalSettings().put("OAuthRedirectUrl", githubAuthorizeUrl);
		pluginSettingsFactory.createGlobalSettings().put("OAuthRedirectUrlParameters", parameters);
	}

	@Override
	protected void doValidation()
	{

		if (StringUtils.isNotBlank(oauthRequired))
		{
			if (StringUtils.isBlank(oauthClientId) || StringUtils.isBlank(oauthSecret))
			{
				addErrorMessage("Please enter both the GitHub OAuth Client ID and Client Secret.");
			}
		}

	}
	
	protected boolean isOAuthConfigurationRequired() {
		return StringUtils.isNotBlank(oauthRequired);
	}

	public String doFinish()
	{

		try
		{

			accessToken = requestAccessToken();

		} catch (SourceControlException sce)
		{
			addErrorMessage(sce.getMessage());
			return INPUT;
		}

		return doAddOrganization();
	}

	private String doAddOrganization()
	{
		try
		{
			Organization newOrganization = new Organization();
			newOrganization.setName(organization);
			newOrganization.setHostUrl(url);
			newOrganization.setDvcsType("github");
			newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());
			newOrganization.setCredential(new Credential(null, null, accessToken));
			
			organizationService.save(newOrganization);
			
		} catch (SourceControlException e)
		{
			addErrorMessage("Failed adding the organization: [" + e.getMessage() + "]");
			log.debug("Failed adding the organization: [" + e.getMessage() + "]");
			return INPUT;
		}

		/*
		 * try { globalRepositoryManager.setupPostcommitHook(repository); }
		 * catch (SourceControlException e) {
		 * log.debug("Failed adding postcommit hook: [" + e.getMessage() + "]");
		 * globalRepositoryManager.removeRepository(repository.getId());
		 * addErrorMessage(
		 * "Error adding postcommit hook. Do you have admin rights to the repository? <br/> Repository was not added. ["
		 * + e.getMessage() + "]");
		 * 
		 * return INPUT; }
		 */

		return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + getXsrfToken());
	}

	private String requestAccessToken()
	{

		URL url;
		HttpURLConnection conn;

		BufferedReader rd;
		String line;
		String result = "";

		if (StringUtils.isEmpty(code))
		{
			throw new SourceControlException("Ops, no access code returned. Did you click Allow?");
		}

		try
		{

			String requestUrl = "https://github.com/login/oauth/access_token?&client_id=" + githubOAuth.getClientId()
					+ "&client_secret=" + githubOAuth.getClientSecret() + "&code=" + code;

			log.debug("requestAccessToken() - " + requestUrl);

			url = new URL(requestUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(true);
			conn.setRequestMethod("POST");

			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null)
			{
				log.debug("RESPONSE: " + line);
				result += line;
			}
			rd.close();

		} catch (MalformedURLException e)
		{
			log.error("Error obtain access token", e);
			
		} catch (Exception e)
		{
			log.error("Error obtain access token", e);
		}

		if (result.startsWith("error="))
		{
			String errorCode = result.replaceAll("error=", "");
			String error = errorCode;
			if (errorCode.equals("incorrect_client_credentials"))
			{
				error = "Incorrect client credentials";
			} else if (errorCode.equals("bad_verification_code"))
			{
				error = "Bad verification code";
			}

			throw new SourceControlException("Error obtaining access token: " + error);
		}

		result = result.replaceAll("access_token=(.*)&token_type.*", "$1");

		return result;
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

	public String getOauthRequired()
	{
		return oauthRequired;
	}

	public void setOauthRequired(String oauthRequired)
	{
		this.oauthRequired = oauthRequired;
	}

}
