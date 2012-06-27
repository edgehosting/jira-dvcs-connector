package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.exception.InvalidCredentialsException;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class UpdateBitbucketCredentials extends JiraWebActionSupport
{
	private static final long serialVersionUID = 5708673582858872703L;

	private final Logger log = LoggerFactory.getLogger(UpdateBitbucketCredentials.class);
	
	private String usernameUp;
	private String organizationId;
	private String adminPasswordUp;

	private final OrganizationService organizationService;

    public UpdateBitbucketCredentials(OrganizationService organizationService)
    {
        this.organizationService = organizationService;
    }

    @Override
    protected void doValidation()
    {

        if (StringUtils.isBlank(adminPasswordUp))
        {
            addErrorMessage("Please provide password.");
        }

        if (StringUtils.isBlank(usernameUp))
        {
            addErrorMessage("Please provide username.");
        }

        try
        {
            Integer.parseInt(organizationId);
        } catch (Exception e)
        {
            addErrorMessage("Invalid request has been sent.");
        }
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
    	
    	try
		{
			organizationService.updateCredentials(Integer.parseInt(organizationId), usernameUp, adminPasswordUp);
		} catch (InvalidCredentialsException e)
		{
			addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
			log.debug("Invalid credentials : Failed adding the account: [" + e.getMessage() + "]");
			return INPUT;
		}

        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + getXsrfToken());
    }


	public String getUsernameUp()
	{
		return usernameUp;
	}

	public void setUsernameUp(String usernameUp)
	{
		this.usernameUp = usernameUp;
	}

	public String getOrganizationId()
	{
		return organizationId;
	}

	public void setOrganizationId(String organizationId)
	{
		this.organizationId = organizationId;
	}

	public String getAdminPasswordUp()
	{
		return adminPasswordUp;
	}

	public void setAdminPasswordUp(String adminPasswordUp)
	{
		this.adminPasswordUp = adminPasswordUp;
	}

}