package com.atlassian.jira.plugins.bitbucket.activeobjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.bitbucket.Synchronizer;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;

public class UrlMigrator implements ActiveObjectsUpgradeTask
{
	    private final Logger logger = LoggerFactory.getLogger(UrlMigrator.class);
		private final Synchronizer synchronizer;

		public UrlMigrator(Synchronizer synchronizer)
		{
			this.synchronizer = synchronizer;
		}

		public void upgrade(ModelVersion modelVersion, final ActiveObjects activeObjects)
	    {
	        logger.debug("upgrade [ " + modelVersion + " ]");

	        // TODO check if this needs to be in transaction
	        // urls in project mappings
	        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class);
	        for (ProjectMapping projectMapping : projectMappings)
			{
	        	// removing branch information from the url
				RepositoryUri fixedUri = RepositoryUri.parse(projectMapping.getRepositoryUri());
				projectMapping.setRepositoryUri(fixedUri.getRepositoryUri());
				projectMapping.save();
			}

	        // re-synchronise
	        for (ProjectMapping projectMapping : projectMappings)
	        {
	        	synchronizer.synchronize(projectMapping.getProjectKey(), RepositoryUri.parse(projectMapping.getRepositoryUri()).getRepositoryUrl());
	        }
	        
	        logger.debug("completed url migration");
	    }

	    public ModelVersion getModelVersion()
	    {
	        return ModelVersion.valueOf("2");
	    }
	}
