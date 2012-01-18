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
import com.atlassian.jira.plugins.bitbucket.streams.GlobalFilterQueryWhereClauseBuilder;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
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
    public ProjectMapping addRepository(String repositoryName, String repositoryType, String projectKey, String repositoryUrl, String username, String password, String adminUsername, String adminPassword, String accessToken)
    {

        final ProjectMapping[] projectMappings = activeObjects.find(ProjectMapping.class, "REPOSITORY_URL = ? and PROJECT_KEY = ?", repositoryUrl, projectKey);
        if (projectMappings.length > 0)
        {
            throw new SourceControlException("Repository [" + repositoryUrl + "] is already linked to project [" + projectKey + "]");
        }
        final Map<String, Object> map = new HashMap<String, Object>();
        // TODO make constants in ProjectMappings.java
        map.put("REPOSITORY_NAME", repositoryName);
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
                String baseWhereClause = "ISSUE_ID = '" + issueId + "'";
                String repositoryIdsFilteringWhereClause = getRepositoryIdsFilteringWhereClause(repositoryType);
                Query query = Query.select().where(baseWhereClause + repositoryIdsFilteringWhereClause);

                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, query);
                return Arrays.asList(mappings);
            }
        });
    }


    /**
     * INFO: AO has problem to do join(), distinct() and limit() in 1 query - AOSqlException on HSQLDB. It is working fine without limit() clause but we need to use limit() in 1 case.
     *
     * @param repositoryType repository type constant {@link com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryManager#BITBUCKET} or
     *                       {@link com.atlassian.jira.plugins.bitbucket.spi.github.impl.GithubRepositoryManager#GITHUB}.
     * @return set of repository ids
     */
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
                map.put(IssueMapping.COLUMN_REPOSITORY_ID, repositoryId);
                map.put(IssueMapping.COLUMN_ISSUE_ID, issueId);
                map.put(IssueMapping.COLUMN_NODE, node);
                map.put(IssueMapping.COLUMN_RAW_AUTHOR, changeset.getRawAuthor());
                map.put(IssueMapping.COLUMN_AUTHOR, changeset.getAuthor());
                map.put(IssueMapping.COLUMN_DATE, changeset.getTimestamp());
                map.put(IssueMapping.COLUMN_RAW_NODE, changeset.getRawNode());
                map.put(IssueMapping.COLUMN_BRANCH, changeset.getBranch());
                map.put(IssueMapping.COLUMN_MESSAGE, changeset.getMessage());

                JSONArray parentsJson = new JSONArray();
                for (String parent : changeset.getParents())
                {
                    parentsJson.put(parent);
                }
                map.put(IssueMapping.COLUMN_PARENTS_DATA, parentsJson.toString());

                JSONObject filesDataJson = new JSONObject();
                JSONArray filesJson = new JSONArray();
                try
                {
                    List<ChangesetFile> files = changeset.getFiles();
                    int count = files.size();
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

                    map.put(IssueMapping.COLUMN_FILES_DATA, filesDataJson.toString());

                    map.put(IssueMapping.COLUMN_VERSION, IssueMapping.LATEST_VERSION);
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
                String baseWhereClause = new GlobalFilterQueryWhereClauseBuilder(gf).build();
                String repositoryIdsFilteringWhereClause = getRepositoryIdsFilteringWhereClause(repositoryType);
                Query query = Query.select().where(baseWhereClause + repositoryIdsFilteringWhereClause).limit(count).order("DATE DESC");
                IssueMapping[] mappings = activeObjects.find(IssueMapping.class, query);
                return Arrays.asList(mappings);
            }
        });
    }

    private String getRepositoryIdsFilteringWhereClause(String repositoryType)
    {
        StringBuilder sb = new StringBuilder();
        Set<Integer> ids = getProjectMappingsForRepositoryType(repositoryType);
        if (CollectionUtils.isNotEmpty(ids))
        {
            sb.append(" AND REPOSITORY_ID in (");
            sb.append(StringUtils.join(ids, ","));
            sb.append(")");
        } else
        {
            sb.append(" AND FALSE ");
        }
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
