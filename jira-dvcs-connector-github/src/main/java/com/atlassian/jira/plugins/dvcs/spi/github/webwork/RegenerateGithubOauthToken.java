package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator.GITHUB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class RegenerateGithubOauthToken extends CommonDvcsConfigurationAction
{
	private final Logger log = LoggerFactory.getLogger(RegenerateGithubOauthToken.class);

	private String organization; // in the meaning of id

	// sent by GH on the way back
	private String code;

	private final OrganizationService organizationService;
    protected final OAuthStore oAuthStore;
    protected final String baseUrl;


	public RegenerateGithubOauthToken(OrganizationService organizationService, ApplicationProperties applicationProperties, OAuthStore oAuthStore)
	{
		this.organizationService = organizationService;
        this.baseUrl = applicationProperties.getBaseUrl();
        this.oAuthStore = oAuthStore;
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception
	{
		return redirectUserToGithub();
	}

	private String redirectUserToGithub()
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

	public String doFinish()
	{
		try
		{
			return doChangeAccessToken();
		} catch (SourceControlException sce)
		{
			addErrorMessage(sce.getMessage());
			log.warn(sce.getMessage());
			if ( sce.getCause() != null )
			{
				log.warn("Caused by: " + sce.getCause().getMessage());
			}
			return INPUT;
		}
	}

	private String doChangeAccessToken()
	{
		try
		{
		    String accessToken = getOAuthUtils().requestAccessToken(organizationService.get(Integer.parseInt(organization), false).getHostUrl(), code);
			organizationService.updateCredentialsAccessToken(Integer.parseInt(organization), accessToken);
		} catch (SourceControlException e)
		{
			addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
			log.debug("Failed adding the account: [" + e.getMessage() + "]");
			return INPUT;
		}

		return getRedirect("SyncRepositoryListAction.jspa?organizationId=" + organization + "&atl_token=" + CustomStringUtils.encode(getXsrfToken()));
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
}
