package com.atlassian.jira.plugins.dvcs.webwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class DeleteOrganizationAction extends JiraWebActionSupport
{
	private static final long serialVersionUID = 6246027331604675862L;

	final Logger logger = LoggerFactory.getLogger(DeleteOrganizationAction.class);
	
	private String organizationId;

	private final OrganizationService organizationService;

	public DeleteOrganizationAction(OrganizationService organizationService)
    {
		this.organizationService = organizationService;
    }

    @Override
    protected void doValidation()
    {
    	
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
    	
    	organizationService.remove(Integer.parseInt(organizationId));
		
        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
    }


	public String getOrganizationId()
	{
		return organizationId;
	}

	public void setOrganizationId(String organizationId)
	{
		this.organizationId = organizationId;
	}


}