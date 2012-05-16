package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

public class UpdateBitbucketCredentials extends JiraWebActionSupport
{
	private static final long serialVersionUID = 6246027331604675862L;

	final Logger logger = LoggerFactory.getLogger(UpdateBitbucketCredentials.class);
	
	private String usernameUp;
	private String organizationId;
	private String adminPasswordUp;

    private final ApplicationProperties applicationProperties;
	private final OrganizationService organizationService;

    public UpdateBitbucketCredentials(OrganizationService organizationService, ApplicationProperties applicationProperties)
    {
        this.organizationService = organizationService;
		this.applicationProperties = applicationProperties;
    }

    @Override
    protected void doValidation()
    {
    	
       if (StringUtils.isBlank(adminPasswordUp)) {
    	   addErrorMessage("Please provide password.");
       } 

       if (StringUtils.isBlank(usernameUp)) {
    	   addErrorMessage("Please provide username.");
       } 
    	
       try {
    	   Integer.parseInt(organizationId);
       } catch (Exception e) {
    	   addErrorMessage("Invalid request has been sent.");
       }
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
    	
    	organizationService.updateCredentials(Integer.parseInt(organizationId), usernameUp, adminPasswordUp);

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