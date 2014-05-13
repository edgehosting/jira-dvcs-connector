package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RegenerateOauthTokenAction extends CommonDvcsConfigurationAction
{
    private final Logger log = LoggerFactory.getLogger(RegenerateOauthTokenAction.class);

    protected String organization; // in the meaning of id TODO rename to organizationId

    protected final OrganizationService organizationService;
    protected final RepositoryService repositoryService;

    public RegenerateOauthTokenAction(EventPublisher eventPublisher,
            OrganizationService organizationService, RepositoryService repositoryService)
    {
        super(eventPublisher);
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        return redirectUserToGrantAccess();
    }

    protected abstract String redirectUserToGrantAccess();

    public String doFinish()
    {
        return doChangeAccessToken();
    }

    protected abstract String getAccessToken();

    private String doChangeAccessToken()
    {
        try
        {
            String accessToken = getAccessToken();
            organizationService.updateCredentialsAccessToken(Integer.parseInt(organization), accessToken);

        } catch (SourceControlException e)
        {
            addErrorMessage("Cannot regenerate OAuth access token: [" + e.getMessage() + "]");
            log.debug("Cannot regenerate OAuth access token: [" + e.getMessage() + "]");
            return INPUT;
        }

        // refreshing list of repositories after regenerating OAuth access token
        Organization org = organizationService.get(Integer.parseInt(organization), false);
        try
        {
            repositoryService.syncRepositoryList(org);
        } catch (SourceControlException e)
        {
            log.error("Could not refresh repository list", e);
        }

        return getRedirect("ConfigureDvcsOrganizations.jspa?atl_token=" + CustomStringUtils.encode(getXsrfToken()));
    }

    public String getOrganizationName()
    {
        return organizationService.get(Integer.parseInt(organization),false).getName();
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
