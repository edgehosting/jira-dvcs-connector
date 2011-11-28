package com.atlassian.jira.plugins.bitbucket.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Query;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ChangesetMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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

    @Override
    public List<ProjectMapping> getRepositories(final String projectKey, final String repositoryType)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<ProjectMapping>>()
        {
			@Override
            public List<ProjectMapping> doInTransaction()
            {
                ProjectMapping[] mappings = activeObjects.find(ProjectMapping.class, "PROJECT_KEY = ? AND REPOSITORY_TYPE = ?", projectKey, repositoryType);
                return Lists.newArrayList(mappings);
            }
        });
    }

    @Override
    public ProjectMapping addRepository(String repositoryType, String projectKey, String repositoryUrl, String username, String password, String adminUsername, String adminPassword, String accessToken)
    {
        
        final ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "REPOSITORY_URL = ? and PROJECT_KEY = ?", repositoryUrl, projectKey);
        if (projectMappings.length>0)
        {
            throw new SourceControlException("Repository ["+repositoryUrl+"] is already linked to project ["+projectKey+"]");
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("REPOSITORY_URL", repositoryUrl);
        map.put("PROJECT_KEY", projectKey);
        map.put("REPOSITORY_TYPE", repositoryType);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password))
        {
            map.put("USERNAME", username);
            map.put("PASSWORD", password);
        }
        if (StringUtils.isNotBlank(adminUsername) && StringUtils.isNotBlank(adminPassword))
        {
        	map.put("ADMIN_USERNAME", adminUsername);
        	map.put("ADMIN_PASSWORD", adminPassword);
        }
        if (StringUtils.isNotBlank(accessToken))
        {
            map.put("ACCESS_TOKEN", accessToken);
        }
        return activeObjects.executeInTransaction(new TransactionCallback<ProjectMapping>()
        {
            @Override
            public ProjectMapping doInTransaction()
            {
        		return activeObjects.create(ProjectMapping.class, map);
            }
        });
    }

    @Override
    public void removeRepository(final int id)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                final ProjectMapping projectMapping = activeObjects.get(ProjectMapping.class, id);
                final IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class, "REPOSITORY_ID = ?", id);

                logger.debug("deleting project mapping [ {} ]", String.valueOf(id));
                logger.debug("deleting [ {} ] issue mappings [ {} ]", new String[]{String.valueOf(issueMappings.length), String.valueOf(id)});

                activeObjects.delete(projectMapping);
                activeObjects.delete(issueMappings);
                return null;
            }
        });
    }

    @Override
    public List<IssueMapping> getIssueMappings(final String issueId, final String repositoryType)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<IssueMapping>>()
        {
            @SuppressWarnings("unchecked")
            @Override
            public List<IssueMapping> doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "ISSUE_ID = ?", issueId);
                if (mappings.length==0)
                    return Collections.EMPTY_LIST;
                
                // get list of all project mappings with our type
                ProjectMapping[] myProjectMappings = activeObjects.find(ProjectMapping.class, "REPOSITORY_TYPE = ?", repositoryType);
                
                final Set<Integer> projectMappingsIds = Sets.newHashSet();
                for (int i = 0; i < myProjectMappings.length; i++)
                {
                    projectMappingsIds.add(myProjectMappings[i].getID());
                }
                
                return Lists.newArrayList(CollectionUtils.select(Arrays.asList(mappings), new Predicate()
                {
                    @Override
                    public boolean evaluate(Object o)
                    {
                        IssueMapping issueMapping = (IssueMapping) o;
                        return projectMappingsIds.contains(issueMapping.getRepositoryId());
                    }
                }));
            }
        });
    }    
    
    @Override
    public void addChangeset(final String issueId, final Changeset changeset)
    {
        final int repositoryId = changeset.getRepositoryId();
        final String node = changeset.getNode();

        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                logger.debug("create issue mapping [ {} ] [ {} - {} ] ", new String[]{issueId, String.valueOf(repositoryId), node});
                // delete existing
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "ISSUE_ID = ? and NODE = ?", issueId, node);
                if (ArrayUtils.isNotEmpty(mappings))
				{
					activeObjects.delete(mappings);
				}
                // add new
				Map<String, Object> map = Maps.newHashMap();
				map.put("REPOSITORY_ID", repositoryId);
				map.put("ISSUE_ID", issueId);
				map.put("NODE", node);
                return activeObjects.create(IssueMapping.class, map);
            }
        });
/*
        activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            public ChangesetMapping doInTransaction()
            {
                logger.debug("create changeset mapping [ {} ] [ {} - {} ] ", new String[]{issueId, String.valueOf(repositoryId), node});
                // delete existing
//                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, "REPOSITORY_ID = ? and NODE = ?", repositoryId, node);
//                if (ArrayUtils.isNotEmpty(mappings))
//				{
//					activeObjects.delete(mappings);
//				}
                // add new
				Map<String, Object> map = Maps.newHashMap();
				map.put("REPOSITORY_ID", repositoryId);
				map.put("ISSUE_ID", issueId);
				map.put("NODE", node);
				map.put("RAW_AUTHOR", changeset.getRawAuthor());
				map.put("AUTHOR", changeset.getAuthor());
				map.put("TIMESTAMP", changeset.getTimestamp());
				map.put("RAW_NODE", changeset.getRawNode());
				map.put("BRANCH", changeset.getBranch());
				map.put("MESSAGE", changeset.getMessage());
                return activeObjects.create(ChangesetMapping.class, map);
            }
        });
 */
    }
    @Override
    public List<ChangesetMapping> getLastChangesetMappings(final int count)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, Query.select().limit(count).order("timestamp DESC"));
                return mappings == null ? new ArrayList<ChangesetMapping>() : Lists.newArrayList(mappings);
            }
        });
    }
    
	@Override
    public ProjectMapping getRepository(final int id)
	{
		return activeObjects.executeInTransaction(new TransactionCallback<ProjectMapping>()
		{
			@Override
            public ProjectMapping doInTransaction()
			{
				return activeObjects.get(ProjectMapping.class, id);
			}
		});
	}

}
