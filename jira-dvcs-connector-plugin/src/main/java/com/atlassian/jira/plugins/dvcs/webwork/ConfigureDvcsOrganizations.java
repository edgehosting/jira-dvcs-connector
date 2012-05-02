package com.atlassian.jira.plugins.dvcs.webwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Webwork action used to configure the bitbucket organizations
 */
public class ConfigureDvcsOrganizations extends JiraWebActionSupport
{
	private static final long serialVersionUID = 8695500426304238626L;

	private final Logger logger = LoggerFactory.getLogger(ConfigureDvcsOrganizations.class);

	private String mode = "";
	private String repositoryUrl = "";
	private String postCommitUrl = "";
	private String projectKey = "";
	private String nextAction = "";
	private String addedRepositoryId = "";
	private int repositoryId;
	private final String baseUrl;

	private String postCommitRepositoryType;
	private final FeatureManager featureManager;

	private final OrganizationService organizationService;

	public ConfigureDvcsOrganizations(OrganizationService organizationService,
			ApplicationProperties applicationProperties, FeatureManager featureManager)
	{
		this.organizationService = organizationService;
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
		logger.debug("configure orgazniation [ " + nextAction + " ]");
		
		return INPUT;
	}

	public void setMode(String value)
	{
		mode = value;
	}

	public String getMode()
	{
		return mode;
	}

	public void setRepositoryUrl(String value)
	{
		repositoryUrl = value;
	}

	public String getRepositoryUrl()
	{
		return repositoryUrl;
	}

	public void setPostCommitUrl(String value)
	{
		postCommitUrl = value;
	}

	public String getPostCommitUrl()
	{
		return postCommitUrl;
	}

	public void setProjectKey(String value)
	{
		projectKey = value;
	}

	public String getProjectKey()
	{
		return projectKey;
	}

	public void setNextAction(String value)
	{
		nextAction = value;
	}

	public String getNextAction()
	{
		return nextAction;
	}

	public int getRepositoryId()
	{
		return repositoryId;
	}

	public void setRepositoryId(int repositoryId)
	{
		this.repositoryId = repositoryId;
	}

	public String getAddedRepositoryId()
	{
		return addedRepositoryId;
	}

	public void setAddedRepositoryId(String addedRepositoryId)
	{
		this.addedRepositoryId = addedRepositoryId;
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
}
