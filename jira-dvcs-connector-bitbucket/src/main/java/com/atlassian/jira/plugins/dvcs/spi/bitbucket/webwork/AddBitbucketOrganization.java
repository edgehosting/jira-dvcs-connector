package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import com.atlassian.jira.plugins.dvcs.exception.InvalidCredentialsException;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.webwork.CommonDvcsConfigurationAction;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		} catch (SourceControlException.UnauthorisedException e)
		{
			addErrorMessage("Failed adding the organization: [" + e.getMessage() + "]");
			log.debug("Failed adding the organization: [" + e.getMessage() + "]");
			return INPUT;
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
		if (StringUtils.isBlank(adminUsername))
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
