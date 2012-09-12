package com.atlassian.jira.plugins.dvcs.webwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class SyncRepositoryListAction extends JiraWebActionSupport
{
	private static final long serialVersionUID = 6246027331604675862L;

	final Logger logger = LoggerFactory.getLogger(SyncRepositoryListAction.class);
	
	private String organizationId;


	private final RepositoryService repositoryService;
	
	private final OrganizationService organizationService;

	public SyncRepositoryListAction(OrganizationService organizationService, RepositoryService repositoryService)
    {
		this.organizationService = organizationService;
		this.repositoryService = repositoryService;
    }

    @Override
    protected void doValidation()
    {
    	
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
    	
    	Organization organization = organizationService.get(Integer.parseInt(organizationId), false);
		
    	repositoryService.syncRepositoryList(organization);

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