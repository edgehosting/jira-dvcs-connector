package com.atlassian.jira.plugins.dvcs.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;

public abstract class RegenerateOauthTokenAction extends CommonDvcsConfigurationAction
{
    private static final long serialVersionUID = -5518449007071758982L;
    private final Logger log = LoggerFactory.getLogger(RegenerateOauthTokenAction.class);

    protected String organization; // in the meaning of id TODO rename to organizationId

    protected final OrganizationService organizationService;
    protected final RepositoryService repositoryService;

    protected final OAuthStore oAuthStore;

    public RegenerateOauthTokenAction(OrganizationService organizationService, RepositoryService repositoryService, OAuthStore oAuthStore)
    {
        this.organizationService = organizationService;
        this.repositoryService = repositoryService;
        this.oAuthStore = oAuthStore;
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
        String accessToken = getAccessToken();
        try
        {
            organizationService.updateCredentialsAccessToken(Integer.parseInt(organization), accessToken);

        } catch (SourceControlException e)
        {
            addErrorMessage("Failed adding the account: [" + e.getMessage() + "]");
            log.debug("Failed adding the account: [" + e.getMessage() + "]");
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

    public String getOrganization()
    {
        return organization;
    }

    public void setOrganization(String organization)
    {
        this.organization = organization;
    }

}
