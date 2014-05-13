package com.atlassian.jira.plugins.dvcs.activeobjects.v2;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.plugins.dvcs.activeobjects.RepositoryUri;
import com.atlassian.jira.plugins.dvcs.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v1.ProjectMapping;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class To_04_ActiveObjectsV2Migrator implements ActiveObjectsUpgradeTask
{
    private final Logger logger = LoggerFactory.getLogger(To_04_ActiveObjectsV2Migrator.class);

	@Override
    @SuppressWarnings("unchecked")
    public void upgrade(ModelVersion modelVersion, final ActiveObjects activeObjects)
    {
        logger.debug("upgrade [ " + modelVersion + " ]");

        activeObjects.migrate(IssueMapping.class, ProjectMapping.class, com.atlassian.jira.plugins.dvcs.activeobjects.v2.IssueMapping.class, com.atlassian.jira.plugins.dvcs.activeobjects.v2.ProjectMapping.class);
        List<Integer> repositoriesToBeSynchronised = Lists.newArrayList(); 
        
        // get all ProjectMappings from v1 and store them as ProjectMappings v2
        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class);
        for (ProjectMapping projectMapping : projectMappings)
        {
        	String username = projectMapping.getUsername();
        	String password = projectMapping.getPassword();
        	String originalUrl = projectMapping.getRepositoryUri();	// the old version url - may include branch
        	String projectKey = projectMapping.getProjectKey();
        	int repositoryId = projectMapping.getID();

        	String fixedUrl = RepositoryUri.parse(originalUrl).getRepositoryUrl();
        	
        	final Map<String, Object> map = Maps.newHashMap();
			map.put("REPOSITORY_URL", fixedUrl);
			map.put("PROJECT_KEY", projectKey);
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
            {
                map.put("ADMIN_USERNAME", username);
                map.put("ADMIN_PASSWORD", password);
            }
            com.atlassian.jira.plugins.dvcs.activeobjects.v2.ProjectMapping pm = activeObjects.create(com.atlassian.jira.plugins.dvcs.activeobjects.v2.ProjectMapping.class, map);

            if (!fixedUrl.equals(originalUrl))
            {
            	repositoriesToBeSynchronised.add(pm.getID());
            }
            
            // for every ProjectMapping take all associated IssueMappings and migrate from v1 to v2
            IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class, "PROJECT_KEY = ? and REPOSITORY_URI = ?", projectKey, originalUrl);
            for (IssueMapping issueMapping : issueMappings)
            {
            	String node = issueMapping.getNode();
            	String issueId = issueMapping.getIssueId();
            	
            	final Map<String, Object> map2 = Maps.newHashMap();
            	map2.put("REPOSITORY_ID", repositoryId);
            	map2.put("NODE", node);
            	map2.put("ISSUE_ID", issueId);
            	activeObjects.create(com.atlassian.jira.plugins.dvcs.activeobjects.v2.IssueMapping.class, map2);
            }
        }

        logger.debug("completed uri to url migration");
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("4");
    }
}
