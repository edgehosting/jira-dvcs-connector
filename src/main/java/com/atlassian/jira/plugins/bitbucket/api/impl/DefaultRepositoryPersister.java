package com.atlassian.jira.plugins.bitbucket.api.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;

/**
 * A simple mapper that uses ActiveObjects to store the mapping details
 */
public class DefaultRepositoryPersister implements RepositoryPersister
{
    private final Logger logger = LoggerFactory.getLogger(DefaultRepositoryPersister.class);

    private final ActiveObjects activeObjects;

    public DefaultRepositoryPersister(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    public List<ProjectMapping> getRepositories(final String projectKey)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<ProjectMapping>>()
        {
			public List<ProjectMapping> doInTransaction()
            {
                ProjectMapping[] mappings = activeObjects.find(ProjectMapping.class, "PROJECT_KEY = ?", projectKey);
                return Lists.newArrayList(mappings);
            }
        });
    }

    public ProjectMapping addRepository(String projectKey, String repositoryUrl, String username, String password)
    {
        // TODO don't create duplicate mapping
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("REPOSITORY_URI", repositoryUrl);
        map.put("PROJECT_KEY", projectKey);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            map.put("USERNAME", username);
            map.put("PASSWORD", password);
        }
        return activeObjects.executeInTransaction(new TransactionCallback<ProjectMapping>()
        {
            public ProjectMapping doInTransaction()
            {
        		return activeObjects.create(ProjectMapping.class, map);
            }
        });
    }

    // TODO remove by repositoryId
    public void removeRepository(final String projectKey, final String repositoryUrl)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                final ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class,
                        "PROJECT_KEY = ? and REPOSITORY_URI = ?",
                        projectKey, repositoryUrl);

                final IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class,
                        "PROJECT_KEY = ? and REPOSITORY_URI = ?",
                        projectKey, repositoryUrl);

                logger.debug("deleting [ {} ] project mappings [ {} ] [ {} ]",
                        new String[]{String.valueOf(projectMappings.length), projectKey, repositoryUrl});
                logger.debug("deleting [ {} ] issue mappings [ {} ] [ {} ]",
                        new String[]{String.valueOf(issueMappings.length), projectKey, repositoryUrl});

                activeObjects.delete(projectMappings);
                activeObjects.delete(issueMappings);
                return null;
            }
        });
    }

    public List<IssueMapping> getIssueMappings(final String issueId)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<IssueMapping>>()
        {
            public List<IssueMapping> doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "ISSUE_ID = ?", issueId);
                return Lists.newArrayList(mappings);
            }
        });
    }
    
    public void addChangeset(final String issueId, final Changeset changeset)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                String repositoryUrl = changeset.getRepositoryUrl();
                final String projectKey = getProjectKey(issueId);
                final Map<String, Object> map = new HashMap<String, Object>();
                map.put("NODE", changeset.getNode());
                map.put("PROJECT_KEY", projectKey);
                map.put("ISSUE_ID", issueId);
                map.put("REPOSITORY_URI", repositoryUrl);
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class,
                		"ISSUE_ID = ? and NODE = ?",
                		issueId, changeset.getNode());
                logger.debug("create issue mapping [ {} ] [ {} ]", new String[]{projectKey, repositoryUrl});
                if (mappings != null && mappings.length > 0)
                	activeObjects.delete(mappings);
                IssueMapping issueMapping = activeObjects.create(IssueMapping.class, map);
                return issueMapping;
            }
        });
    }

    public void removeChangeset(final String issueId, final Changeset changeset)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class,
                        "ISSUE_ID = ? and NODE = ?",
                        issueId, changeset.getNode());
                activeObjects.delete(mappings);
                return null;
            }
        });
    }
    
    public ProjectMapping getRepository(String projectKey, String repositoryUrl)
    {
        ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class,
                "PROJECT_KEY = ? and REPOSITORY_URI = ?",
                projectKey, repositoryUrl);
        if (projectMappings == null || projectMappings.length != 1)
            throw new SourceControlException("invalid mapping for project [ " + projectKey + " ] to " +
                    "repository [ " + repositoryUrl + " ] was [ " +
                    (projectMappings == null ? "null" : String.valueOf(projectMappings.length)) + " ]");
        return projectMappings[0];
    }

    private String getProjectKey(String issueId)
    {
        // TODO is this safe to do?
        return issueId.substring(0, issueId.lastIndexOf("-"));
    }



}
