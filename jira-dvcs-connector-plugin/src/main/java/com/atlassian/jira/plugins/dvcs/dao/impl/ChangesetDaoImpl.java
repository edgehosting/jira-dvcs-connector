package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class ChangesetDaoImpl implements ChangesetDao
{
    private static final Logger log = LoggerFactory.getLogger(ChangesetDaoImpl.class);

    private final ActiveObjects activeObjects;
    private final ChangesetTransformer transformer = new ChangesetTransformer();

    public ChangesetDaoImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    protected Changeset transform(ChangesetMapping changesetMapping)
    {
        return transformer.transform(changesetMapping);
    }

    protected List<Changeset> transform(List<ChangesetMapping> changesetMappings)
    {
        final Collection<Changeset> changesets = Collections2.transform(changesetMappings,
                new Function<ChangesetMapping, Changeset>()
                {
                    @Override
                    public Changeset apply(ChangesetMapping changesetMapping)
                    {
                        return transform(changesetMapping);
                    }
                });

        return new ArrayList<Changeset>(changesets);
    }


    @Override
    public void removeAllInRepository(final int repositoryId)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                final ChangesetMapping[] changesetMappings = activeObjects.find(ChangesetMapping.class, ChangesetMapping.REPOSITORY_ID+" = ?", repositoryId);

                log.debug("deleting [ {} ] changesets from repository with id = [ {} ]", new String[]{String.valueOf(changesetMappings.length), String.valueOf(repositoryId)});

                activeObjects.delete(changesetMappings);
                return null;
            }
        });

    }

    @Override
    public Changeset save(final Changeset changeset)
    {
        final ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {

            @Override
            public ChangesetMapping doInTransaction()
            {
                // delete existing
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, 
                        ChangesetMapping.REPOSITORY_ID + " = ? and " + 
                        ChangesetMapping.NODE + " = ? and " + 
                        ChangesetMapping.ISSUE_KEY + " = ? ", 
                        changeset.getRepositoryId(), changeset.getNode(), changeset.getIssueKey());

                if (ArrayUtils.isNotEmpty(mappings))
                {
                    activeObjects.delete(mappings);
                }

                // add new
                ChangesetMapping om;

                final Map<String, Object> map = new HashMap<String, Object>();
                map.put(ChangesetMapping.REPOSITORY_ID, changeset.getRepositoryId());
                map.put(ChangesetMapping.ISSUE_KEY, changeset.getIssueKey());
                map.put(ChangesetMapping.PROJECT_KEY, parseProjectKey(changeset.getIssueKey()));
                map.put(ChangesetMapping.NODE, changeset.getNode());
                map.put(ChangesetMapping.RAW_AUTHOR, changeset.getRawAuthor());
                map.put(ChangesetMapping.AUTHOR, changeset.getAuthor());
                map.put(ChangesetMapping.DATE, changeset.getDate());
                map.put(ChangesetMapping.RAW_NODE, changeset.getRawNode());
                map.put(ChangesetMapping.BRANCH, changeset.getBranch());
                map.put(ChangesetMapping.MESSAGE, changeset.getMessage());
                JSONArray parentsJson = new JSONArray();
                for (String parent : changeset.getParents())
                {
                    parentsJson.put(parent);
                }
                map.put(ChangesetMapping.PARENTS_DATA, parentsJson.toString());

                JSONObject filesDataJson = new JSONObject();
                JSONArray filesJson = new JSONArray();
                try
                {
                    List<ChangesetFile> files = changeset.getFiles();
                    int count = changeset.getAllFileCount();
                    filesDataJson.put("count", count);
                    for (int i = 0; i < Math.min(count, Changeset.MAX_VISIBLE_FILES); i++)
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
                    map.put(ChangesetMapping.FILES_DATA, filesDataJson.toString());
                } catch (JSONException e)
                {
                    log.error("Creating files JSON failed!", e);
                }

                map.put(ChangesetMapping.VERSION, ChangesetMapping.LATEST_VERSION);

                om = activeObjects.create(ChangesetMapping.class, map);

                return om;
            }
        });
        
        activeObjects.flush(changesetMapping);

        return transform(changesetMapping);
        
    }

    private String parseProjectKey(String issueKey) {
        return issueKey.substring(0, issueKey.indexOf("-"));
    }

    @Override
    public Changeset getByNode(final int repositoryId, final String changesetNode)
    {
        final ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class,ChangesetMapping.REPOSITORY_ID + " = ? AND " + ChangesetMapping.NODE + " = ?", repositoryId, changesetNode);
                return mappings.length != 0 ? mappings[0] : null;
            }
        });

        return transform(changesetMapping);

    }

    @Override
    public List<Changeset> getByIssueKey(final String issueKey)
    {
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, ChangesetMapping.ISSUE_KEY + " = ?", issueKey);
                return Arrays.asList(mappings);
            }
        });

        return transform(changesetMappings);
    }

    @Override
    public List<Changeset> getLatestChangesets(final int maxResults, final GlobalFilter gf)
    {
        if (maxResults <= 0)
        {
            return Collections.emptyList();
        }
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                String baseWhereClause = new GlobalFilterQueryWhereClauseBuilder(gf).build();
                Query query = Query.select().where(baseWhereClause).limit(maxResults).order(ChangesetMapping.DATE + " DESC");
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, query);
                return Arrays.asList(mappings);
            }
        });

        return transform(changesetMappings);
    }
}