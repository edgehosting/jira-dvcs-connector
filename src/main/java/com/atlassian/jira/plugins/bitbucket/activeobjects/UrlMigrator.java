package com.atlassian.jira.plugins.bitbucket.activeobjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;

// removing branch information from the url
@SuppressWarnings("deprecation")
public class UrlMigrator implements ActiveObjectsUpgradeTask
{
	    private final Logger logger = LoggerFactory.getLogger(UrlMigrator.class);


		public void upgrade(ModelVersion modelVersion, final ActiveObjects activeObjects)
	    {
	        logger.debug("upgrade [ " + modelVersion + " ]");

	        // urls in project mappings
	        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class);
	        for (ProjectMapping projectMapping : projectMappings)
			{
	        	// removing branch information from the url
				RepositoryUri fixedUri = RepositoryUri.parse(projectMapping.getRepositoryUri());
				projectMapping.setRepositoryUri(fixedUri.getOwner()+"/"+fixedUri.getSlug());
				projectMapping.save();
			}
	        
//			Hmm Sync will not work - data are in old tables
//	        // re-synchronise
//	        for (ProjectMapping projectMapping : projectMappings)
//	        {
//	        	new DefaultSourceControlRepository(0, null, null, null, null)
//	        	synchronizer.synchronize(projectMapping.getID());
//	        }
//	        
	        logger.debug("completed url migration");
	    }

	    public ModelVersion getModelVersion()
	    {
	        return ModelVersion.valueOf("2");
	    }
	}
