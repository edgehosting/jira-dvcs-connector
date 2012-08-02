package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.listener.IPluginFeatureDetector;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Webwork action used to configure the bitbucket organizations
 */
public class ConfigureDvcsOrganizations extends JiraWebActionSupport
{
	private static final long serialVersionUID = 8695500426304238626L;

	private final Logger logger = LoggerFactory.getLogger(ConfigureDvcsOrganizations.class);

	private String postCommitRepositoryType;
	private final FeatureManager featureManager;
	private final OrganizationService organizationService;
	private final DvcsCommunicatorProvider communicatorProvider;

    private final IPluginFeatureDetector featuresDetector;

	public ConfigureDvcsOrganizations(OrganizationService organizationService,
			FeatureManager featureManager, DvcsCommunicatorProvider communicatorProvider, IPluginFeatureDetector featuresDetector)
	{
		this.organizationService = organizationService;
		this.communicatorProvider = communicatorProvider;
		this.featureManager = featureManager;
        this.featuresDetector = featuresDetector;
	}

	@Override
	protected void doValidation()
	{
	}

	@Override
	@RequiresXsrfCheck
	protected String doExecute() throws Exception
	{
		logger.debug("Configure orgazniation default action.");

		return INPUT;
	}

	public Organization[] loadOrganizations()
	{

		List<Organization> allOrganizations = organizationService.getAll(true);
		return allOrganizations.toArray(new Organization[]{});
		
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
	
	public boolean isUserInvitationsEnabled() {
	    
	    return featuresDetector.isUserInvitationsEnabled();
	    
	}
	
	public boolean isGithubOauthRequired() {
		return !communicatorProvider.getCommunicator("github").isOauthConfigured();
	}
}
