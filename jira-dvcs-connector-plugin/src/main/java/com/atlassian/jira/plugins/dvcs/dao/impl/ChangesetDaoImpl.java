package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.Table;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.ActiveObjectsUtils;
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

    @SuppressWarnings("unchecked")
    protected List<Changeset> transform(List<ChangesetMapping> changesetMappings)
    {
        return (List<Changeset>) CollectionUtils.collect(changesetMappings, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                ChangesetMapping changesetMapping = (ChangesetMapping) input;
                
                return ChangesetDaoImpl.this.transform(changesetMapping);
            }
        });
    }

    @Override
    public void removeAllInRepository(final int repositoryId)
    {

        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {

                // delete association repo - changesets
                Query query = Query.select().where(RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId);
                log.debug("deleting repo - changesets associations from RepoToChangeset with id = [ {} ]", new String[]{String.valueOf(repositoryId)});
                ActiveObjectsUtils.delete(activeObjects, RepositoryToChangesetMapping.class, query);

                // delete orphaned changesets
                query = Query.select().where(
                        "ID not in  (select \"" + RepositoryToChangesetMapping.CHANGESET_ID + "\" from \"AO_E8B6CC_REPO_TO_CHANGESET\")");
                log.debug("deleting orphaned changesets");
                ActiveObjectsUtils.delete(activeObjects, ChangesetMapping.class, query);

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
//                        ChangesetMapping.REPOSITORY_ID + " = ? and " +
                        ChangesetMapping.NODE + " = ? and " + 
                        ChangesetMapping.ISSUE_KEY + " = ? ",
//                        changeset.getRepositoryId(),
                        changeset.getNode(),
                        changeset.getIssueKey());



                // add new
                ChangesetMapping chm = null;

                if (ArrayUtils.isEmpty(mappings))
                {
                    chm = activeObjects.create(ChangesetMapping.class);
                    chm = activeObjects.find(ChangesetMapping.class, "ID = ?", chm.getID())[0];
                } else
                {
                    if (mappings.length > 1)
                    {
                        log.debug("We have in DB more than one changest with same nodeID.");
                        // todo: do something useful
                    }
                    chm = mappings[0];
                }


                // we need to remove null characters '\u0000' because PostgreSQL cannot store String values with such
                // characters
                // todo: remove NULL Chars before call setters
//                final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
//                map.put(ChangesetMapping.REPOSITORY_ID, changeset.getRepositoryId());
                chm.setIssueKey(changeset.getIssueKey());
                chm.setProjectKey(parseProjectKey(changeset.getIssueKey()));
                chm.setNode(changeset.getNode());
                chm.setRawAuthor(changeset.getRawAuthor());
                chm.setAuthor(changeset.getAuthor());
                chm.setDate(changeset.getDate());
                chm.setRawNode(changeset.getRawNode());
                chm.setBranch(changeset.getBranch());
                chm.setMessage(changeset.getMessage());
                chm.setAuthorEmail(changeset.getAuthorEmail());
                chm.setSmartcommitAvailable(changeset.isSmartcommitAvaliable());
//                map.put(ChangesetMapping.ISSUE_KEY, changeset.getIssueKey());
//                map.put(ChangesetMapping.PROJECT_KEY, parseProjectKey(changeset.getIssueKey()));
//                map.put(ChangesetMapping.NODE, changeset.getNode());
//                map.put(ChangesetMapping.RAW_AUTHOR, changeset.getRawAuthor());
//                map.put(ChangesetMapping.AUTHOR, changeset.getAuthor());
//                map.put(ChangesetMapping.DATE, changeset.getDate());
//                map.put(ChangesetMapping.RAW_NODE, changeset.getRawNode());
//                map.put(ChangesetMapping.BRANCH, changeset.getBranch());
//                map.put(ChangesetMapping.MESSAGE, changeset.getMessage());
//                map.put(ChangesetMapping.AUTHOR_EMAIL, changeset.getAuthorEmail());
//                map.put(ChangesetMapping.SMARTCOMMIT_AVAILABLE, changeset.isSmartcommitAvaliable());

                JSONArray parentsJson = new JSONArray();
                for (String parent : changeset.getParents())
                {
                    parentsJson.put(parent);
                }
                
                String parentsData = parentsJson.toString();
                if (parentsData.length() > 255)
                {
                    parentsData = ChangesetMapping.TOO_MANY_PARENTS;
                }
                chm.setParentsData(parentsData);
//                map.put(ChangesetMapping.PARENTS_DATA, parentsData);

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
                    chm.setFilesData(filesDataJson.toString());
//                    map.put(ChangesetMapping.FILES_DATA, filesDataJson.toString());

                } catch (JSONException e)
                {
                    log.error("Creating files JSON failed!", e);
                }

                chm.setVersion(ChangesetMapping.LATEST_VERSION);
