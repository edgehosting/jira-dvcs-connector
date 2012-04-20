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
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.streams.GlobalFilter;
import com.atlassian.jira.plugins.bitbucket.streams.GlobalFilterQueryWhereClauseBuilder;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A simple mapper that uses ActiveObjects to store the mapping details
 */
public class DefaultRepositoryPersister implements RepositoryPersister
{
    private final Logger log = LoggerFactory.getLogger(DefaultRepositoryPersister.class);
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
                ProjectMapping[] mappings = activeObjects.find(ProjectMapping.class, ProjectMapping.PROJECT_KEY + " = ? AND "
                    + ProjectMapping.REPOSITORY_TYPE + " = ?", projectKey, repositoryType);
                return Lists.newArrayList(mappings);
            }
        });
    }

    @Override
    public ProjectMapping addRepository(String repositoryName, String repositoryType, String projectKey, String repositoryUrl,
        String adminUsername, String adminPassword, String accessToken)
    {
        if (findRepositories(projectKey, repositoryUrl).length > 0)
        {
            throw new SourceControlException("Repository [" + repositoryUrl + "] is already linked to project [" + projectKey + "]");
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(ProjectMapping.REPOSITORY_NAME, repositoryName);
        map.put(ProjectMapping.REPOSITORY_URL, repositoryUrl);
        map.put(ProjectMapping.PROJECT_KEY, projectKey);
        map.put(ProjectMapping.REPOSITORY_TYPE, repositoryType);
        if (StringUtils.isNotBlank(adminUsername) && StringUtils.isNotBlank(adminPassword))
        {
            map.put(ProjectMapping.ADMIN_USERNAME, adminUsername);
            map.put(ProjectMapping.ADMIN_PASSWORD, adminPassword);
        }
        if (StringUtils.isNotBlank(accessToken))
        {
            map.put(ProjectMapping.ACCESS_TOKEN, accessToken);
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
    public ProjectMapping[] findRepositories(String projectKey, String repositoryUrl)
    {
        return activeObjects.find(ProjectMapping.class, ProjectMapping.REPOSITORY_URL + " = ? and "
            + ProjectMapping.PROJECT_KEY + " = ?", repositoryUrl, projectKey);
    }

    @Override
    public void removeAllIssueMappings(final int repositoryId) {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                final IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class, IssueMapping.REPOSITORY_ID+" = ?", repositoryId);

                log.debug("deleting [ {} ] issue mappings [ {} ]", new String[]{String.valueOf(issueMappings.length), String.valueOf(repositoryId)});

                activeObjects.delete(issueMappings);
                return null;
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
                final IssueMapping[] issueMappings = activeObjects.find(IssueMapping.class, IssueMapping.REPOSITORY_ID+" = ?", id);

                log.debug("deleting project mapping [ {} ]", String.valueOf(id));
                log.debug("deleting [ {} ] issue mappings [ {} ]", new String[]{String.valueOf(issueMappings.length), String.valueOf(id)});

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
            	Set<Integer> projectMappings = getProjectMappingsForRepositoryType(repositoryType);
            	if (CollectionUtils.isEmpty(projectMappings)) {
            		return new ArrayList<IssueMapping>();
            	}
            	
                String baseWhereClause = IssueMapping.ISSUE_ID + " = '" + issueId + "'";
                String repositoryIdsFilteringWhereClause = getRepositoryIdsFilteringWhereClause(projectMappings);
                Query query = Query.select().where(baseWhereClause + repositoryIdsFilteringWhereClause);

                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, query);
                return Arrays.asList(mappings);
            }
        });
    }


    /**
     * INFO: AO has problem to do join(), distinct() and limit() in 1 query - AOSqlException on HSQLDB. It is working fine without limit() clause but we need to use limit() in 1 case.
     *
     * @param repositoryType repository type constant {@link com.atlassian.jira.plugins.bitbucket.spi.impl.BitbucketRepositoryManager#BITBUCKET} or
     *                       {@link com.atlassian.jira.plugins.bitbucket.spi.github.impl.GithubRepositoryManager#GITHUB}.
     * @return set of repository ids
     */
    private Set<Integer> getProjectMappingsForRepositoryType(final String repositoryType)
    {
        ProjectMapping[] myProjectMappings = activeObjects.find(ProjectMapping.class, ProjectMapping.REPOSITORY_TYPE + " = ?",
            repositoryType);

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
                log.debug("create issue mapping [ {} ] [ {} - {} ] ", new String[]{issueId, String.valueOf(repositoryId), node});
                // delete existing
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, IssueMapping.ISSUE_ID + " = ? and "
                    + IssueMapping.NODE + " = ?", issueId, node);
                if (ArrayUtils.isNotEmpty(mappings))
                {
                    activeObjects.delete(mappings);
                }
                // add new
                Map<String, Object> map = Maps.newHashMap();
                map.put(IssueMapping.REPOSITORY_ID, repositoryId);
                map.put(IssueMapping.ISSUE_ID, issueId);
                map.put(IssueMapping.NODE, node);
                map.put(IssueMapping.RAW_AUTHOR, changeset.getRawAuthor());
                map.put(IssueMapping.AUTHOR, changeset.getAuthor());
                map.put(IssueMapping.DATE, changeset.getTimestamp());
                map.put(IssueMapping.RAW_NODE, changeset.getRawNode());
                map.put(IssueMapping.BRANCH, changeset.getBranch());
                map.put(IssueMapping.MESSAGE, changeset.getMessage());

                JSONArray parentsJson = new JSONArray();
                for (String parent : changeset.getParents())
                {
                    parentsJson.put(parent);
                }
                map.put(IssueMapping.PARENTS_DATA, parentsJson.toString());

                JSONObject filesDataJson = new JSONObject();
                JSONArray filesJson = new JSONArray();
                try
                {
                    List<ChangesetFile> files = changeset.getFiles();
                    int count = changeset.getAllFileCount();
                    filesDataJson.put("count", count);
                    for (int i = 0; i < Math.min(count, DvcsRepositoryManager.MAX_VISIBLE_FILES); i++)
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
                    map.put(IssueMapping.FILES_DATA, filesDataJson.toString());
                    map.put(IssueMapping.VERSION, IssueMapping.LATEST_VERSION);
                } catch (JSONException e)
                {
                    log.error("Creating files JSON failed!", e);
                }

                return activeObjects.create(IssueMapping.class, map);
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
            	Set<Integer> projectMappings = getProjectMappingsForRepositoryType(repositoryType);
            	if (CollectionUtils.isEmpty(projectMappings)) {
            		return new ArrayList<IssueMapping>();
            	}
            	
                String baseWhereClause = new GlobalFilterQueryWhereClauseBuilder(gf).build();
                String repositoryIdsFilteringWhereClause = getRepositoryIdsFilteringWhereClause(projectMappings);
                Query query = Query.select().where(baseWhereClause + repositoryIdsFilteringWhereClause).limit(count).order(IssueMapping.DATE + " DESC");
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, query);
                return Arrays.asList(mappings);
            }
        });
    }

    private String getRepositoryIdsFilteringWhereClause(Set<Integer> projectMappings)
    {
		StringBuilder sb = new StringBuilder();
		sb.append(" AND " + IssueMapping.REPOSITORY_ID + " in (");
		sb.append(StringUtils.join(projectMappings, ","));
		sb.append(")");
		return sb.toString();
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
    public IssueMapping getIssueMapping(final int repositoryId, final String node)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<IssueMapping>()
        {
            @Override
            public IssueMapping doInTransaction()
            {
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class,IssueMapping.REPOSITORY_ID + " = ? AND " + IssueMapping.NODE + " = ?", repositoryId, node);
                return mappings.length != 0 ? mappings[0] : null;
            }
        });
    }

    /* (non-Javadoc)
     * @see com.atlassian.jira.plugins.bitbucket.api.ChangesetCache#isChangesetInDB(int, java.lang.String)
     */
    @Override
    public boolean isChangesetInDB(int repositoryId, String node)
    {
        return getIssueMapping(repositoryId, node)!=null;
    }

}
