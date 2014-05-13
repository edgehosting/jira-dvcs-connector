package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsConfigPageShownAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationsManagerImpl;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Webwork action used to configure the bitbucket organizations
 */
public class ConfigureDvcsOrganizations extends JiraWebActionSupport
{
    static final String DEFAULT_SOURCE = CommonDvcsConfigurationAction.DEFAULT_SOURCE;
    private final Logger logger = LoggerFactory.getLogger(ConfigureDvcsOrganizations.class);

    private String postCommitRepositoryType;
    private String source;

    private final EventPublisher eventPublisher;
    private final FeatureManager featureManager;
    private final OrganizationService organizationService;
    private final PluginFeatureDetector featuresDetector;
    private final InvalidOrganizationManager invalidOrganizationsManager;
    private final OAuthStore oAuthStore;

    public ConfigureDvcsOrganizations(EventPublisher eventPublisher, OrganizationService organizationService, FeatureManager featureManager,
            PluginFeatureDetector featuresDetector, PluginSettingsFactory pluginSettingsFactory, OAuthStore oAuthStore)
    {
        this.eventPublisher = eventPublisher;
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
        eventPublisher.publish(new DvcsConfigPageShownAnalyticsEvent(getSourceOrDefault()));
        return INPUT;
    }

    public String doDefault() throws Exception
    {
        return doExecute();
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

    public boolean isIntegratedAccount(Organization org)
    {
        return org.isIntegratedAccount();
    }

    public OAuthStore getOAuthStore()
    {
        return oAuthStore;
    }

    public String getSource()
    {
        return source;
    }

    public String getSourceOrDefault()
    {
        return StringUtils.defaultIfEmpty(source, DEFAULT_SOURCE);
    }

    public void setSource(String source)
    {
        this.source = source;
    }
}
