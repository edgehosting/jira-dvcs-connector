package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.listener.PluginFeatureDetector;
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

    private final PluginFeatureDetector featuresDetector;

	public ConfigureDvcsOrganizations(OrganizationService organizationService,
			FeatureManager featureManager, DvcsCommunicatorProvider communicatorProvider, PluginFeatureDetector featuresDetector)
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
		
		sort(allOrganizations);
		
		return allOrganizations.toArray(new Organization[]{});
		
	}

    private void sort(List<Organization> allOrganizations)
    {
        // TODO add javadoc, this is to keep integrated account on the top of the list
        Collections.sort(allOrganizations, new Comparator<Organization>()
        {
            @Override
            public int compare(Organization org1, Organization org2)
            {
                if (StringUtils.isNotBlank((org1.getCredential().getOauthKey())))
                {
                    return -1;
                } else if (StringUtils.isNotBlank((org2.getCredential().getOauthKey())))
                {
                    {
                        return 1;
                    }

                }
                return 0;
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
	
	public boolean isUserInvitationsEnabled() {
	    
	    return featuresDetector.isUserInvitationsEnabled();
	    
	}
	
	public boolean isGithubOauthRequired() {
		return !communicatorProvider.getCommunicator("github").isOauthConfigured();
	}
	
	public boolean isBitbucketOauthRequired() {
		return !communicatorProvider.getCommunicator("bitbucket").isOauthConfigured();
	}
	
	public boolean isIntegratedAccount(Organization org) {
	    return org.isIntegratedAccount();
	}
}
