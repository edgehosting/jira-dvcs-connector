package com.atlassian.jira.plugins.bitbucket.activeobjects.v2;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.google.common.collect.Maps;

@SuppressWarnings("deprecation")
public class ActiveObjectsV2Migrator implements ActiveObjectsUpgradeTask
{
    private final Logger logger = LoggerFactory.getLogger(ActiveObjectsV2Migrator.class);


	public void upgrade(ModelVersion modelVersion, final ActiveObjects activeObjects)
    {
        logger.debug("upgrade [ " + modelVersion + " ]");

        activeObjects.migrate(IssueMapping.class, ProjectMapping.class, IssueMapping2.class, ProjectMapping2.class);

        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class);
        for (ProjectMapping projectMapping : projectMappings)
        {
        	String username = projectMapping.getUsername();
        	String password = projectMapping.getPassword();
        	String repositoryUrl = projectMapping.getRepositoryUri();
        	String projectKey = projectMapping.getProjectKey();
        	int repositoryId = projectMapping.getID();

        	final Map<String, Object> map = Maps.newHashMap();
			map.put("REPOSITORY_URL", repositoryUrl);
			map.put("PROJECT_KEY", projectKey);
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
            {
                map.put("USERNAME", username);
                map.put("PASSWORD", password);
            }
            activeObjects.create(ProjectMapping2.class, map);

            IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class, "PROJECT_KEY = ? and REPOSITORY_URI = ?", projectKey, repositoryUrl);
            for (IssueMapping issueMapping : issueMappings)
            {
            	String node = issueMapping.getNode();
            	String issueId = issueMapping.getIssueId();
             
            	final Map<String, Object> map2 = Maps.newHashMap();
            	map2.put("REPOSITORY_ID", repositoryId);
				map2.put("NODE", node);
				map2.put("ISSUE_ID", issueId);
            	activeObjects.create(IssueMapping2.class, map2);
            }
        }
        
        logger.debug("completed uri to url migration");
    }

    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("4");
    }
}
