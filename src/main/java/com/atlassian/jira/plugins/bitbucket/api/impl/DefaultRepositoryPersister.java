package com.atlassian.jira.plugins.bitbucket.api.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.streams.GlobalFilter;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            @Override
            public List<IssueMapping> doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "ISSUE_ID = ?", issueId);
                return filterMappingsByRepositoryType(mappings, repositoryType);
            }
        });
    }
    
    /**
     * There is a bug in AO that prevents doing select on multiple tables. This is a workaround for this.
     * TODO find the AO bug id. 
     * TODO maybe this can be optimised using SQL: "and repositoryId in (?)" 
     * @param mappings
     * @param repositoryType
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<IssueMapping> filterMappingsByRepositoryType(IssueMapping[] mappings, String repositoryType)
    {
                if (mappings.length==0)
            return Collections.emptyList();
                // get list of all project mappings with our type
        final Set<Integer> projectMappingsIds = getProjectMappingsForRepositoryType(repositoryType);
                
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

    private Set<Integer> getProjectMappingsForRepositoryType(final String repositoryType)
    {
        ProjectMapping[] myProjectMappings = activeObjects.find(ProjectMapping.class, "REPOSITORY_TYPE = ?", repositoryType);
        
        final Set<Integer> projectMappingsIds = Sets.newHashSet();
        for (ProjectMapping myProjectMapping : myProjectMappings)
        {
            projectMappingsIds.add(myProjectMapping.getID());
        }
        return projectMappingsIds;
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
				// TODO create constants for column names in IssueMappings.class
				map.put("REPOSITORY_ID", repositoryId);
				map.put("ISSUE_ID", issueId);
				map.put("NODE", node);
				map.put("RAW_AUTHOR", changeset.getRawAuthor());
				map.put("AUTHOR", changeset.getAuthor());
				map.put("DATE", changeset.getTimestamp());
				map.put("RAW_NODE", changeset.getRawNode());
				map.put("BRANCH", changeset.getBranch());
				map.put("MESSAGE", changeset.getMessage());

                JSONArray parentsJson = new JSONArray();
                for (String parent : changeset.getParents())
                {
                    parentsJson.put(parent);
                }
                map.put("PARENTS_DATA", parentsJson.toString());

                JSONObject filesDataJson = new JSONObject();
                JSONArray filesJson = new JSONArray();
                try
                {
                    List<ChangesetFile> files = changeset.getFiles();
                    int count = files.size();
                    filesDataJson.put("count", count);
                    for (int i=0; i< Math.min(count, DvcsRepositoryManager.MAX_VISIBLE_FILES); i++)
                    {
                        ChangesetFile changesetFile = files.get(i);
                        JSONObject fileJson = new JSONObject();
                        fileJson.put("filename", changesetFile.getFile());
                        fileJson.put("status", changesetFile.getFileAction().getAction());
                        fileJson.put("additions", changesetFile.getAdditions());
                        fileJson.put("deletions", changesetFile.getDeletions());

                        filesJson.put(fileJson);
                    }
                    filesDataJson.put("files", filesJson);

                    map.put("FILES_DATA", filesDataJson.toString());

                    map.put("VERSION", IssueMapping.LATEST_VERSION);
                } catch (JSONException e)
                {
                    logger.error("Creating files JSON failed!", e);
                }

                IssueMapping create = activeObjects.create(IssueMapping.class, map);
                return create;
            }
        });
    }
    @Override
    public List<IssueMapping> getLatestIssueMappings(final int count, final GlobalFilter gf, final String repositoryType)
    {
        if (count <= 0)
    {
            return Collections.emptyList();
        }
        return activeObjects.executeInTransaction(new TransactionCallback<List<IssueMapping>>()
        {
            @Override
            public List<IssueMapping> doInTransaction()
            {
                String whereClauseSb = createQueryWhereClause(gf);
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, Query.select().where(whereClauseSb).limit(count).order("DATE DESC"));
                return filterMappingsByRepositoryType(mappings, repositoryType);
            }
        });
    }
    
    // TODO move to separate class and add unit tests
    private String createQueryWhereClause(GlobalFilter gf)
    {
        StringBuilder whereClauseProjectsSb = new StringBuilder();
        StringBuilder whereClauseIssueKyesSb = new StringBuilder();
        StringBuilder whereClauseUsersSb = new StringBuilder();
        if (gf != null)
        {
            if (gf.getInProjects() != null && gf.getInProjects().iterator().hasNext())
            {
                for (String projectKey : gf.getInProjects())
                {
                    if (StringUtils.isBlank(projectKey))
                    {
                        continue;
                    }
                    if (whereClauseProjectsSb.length() != 0)
                    {
                        whereClauseProjectsSb.append(" OR ");
                    }
                    whereClauseProjectsSb.append("ISSUE_ID like '").append(projectKey).append("-%' ");
                }
            }
            if (gf.getNotInProjects() != null && gf.getNotInProjects().iterator().hasNext())
            {
                for (String projectKey : gf.getNotInProjects())
                {
                    if (StringUtils.isBlank(projectKey))
                    {
                        continue;
                    }
                    if (whereClauseProjectsSb.length() != 0)
                    {
                        whereClauseProjectsSb.append(" AND ");
                    }
                    whereClauseProjectsSb.append("ISSUE_ID not like '").append(projectKey).append("-%' ");
                }
            }

            if (gf.getInIssues() != null && gf.getInIssues().iterator().hasNext())
            {
                for (String issueKey : gf.getInIssues())
                {
                    if (StringUtils.isBlank(issueKey))
                    {
                        continue;
                    }
                    if (whereClauseIssueKyesSb.length() != 0)
                    {
                        whereClauseIssueKyesSb.append(" OR ");
                    }
                    whereClauseIssueKyesSb.append("ISSUE_ID like '").append(issueKey.toUpperCase()).append("' ");
                }
            }
            if (gf.getNotInIssues() != null && gf.getNotInIssues().iterator().hasNext())
            {
                for (String issueKey : gf.getNotInIssues())
                {
                    if (StringUtils.isBlank(issueKey))
                    {
                        continue;
                    }
                    if (whereClauseIssueKyesSb.length() != 0)
                    {
                        whereClauseIssueKyesSb.append(" AND ");
                    }
                    whereClauseIssueKyesSb.append("ISSUE_ID not like '").append(issueKey.toUpperCase()).append("' ");
                }
            }

            if (gf.getInUsers() != null && gf.getInUsers().iterator().hasNext())
            {
                for (String username : gf.getInUsers())
                {
                    if (StringUtils.isBlank(username))
                    {
                        continue;
                    }
                    if (whereClauseUsersSb.length() != 0)
                    {
                        whereClauseUsersSb.append(" OR ");
                    }
                    whereClauseUsersSb.append("AUTHOR like '").append(username).append("' ");
                }
            }
            if (gf.getNotInUsers() != null && gf.getNotInUsers().iterator().hasNext())
            {
                for (String username : gf.getNotInUsers())
                {
                    if (StringUtils.isBlank(username))
                    {
                        continue;
                    }
                    if (whereClauseUsersSb.length() != 0)
                    {
                        whereClauseUsersSb.append(" AND ");
                    }
                    whereClauseUsersSb.append("AUTHOR not like '").append(username).append("' ");
                }
            }
        }
        StringBuilder whereClauseSb = new StringBuilder();
        if (whereClauseProjectsSb.length() != 0)
        {
            whereClauseSb.append("(").append(whereClauseProjectsSb.toString()).append(")");
        }
        if (whereClauseIssueKyesSb.length() != 0)
        {
            if (whereClauseSb.length() != 0)
            {
                whereClauseSb.append(" AND ");
            }
            whereClauseSb.append("(").append(whereClauseIssueKyesSb.toString()).append(")");
        }
        if (whereClauseUsersSb.length() != 0)
        {
            if (whereClauseSb.length() != 0)
            {
                whereClauseSb.append(" AND ");
            }
            whereClauseSb.append("(").append(whereClauseUsersSb.toString()).append(")");
        }

        // if no filter applyied than "no" where clause should be used
        if (whereClauseSb.length() == 0)
        {
            whereClauseSb.append(" true ");
        }
        return whereClauseSb.toString();
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

    @Override
    public IssueMapping getIssueMapping(final String node)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<IssueMapping>()
        {
            @Override
            public IssueMapping doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, "NODE = ?", node);
                return mappings.length != 0 ? mappings[0] : null;
            }
        });
    }

}
