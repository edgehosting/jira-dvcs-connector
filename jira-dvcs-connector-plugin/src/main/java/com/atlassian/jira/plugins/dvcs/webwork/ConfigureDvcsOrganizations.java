package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Webwork action used to configure the bitbucket organizations
 */
public class ConfigureDvcsOrganizations extends JiraWebActionSupport
{
	private static final long serialVersionUID = 8695500426304238626L;

	private final Logger logger = LoggerFactory.getLogger(ConfigureDvcsOrganizations.class);

	private final String baseUrl;

	private String postCommitRepositoryType;
	private final FeatureManager featureManager;

	private final OrganizationService organizationService;

	private final DvcsCommunicatorProvider communicatorProvider;

	public ConfigureDvcsOrganizations(OrganizationService organizationService,
			ApplicationProperties applicationProperties, FeatureManager featureManager, DvcsCommunicatorProvider communicatorProvider)
	{
		this.organizationService = organizationService;
		this.communicatorProvider = communicatorProvider;
		baseUrl = applicationProperties.getBaseUrl();
		this.featureManager = featureManager;
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
	
	public boolean isGithubOauthRequired() {
		return !communicatorProvider.getCommunicator("github").isOauthConfigured();
	}
}
