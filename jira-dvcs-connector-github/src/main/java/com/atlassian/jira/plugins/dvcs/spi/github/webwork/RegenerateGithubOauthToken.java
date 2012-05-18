package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.exception.InvalidCredentialsException;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;

public class RegenerateGithubOauthToken extends CommonDvcsConfigurationAction
{
	private static final long serialVersionUID = -2316358416248237835L;

	private final Logger log = LoggerFactory.getLogger(RegenerateGithubOauthToken.class);

	private String organization; // in the meaning of id

	// sent by GH on the way back
	private String code;

	private String accessToken = "";


	private final OrganizationService organizationService;
	private final GithubOAuthUtils githubOAuthUtils;
	

	public RegenerateGithubOauthToken(OrganizationService organizationService,
								GithubOAuthUtils githubOAuthUtils)
	{
		this.organizationService = organizationService;
		this.githubOAuthUtils = githubOAuthUtils;
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception
	{
		// go GH
		return redirectUserToGithub();

	}

	private String redirectUserToGithub()
	{
		String githubAuthorizeUrl = githubOAuthUtils.createGithubRedirectUrl("RegenerateGithubOauthToken",
				"", getXsrfToken(), organization, getAutoLinking());

		return getRedirect(githubAuthorizeUrl);
	}


	@Override
	protected void doValidation()
	{


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

		return doChangeAccessToken();
	}

	private String doChangeAccessToken()
	{
		try
		{
			organizationService.updateCredentialsAccessToken(Integer.parseInt(organization), accessToken);
			
		} catch (SourceControlException e)
		{
			addErrorMessage("Failed adding the organization: [" + e.getMessage() + "]");
			log.debug("Failed adding the organization: [" + e.getMessage() + "]");
			return INPUT;
		} catch (InvalidCredentialsException e)
		{
			addErrorMessage("Failed adding the organization: [" + e.getMessage() + "]");
			log.debug("Invalid credentials : Failed adding the organization: [" + e.getMessage() + "]");
			return INPUT;
		}

		return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + getXsrfToken());
	}

	private String requestAccessToken()
	{

		return githubOAuthUtils.requestAccessToken(code);
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

}
