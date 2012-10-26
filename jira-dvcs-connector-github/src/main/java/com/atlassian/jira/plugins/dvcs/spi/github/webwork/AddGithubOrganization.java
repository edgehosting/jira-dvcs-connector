package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static com.atlassian.jira.util.UrlValidator.isValid;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.InvalidCredentialsException;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.UrlValidator;

public class AddGithubOrganization extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = -5043563666764556942L;

    private final Logger log = LoggerFactory.getLogger(AddGithubOrganization.class);

	private String url;
	private String organization;

	private String oauthHost;
	private String oauthClientId;
	private String oauthSecret;
	private String oauthRequired;

	// sent by GH on the way back
	private String code;

	private String accessToken = "";

	private final GithubOAuth githubOAuth;
	private final OrganizationService organizationService;
	private final GithubOAuthUtils githubOAuthUtils;
	

	public AddGithubOrganization(OrganizationService organizationService,
								GithubOAuth githubOAuth,
								GithubOAuthUtils githubOAuthUtils)
	{
		this.organizationService = organizationService;
		this.githubOAuth = githubOAuth;
		this.githubOAuthUtils = githubOAuthUtils;
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
		return redirectUserToGithub();

	}

	private void configureOAuth()
	{
		githubOAuth.setClient(oauthHost, oauthClientId, oauthSecret);
	}

	private String redirectUserToGithub()
	{
		String githubAuthorizeUrl = githubOAuthUtils.createGithubRedirectUrl("AddGithubOrganization",
				url, getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());

		return getRedirect(githubAuthorizeUrl);
	}

	@Override
	protected void doValidation()
	{
		boolean validGitHubHost = true;
		if (StringUtils.isNotBlank(oauthRequired))
		{
			if (StringUtils.isBlank(oauthHost))
	        {
				oauthHost = "https://github.com";
	        }

			validGitHubHost = isValid(oauthHost);
			if (!validGitHubHost)
			{
				addErrorMessage("Please provide a valid url for the GitHub host");
			}
			
			if (StringUtils.isBlank(oauthClientId) || StringUtils.isBlank(oauthSecret))
			{
				addErrorMessage("Missing credentials.");
			}
			
			url = oauthHost;
		}
		else
		{
			url = githubOAuth.getHost();
		}
		
		if (StringUtils.isBlank(organization))
		{
			addErrorMessage("Please provide an organization.");
		}

		if (validGitHubHost) {
			AccountInfo accountInfo = organizationService.getAccountInfo(url, organization);
	        if (accountInfo == null)
	        {
	            addErrorMessage("Invalid user/team account.");
	        }
		}
	}
	
    protected boolean isOAuthConfigurationRequired()
    {
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
		
		} catch (Exception e) {
		    addErrorMessage("Error obtain access token.");
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
			newOrganization.setSmartcommitsOnNewRepos(hadAutolinkingChecked());
			
			organizationService.save(newOrganization);
			
		} catch (SourceControlException e)
		{		
			addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
			log.debug("Failed adding the account: [" + e.getMessage() + "]");
			e.printStackTrace();
			return INPUT;
		} catch (InvalidCredentialsException e)
		{
			addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
			log.debug("Invalid credentials : Failed adding the account: [" + e.getMessage() + "]");
			return INPUT;
		}

        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
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

	public String getOauthHost()
	{
		return oauthHost;
	}

	public void setOauthHost(String oauthHost)
	{
		this.oauthHost = oauthHost;
	}

}