//                map.put(ChangesetMapping.VERSION, ChangesetMapping.LATEST_VERSION);


                chm.save();

                associateRepositoryToChangeset(changeset.getRepositoryId(), chm);


                return chm;
            }
        });

        return transform(changesetMapping);
        
    }

    private void associateRepositoryToChangeset(int repositoryId, ChangesetMapping changesetMapping)
    {

        RepositoryToChangesetMapping[] mappings = activeObjects.find(RepositoryToChangesetMapping.class,
                RepositoryToChangesetMapping.REPOSITORY_ID + " = ? and " +
                RepositoryToChangesetMapping.CHANGESET_ID + " = ? ",
                repositoryId,
                changesetMapping);

        if (ArrayUtils.isEmpty(mappings))
        {
            final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();

            map.put(RepositoryToChangesetMapping.REPOSITORY_ID, repositoryId);
            map.put(RepositoryToChangesetMapping.CHANGESET_ID, changesetMapping);

            activeObjects.create(RepositoryToChangesetMapping.class, map);
        }
    }

    public static String parseProjectKey(String issueKey)
    {
        return issueKey.substring(0, issueKey.indexOf("-"));
    }

    @Override
    public Changeset getByNode(final int repositoryId, final String changesetNode)
    {
        // todo: mfa
//        final ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
//        {
//            @Override
//            public ChangesetMapping doInTransaction()
//            {
//                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class,ChangesetMapping.REPOSITORY_ID + " = ? AND " + ChangesetMapping.NODE + " = ?", repositoryId, changesetNode);
//                return mappings.length != 0 ? mappings[0] : null;
//            }
//        });
//
//        return transform(changesetMapping);

        return null;
    }

    @Override
    public List<Changeset> getByIssueKey(final String issueKey)
    {
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class,
                                                                 Query.select().where(ChangesetMapping.ISSUE_KEY + " = ?", issueKey)
                                                                               .order(ChangesetMapping.DATE));

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

	@Override
	public void forEachLatestChangesetsAvailableForSmartcommitDo(final ForEachChangesetClosure closure)
	{
		Query query = createLatestChangesetsAvailableForSmartcommitQuery();
		activeObjects.stream(ChangesetMapping.class, query, new EntityStreamCallback<ChangesetMapping, Integer>() {
			@Override
			public void onRowRead(ChangesetMapping mapping)
			{
				closure.execute(transform(mapping));
			}
		});
	}

	private Query createLatestChangesetsAvailableForSmartcommitQuery()
	{
		return Query.select("*").where(ChangesetMapping.SMARTCOMMIT_AVAILABLE + " = ? ", Boolean.TRUE)
		.order(ChangesetMapping.DATE + " DESC");
	}

    @Override
    public Set<String> findReferencedProjects(int repositoryId)
    {
        // todo: mfa
        Query query = Query.select(ChangesetMapping.PROJECT_KEY);
//        Query query = Query.select(ChangesetMapping.PROJECT_KEY).distinct()
//                .where(ChangesetMapping.REPOSITORY_ID + " = ? ", repositoryId).order(ChangesetMapping.PROJECT_KEY);

        final Set<String> projectKeys = new HashSet<String>();
        activeObjects.stream(ProjectKey.class, query, new EntityStreamCallback<ProjectKey, String>()
        {
            @Override
            public void onRowRead(ProjectKey mapping)
            {
                projectKeys.add(mapping.getProjectKey());
            }
        });

        return projectKeys;
    }
    
    @Table("ChangesetMapping")
    static interface ProjectKey extends RawEntity<String> {
        
        @PrimaryKey(ChangesetMapping.PROJECT_KEY)
        String getProjectKey();
        
        void setProjectKey();
    }

	@Override
	public void markSmartcommitAvailability(int id, boolean available)
	{
		final ChangesetMapping changesetMapping = activeObjects.get(ChangesetMapping.class, id);
		changesetMapping.setSmartcommitAvailable(available);
		activeObjects.executeInTransaction(new TransactionCallback<Void>()
		{
			@Override
			public Void doInTransaction()
			{
				changesetMapping.save();
				return null;
			}
		});
	}


}
