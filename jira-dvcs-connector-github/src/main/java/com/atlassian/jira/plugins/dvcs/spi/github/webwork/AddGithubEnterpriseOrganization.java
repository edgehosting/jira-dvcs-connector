package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static com.atlassian.jira.util.UrlValidator.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.DefaultGithubOauthProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubEnterpriseCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class AddGithubEnterpriseOrganization extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = -5043563666764556942L;

    private final Logger log = LoggerFactory.getLogger(AddGithubOrganization.class);

	private String organization;

	private String urlGhe;
	private String oauthClientIdGhe;
	private String oauthSecretGhe;
	private String oauthRequiredGhe;

	// sent by GH on the way back
	private String code;
	private String url;

	private String accessToken = "";

	private final GithubOAuth githubOAuth;
	private final OrganizationService organizationService;
	private final GithubOAuthUtils githubOAuthUtils;

	public AddGithubEnterpriseOrganization(OrganizationService organizationService,
								GithubOAuth githubOAuth,
								GithubOAuthUtils githubOAuthUtils,
								ApplicationProperties applicationProperties)
	{
		this.organizationService = organizationService;
		this.githubOAuth = githubOAuth;
		this.githubOAuthUtils = new GithubOAuthUtils(
		                           DefaultGithubOauthProvider.createEnterpriseProvider(githubOAuth), 
		                           githubOAuth,
		                           applicationProperties);
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception
	{
        if (isOAuthConfigurationRequired())
        {
            configureOAuth();
        }
		
        url = urlGhe;

		// then continue
		return redirectUserToGithub();

	}

	private void configureOAuth()
	{
		githubOAuth.setEnterpriseClient(oauthClientIdGhe, oauthSecretGhe);
	}

	private String redirectUserToGithub()
	{
		String githubAuthorizeUrl = githubOAuthUtils.createGithubRedirectUrl("AddGithubEnterpriseOrganization",
		        urlGhe, getXsrfToken(), organization, getAutoLinking(), getAutoSmartCommits());

		return getRedirect(githubAuthorizeUrl);
	}

	@Override
	protected void doValidation()
	{
	    
	    if (StringUtils.isNotBlank(oauthRequiredGhe))
        {
            if (StringUtils.isBlank(oauthClientIdGhe) || StringUtils.isBlank(oauthSecretGhe))
            {
                addErrorMessage("Missing credentials.");
            }
        }
        
	    if (urlGhe.endsWith("/"))
	    {
	        urlGhe = StringUtils.chop(urlGhe);
	        
	    }
	    if (!isValid(urlGhe))
	    {
	        addErrorMessage("Please provide valid GitHub host URL.");
	    }

        if (StringUtils.isBlank(urlGhe) || StringUtils.isBlank(organization))
        {
            addErrorMessage("Please provide both url and organization parameters.");
        }
//TODO validation of account is disabled because of private mode 
//        AccountInfo accountInfo = organizationService.getAccountInfo(urlGhe, organization);
//        if (accountInfo == null)
//        {
//            addErrorMessage("Invalid user/team account.");
//        }
		
	}
	
    protected boolean isOAuthConfigurationRequired()
    {
        return StringUtils.isNotBlank(oauthRequiredGhe);
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
			newOrganization.setDvcsType(GithubEnterpriseCommunicator.GITHUB_ENTERPRISE);
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
		}

        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
	}

	private String requestAccessToken()
	{
		return githubOAuthUtils.requestAccessToken(url, code);
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

    public String getOauthClientIdGhe()
    {
        return oauthClientIdGhe;
    }

    public void setOauthClientIdGhe(String oauthClientIdGhe)
    {
        this.oauthClientIdGhe = oauthClientIdGhe;
    }

    public String getOauthSecretGhe()
    {
        return oauthSecretGhe;
    }

    public void setOauthSecretGhe(String oauthSecretGhe)
    {
        this.oauthSecretGhe = oauthSecretGhe;
    }

    public String getOauthRequiredGhe()
    {
        return oauthRequiredGhe;
    }

    public void setOauthRequiredGhe(String oauthRequiredGhe)
    {
        this.oauthRequiredGhe = oauthRequiredGhe;
    }

    public String getUrlGhe()
    {
        return urlGhe;
    }

    public void setUrlGhe(String urlGhe)
    {
        this.urlGhe = urlGhe;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }


}