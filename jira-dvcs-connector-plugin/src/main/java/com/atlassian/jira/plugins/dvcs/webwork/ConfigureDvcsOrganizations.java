package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
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
		logger.debug("Configure orgazniation default action.");

		return INPUT;
	}

	public Organization[] loadOrganizations()
	{

		Repository mockRepo = new Repository(124, 12, "bitbucket", "sentinel-core-components",
				"Sentinel Core Components", new Date(), true, null);

		Organization mockOrg = new Organization();
		mockOrg.setId(12);
		mockOrg.setHostUrl("https://bitbucket.com");
		mockOrg.setDvcsType("bitbucket");
		mockOrg.setName("sentinel");
		mockOrg.setAutolinkNewRepos(true);
		mockOrg.setRepositories(new Repository[] { mockRepo });
		
		Repository mockRepo2 = new Repository(124, 12, "github", "blogging-samples",
				"Blogging Samples Repo", new Date(), false, null);
		
		Organization mockOrg2 = new Organization();
		mockOrg2.setId(12);
		mockOrg2.setHostUrl("https://github.com");
		mockOrg2.setDvcsType("github");
		mockOrg2.setName("samuel");
		mockOrg2.setAutolinkNewRepos(false);
		mockOrg2.setRepositories(new Repository[] { mockRepo2 });

		Organization[] mocks = new Organization[] { mockOrg, mockOrg2  };

		//return mocks;
		 return organizationService.getAll().toArray(new Organization []{});
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
