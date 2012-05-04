package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException.UnauthorisedException;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
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

	private final OrganizationService organizationService;

	public AddBitbucketOrganization(OrganizationService organizationService)
	{
		this.organizationService = organizationService;
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception
	{
		try
		{
			Organization newOrganization = new Organization();
			newOrganization.setName(organization);
			newOrganization.setHostUrl(url);
			newOrganization.setDvcsType("bitbucket");
			newOrganization.setCredential(new Credential(adminUsername, adminPassword, null));
			newOrganization.setAutolinkNewRepos(hadAutolinkingChecked());

            organizationService.save(newOrganization);

		} catch (UnauthorisedException e)
		{
			addErrorMessage("Failed adding the organization: [" + e.getMessage() + "]");
			log.debug("Failed adding the organization: [" + e.getMessage() + "]");
			return INPUT;
		} catch (SourceControlException e)
		{
			addErrorMessage("Failed adding the organization: [" + e.getMessage() + "]");
			log.debug("Failed adding the organization: [" + e.getMessage() + "]");
			return INPUT;
		}

		try
		{
			// globalRepositoryManager.setupPostcommitHook(repository);
		} catch (SourceControlException e)
		{
			/*
			 * log.debug("Failed adding postcommit hook: ["+e.getMessage()+"]");
			 * globalRepositoryManager.removeRepository(repository.getId());
			 * addErrorMessage(
			 * "The username/password you provided are invalid. Make sure you entered the correct username/password and that the username has admin rights on "
			 * + url + ".<br/>" + "<br/>Then, try again.<br/><br/> [" +
			 * e.getMessage() + "]");
			 */

			return INPUT;
		}

		// go back to main DVCS configuration page
		return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + getXsrfToken());
	}

	@Override
	protected void doValidation()
	{
		if (StringUtils.isBlank(adminUsername) || StringUtils.isBlank(adminPassword))
		{
			addErrorMessage("Missing credentials.");
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
}
