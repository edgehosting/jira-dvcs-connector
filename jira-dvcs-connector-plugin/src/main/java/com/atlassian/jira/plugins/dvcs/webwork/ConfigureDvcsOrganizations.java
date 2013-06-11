package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.conditions.GithubEnterpriseEnabledCondition;
import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationsManagerImpl;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Webwork action used to configure the bitbucket organizations
 */
public class ConfigureDvcsOrganizations extends JiraWebActionSupport
{
    private final Logger logger = LoggerFactory.getLogger(ConfigureDvcsOrganizations.class);

    private String postCommitRepositoryType;
    private final FeatureManager featureManager;
    private final OrganizationService organizationService;
    private final PluginFeatureDetector featuresDetector;
    private final InvalidOrganizationManager invalidOrganizationsManager;
    private final OAuthStore oAuthStore;

    public ConfigureDvcsOrganizations(OrganizationService organizationService, FeatureManager featureManager,
            PluginFeatureDetector featuresDetector, PluginSettingsFactory pluginSettingsFactory, OAuthStore oAuthStore)
    {
        this.organizationService = organizationService;
        this.featureManager = featureManager;
        this.featuresDetector = featuresDetector;
        this.oAuthStore = oAuthStore;
        this.invalidOrganizationsManager = new InvalidOrganizationsManagerImpl(pluginSettingsFactory);
    }

    @Override
    protected void doValidation()
    {
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        logger.debug("Configure organization default action.");
        return INPUT;
    }

    public Organization[] loadOrganizations()
    {
        List<Organization> allOrganizations = organizationService.getAll(true);
        sort(allOrganizations);
        return allOrganizations.toArray(new Organization[] {});
    }

    public boolean isInvalidOrganization(Organization organization)
    {
        return !invalidOrganizationsManager.isOrganizationValid(organization.getId());
    }

    /**
     * Custom sorting of organizations - integrated accounts are displayed on top.
     *
     * @param allOrganizations
     */
    private void sort(List<Organization> allOrganizations)
    {
        Collections.sort(allOrganizations, new Comparator<Organization>()
        {
            @Override
            public int compare(Organization org1, Organization org2)
            {
                // integrated accounts has precedence
                if (org1.isIntegratedAccount() && !org2.isIntegratedAccount())
                {
                    return -1;

                } else if (!org1.isIntegratedAccount() && org2.isIntegratedAccount())
                {
                    return +1;

                } else
                {
                    // by default compares via name
                    return org1.getName().toLowerCase().compareTo(org2.getName().toLowerCase());
                }

            }
        });
    }

    public String getPostCommitRepositoryType()
    {
        return postCommitRepositoryType;
    }

    public void setPostCommitRepositoryType(String postCommitRepositoryType)
    {
        this.postCommitRepositoryType = postCommitRepositoryType;
    }

    public boolean isOnDemandLicense()
    {
        return featureManager.isEnabled(CoreFeatures.ON_DEMAND);
    }

    public boolean isUserInvitationsEnabled()
    {
        return featuresDetector.isUserInvitationsEnabled();
    }

    public boolean isGithubEnterpriseEnabled()
    {
        return GithubEnterpriseEnabledCondition.isGitHubEnterpriseEnabled();
    }

    public boolean isIntegratedAccount(Organization org)
    {
        return org.isIntegratedAccount();
    }

    public OAuthStore getOAuthStore()
    {
        return oAuthStore;
    }
}
