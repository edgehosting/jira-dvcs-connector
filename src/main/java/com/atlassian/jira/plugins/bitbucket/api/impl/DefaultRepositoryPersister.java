package com.atlassian.jira.plugins.bitbucket.api.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping2;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping2;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

    public List<ProjectMapping2> getRepositories(final String projectKey)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<ProjectMapping2>>()
        {
			public List<ProjectMapping2> doInTransaction()
            {
                ProjectMapping2[] mappings = activeObjects.find(ProjectMapping2.class, "PROJECT_KEY = ?", projectKey);
                return Lists.newArrayList(mappings);
            }
        });
    }

    public ProjectMapping2 addRepository(String projectKey, String repositoryUrl, String username, String password)
    {
        // TODO don't create duplicate mapping
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("REPOSITORY_URL", repositoryUrl);
        map.put("PROJECT_KEY", projectKey);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            map.put("USERNAME", username);
            map.put("PASSWORD", password);
        }
        return activeObjects.executeInTransaction(new TransactionCallback<ProjectMapping2>()
        {
            public ProjectMapping2 doInTransaction()
            {
        		return activeObjects.create(ProjectMapping2.class, map);
            }
        });
    }

    public void removeRepository(final int id)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            public Object doInTransaction()
            {
                final ProjectMapping2 projectMapping = activeObjects.get(ProjectMapping2.class, id);
                final IssueMapping2[] issueMappings = activeObjects.find(IssueMapping2.class, "REPOSITORY_ID = ?", id);

                logger.debug("deleting project mapping [ {} ]", String.valueOf(id));
                logger.debug("deleting [ {} ] issue mappings [ {} ]", new String[]{String.valueOf(issueMappings.length), String.valueOf(id)});

                activeObjects.delete(projectMapping);
                activeObjects.delete(issueMappings);
                return null;
            }
        });
    }

    public List<IssueMapping2> getIssueMappings(final String issueId)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<IssueMapping2>>()
        {
            public List<IssueMapping2> doInTransaction()
            {
                IssueMapping2[] mappings = activeObjects.find(IssueMapping2.class, "ISSUE_ID = ?", issueId);
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
                int repositoryId = changeset.getRepositoryId();
                logger.debug("create issue mapping [ {} ] [ {} - {} ] ", new String[]{issueId, changeset.getNode(), changeset.getMessage()});
                // delete existing
                IssueMapping2[] mappings = activeObjects.find(IssueMapping2.class, "ISSUE_ID = ? and NODE = ?", issueId, changeset.getNode());
                if (ArrayUtils.isNotEmpty(mappings))
				{
					activeObjects.delete(mappings);
				}
                // add new
				Map<String, Object> map = Maps.newHashMap();
				map.put("REPOSITORY_ID", repositoryId);
				map.put("ISSUE_ID", issueId);
				map.put("NODE", changeset.getNode());
                return activeObjects.create(IssueMapping2.class, map);
            }
        });
    }
    
	public ProjectMapping2 getRepository(final int id)
	{
		return activeObjects.executeInTransaction(new TransactionCallback<ProjectMapping2>()
		{
			public ProjectMapping2 doInTransaction()
			{
				return activeObjects.get(ProjectMapping2.class, id);
			}
		});
	}

}
