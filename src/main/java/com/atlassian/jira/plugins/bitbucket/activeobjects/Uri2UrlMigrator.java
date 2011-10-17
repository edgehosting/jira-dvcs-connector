package com.atlassian.jira.plugins.bitbucket.activeobjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;

@SuppressWarnings("deprecation")
public class Uri2UrlMigrator implements ActiveObjectsUpgradeTask
{
    private final Logger logger = LoggerFactory.getLogger(Uri2UrlMigrator.class);


	public void upgrade(ModelVersion modelVersion, final ActiveObjects activeObjects)
    {
        logger.debug("upgrade [ " + modelVersion + " ]");

        // urls in project mappings
        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class);
        for (ProjectMapping projectMapping : projectMappings)
        {
        	RepositoryUri uri = RepositoryUri.parse(projectMapping.getRepositoryUri());
        	projectMapping.setRepositoryUri(uri.getRepositoryUrl());
        	projectMapping.save();
        }
        
        IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class);
        for (IssueMapping issueMapping : issueMappings)
		{
			RepositoryUri uri = RepositoryUri.parse(issueMapping.getRepositoryUri());
			issueMapping.setRepositoryUri(uri.getRepositoryUrl());
			issueMapping.save();
		}
 
        logger.debug("completed uri to url migration");
    }

    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("3");
    }
}
