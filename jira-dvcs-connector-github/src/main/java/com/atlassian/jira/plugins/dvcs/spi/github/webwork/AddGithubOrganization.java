package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;
import com.atlassian.jira.plugins.bitbucket.spi.github.GithubOAuth;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class AddGithubOrganization extends JiraWebActionSupport {
	private static final long serialVersionUID = -2316358416248237835L;

	private final Logger log = LoggerFactory
			.getLogger(AddGithubOrganization.class);

	private String url;
	private String organization;

	private String code;

	private final Synchronizer synchronizer;
	private final ApplicationProperties ap;
	private final GithubOAuth githubOAuth;
	private String accessToken = "";

	private final PluginSettingsFactory pluginSettingsFactory;

	public AddGithubOrganization(Synchronizer synchronizer,
			ApplicationProperties applicationProperties,
			GithubOAuth githubOAuth, PluginSettingsFactory pluginSettingsFactory) {
		this.synchronizer = synchronizer;
		this.ap = applicationProperties;
		this.githubOAuth = githubOAuth;
		this.pluginSettingsFactory = pluginSettingsFactory;
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception {
	
		return redirectUserToGithub();
	
	}

	private String redirectUserToGithub() {
	
		String encodedRepositoryUrl = encode(url);

		// Redirect back URL
		String redirectBackUrl = ap.getBaseUrl()
				+ "/secure/admin/AddGithubRepository!finish.jspa?repositoryUrl="
				+ encodedRepositoryUrl + "&atl_token=" + getXsrfToken();
		String encodedRedirectBackUrl = encode(redirectBackUrl);
		//
		// build URL to github
		//
		String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id="
				+ githubOAuth.getClientId()
				+ "&redirect_uri="
				+ encodedRedirectBackUrl;

		//
		//
		fixBackwardCompatibility();

		return getRedirect(githubAuthorizeUrl);
	}

	/**
	 * TODO add detailed comment what is this for.
	 * 
	 * @param redirectBackUrl
	 */
	private void fixBackwardCompatibility() {
		
		String encodedRepositoryUrl = encode(url);
		
		String parameters = "repositoryUrl=" + encodedRepositoryUrl + "&atl_token=" + getXsrfToken();
		String redirectBackUrl = ap.getBaseUrl()
				+ "/secure/admin/GitHubOAuth2.jspa?" + parameters;
		String encodedRedirectBackUrl = encode(redirectBackUrl);
		
		String githubAuthorizeUrl = "https://github.com/login/oauth/authorize?scope=repo&client_id="
				+ githubOAuth.getClientId()
				+ "&redirect_uri="
				+ encodedRedirectBackUrl;

		pluginSettingsFactory.createGlobalSettings().put("OAuthRedirectUrl",
				githubAuthorizeUrl);
		pluginSettingsFactory.createGlobalSettings().put(
				"OAuthRedirectUrlParameters", parameters);
	}

	public String doFinish() {
		
		try {
		
			accessToken = requestAccessToken();
		
		} catch (SourceControlException sce) {
			addErrorMessage(sce.getMessage());
			return INPUT;
		}

		return doAddOrganization();
	}

	private String doAddOrganization() {
		
		SourceControlRepository repository;
	
		try {
			repository = null; // globalRepositoryManager.addRepository(GithubRepositoryManager.GITHUB,
								// projectKey, url,
			// "", "", accessToken);
			synchronizer.synchronize(repository);

		} catch (SourceControlException e) {
			addErrorMessage("Failed adding the repository: [" + e.getMessage()
					+ "]");
			log.debug("Failed adding the repository: [" + e.getMessage() + "]");
			return INPUT;
		}

		/*try {
			globalRepositoryManager.setupPostcommitHook(repository);
		} catch (SourceControlException e) {
			log.debug("Failed adding postcommit hook: [" + e.getMessage() + "]");
			globalRepositoryManager.removeRepository(repository.getId());
			addErrorMessage("Error adding postcommit hook. Do you have admin rights to the repository? <br/> Repository was not added. ["
					+ e.getMessage() + "]");

			return INPUT;
		}*/

		return getRedirect("ConfigureBitbucketRepositories.jspa?addedRepositoryId="
				+ repository.getId() + "&atl_token=" + getXsrfToken());
	}

	private String requestAccessToken() {
		
		URL url;
		HttpURLConnection conn;

		BufferedReader rd;
		String line;
		String result = "";

		if (StringUtils.isEmpty(code)) {
			throw new SourceControlException(
					"Ops, no access code returned. Did you click Allow?");
		}

		try {
			
			String requestUrl = "https://github.com/login/oauth/access_token?&client_id="
					+ githubOAuth.getClientId() + "&client_secret="
					+ githubOAuth.getClientSecret() + "&code=" + code;
			
			log.debug("requestAccessToken() - " + requestUrl);

			url = new URL(requestUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(true);
			conn.setRequestMethod("POST");
			
			rd = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				log.debug("RESPONSE: " + line);
				result += line;
			}
			rd.close();
	
		} catch (MalformedURLException e) {
			log.error("Error obtain access token", e);
		} catch (Exception e) {
			log.error("Error obtain access token", e);
		}

		if (result.startsWith("error=")) {
			String errorCode = result.replaceAll("error=", "");
			String error = errorCode;
			if (errorCode.equals("incorrect_client_credentials")) {
				error = "Incorrect client credentials";
			} else if (errorCode.equals("bad_verification_code")) {
				error = "Bad verification code";
			}

			throw new SourceControlException("Error obtaining access token: "
					+ error);
		}

		result = result.replaceAll("access_token=(.*)&token_type.*", "$1");

		return result;
	}
	
	public static String encode(String url) {
		return CustomStringUtils.encode(url);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}

}
