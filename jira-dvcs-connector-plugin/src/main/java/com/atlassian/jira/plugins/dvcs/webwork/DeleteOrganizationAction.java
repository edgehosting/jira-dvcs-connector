package com.atlassian.jira.plugins.dvcs.webwork;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Organization;
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
        if (StringUtils.isBlank(organizationId))
        {
            addErrorMessage("No id has been provided, invalid request");
        } else
        {
            Organization integratedAccount = organizationService.findIntegratedAccount();
            if (integratedAccount != null && Integer.valueOf(organizationId).equals(integratedAccount.getId()))
            {
                addErrorMessage("Failed to delete integrated account.");
            }
        }
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        try
        {
            organizationService.remove(Integer.parseInt(organizationId));
        } catch (Exception e)
        {
            logger.error("Failed to remove account " + organizationId, e);
        }

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