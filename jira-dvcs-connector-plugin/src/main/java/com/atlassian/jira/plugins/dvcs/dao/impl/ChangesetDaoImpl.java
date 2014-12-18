package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingFunction;
import com.atlassian.jira.plugins.dvcs.dao.impl.GlobalFilterQueryWhereClauseBuilder.SqlAndParams;
import com.atlassian.jira.plugins.dvcs.dao.impl.querydsl.ChangesetQDSL;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.ImmutableSet;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.Table;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.dao.impl.DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY;
import static com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils.ID;
import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class ChangesetDaoImpl implements ChangesetDao
{
    private static final Logger log = LoggerFactory.getLogger(ChangesetDaoImpl.class);

    private final ActiveObjects activeObjects;
    private final ChangesetTransformer transformer;
    private final QueryHelper queryHelper;
    private final ChangesetQDSL changesetQDSL;
    private final QDSLFeatureHelper qdslFeatureHelper;

    @Autowired
    public ChangesetDaoImpl(@ComponentImport ActiveObjects activeObjects, QueryHelper queryHelper,
            ChangesetQDSL changesetQDSL, final QDSLFeatureHelper qdslFeatureHelper)
    {
        this.activeObjects = checkNotNull(activeObjects);
        this.queryHelper = queryHelper;
        this.transformer = new ChangesetTransformer(activeObjects, this);
        this.changesetQDSL = changesetQDSL;
        this.qdslFeatureHelper = qdslFeatureHelper;
    }

    private Changeset transform(ChangesetMapping changesetMapping, int defaultRepositoryId)
    {
        return transform(changesetMapping, defaultRepositoryId, null);
    }

    private Changeset transform(ChangesetMapping changesetMapping, int defaultRepositoryId, String dvcsType)
    {
        return transformer.transform(changesetMapping, defaultRepositoryId, dvcsType);
    }

    private List<Changeset> transform(List<ChangesetMapping> changesetMappings)
    {
        return transform(changesetMappings, 0, null);
    }

    private List<Changeset> transform(List<ChangesetMapping> changesetMappings, String dvcsType)
    {
        return transform(changesetMappings, 0, dvcsType);
    }

    private List<Changeset> transform(List<ChangesetMapping> changesetMappings, int defaultRepositoryId, String dvcsType)
    {
        List<Changeset> changesets = new ArrayList<Changeset>();

        for (ChangesetMapping changesetMapping : changesetMappings)
        {
            Changeset changeset = transform(changesetMapping, defaultRepositoryId, dvcsType);
            if (changeset != null)
            {
                changesets.add(changeset);
            }
        }

        return changesets;
    }

    @Override
    public void removeAllInRepository(final int repositoryId)
    {
        long startTime = System.currentTimeMillis();
        activeObjects.executeInTransaction(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                // todo: transaction: plugin use SalTransactionManager and there is empty implementation of TransactionSynchronisationManager.
                // todo: Therefore there are only entityCache transactions. No DB transactions.

                // delete association repo - changesets
                Query query = Query.select().where(RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId);
                log.debug("deleting repo - changesets associations from RepoToChangeset with id = [ {} ]", new String[] { String.valueOf(repositoryId) });
                ActiveObjectsUtils.delete(activeObjects, RepositoryToChangesetMapping.class, query);

                // delete association issues - changeset
                query = Query.select()
                        .alias(IssueToChangesetMapping.class, "i2c")
                        .where("not exists " +
                                "(select 1 from " + queryHelper.getSqlTableName(RepositoryToChangesetMapping.TABLE_NAME) + " where i2c." +
                                queryHelper.getSqlColumnName(IssueToChangesetMapping.CHANGESET_ID) + " = " + queryHelper.getSqlColumnName(RepositoryToChangesetMapping.CHANGESET_ID) + ")");


                log.debug("deleting orphaned issue-changeset associations");
                ActiveObjectsUtils.delete(activeObjects, IssueToChangesetMapping.class, query);


                // delete orphaned changesets
                query = Query.select()
                        .alias(ChangesetMapping.class, "c")
                        .where("not exists " +
                                "(select 1 from " + queryHelper.getSqlTableName(RepositoryToChangesetMapping.TABLE_NAME) + " where c." +
                                queryHelper.getSqlColumnName(ID) + " = " + queryHelper.getSqlColumnName(RepositoryToChangesetMapping.CHANGESET_ID) + ")");

                log.debug("deleting orphaned changesets");
                ActiveObjectsUtils.delete(activeObjects, ChangesetMapping.class, query);

                return null;
            }
        });
        log.debug("Changesets in repository {} were deleted in {} ms", repositoryId, System.currentTimeMillis() - startTime);
    }

    @Override
    public Changeset create(final Changeset changeset, final Set<String> extractedIssues)
    {
        createOrAssociate(changeset, extractedIssues);

        return changeset;
    }

    @Override
    public boolean createOrAssociate(final Changeset changeset, final Set<String> extractedIssues)
    {
        final MutableBoolean wasCreated = new MutableBoolean(false);
        ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                ChangesetMapping chm = getChangesetMapping(changeset);
                if (chm == null)
                {
                    chm = activeObjects.create(ChangesetMapping.class);
                    fillProperties(changeset, chm);
                    chm.save();
                    wasCreated.setValue(true);
                }

                associateRepositoryToChangeset(chm, changeset.getRepositoryId());
                if (extractedIssues != null)
                {
                    associateIssuesToChangeset(chm, extractedIssues);
                }

                return chm;
            }
        });

        changeset.setId(changesetMapping.getID());
        return wasCreated.booleanValue();
    }

    @Override
    public Changeset update(final Changeset changeset)
    {
        activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                ChangesetMapping chm = getChangesetMapping(changeset);
                if (chm != null)
                {
                    fillProperties(changeset, chm);
                    chm.save();
                }
                else
                {
                    log.warn("Changest with node {} is not exists.", changeset.getNode());
                }
                return chm;
            }
        });

        return changeset;
    }

    @Override
    public Changeset migrateFilesData(final Changeset changeset, final String dvcsType)
    {
        activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                ChangesetMapping chm = getChangesetMapping(changeset);
                if (chm != null)
                {
                    transformer.migrateChangesetFileData(chm, dvcsType, changeset);
                }
                else
                {
                    log.warn("Changest with node {} is not exists.", changeset.getNode());
                }
                return chm;
            }
        });
        return changeset;
    }

    private ChangesetMapping getChangesetMapping(Changeset changeset)
    {
        // A Query is little bit more complicated, but:

        // 1. previous implementation did not properly fill RAW_NODE, in some cases it is null, in some other cases it is empty string
        String hasRawNode = "( " + ChangesetMapping.RAW_NODE + " is not null AND " + ChangesetMapping.RAW_NODE + " != '') ";

        // 2. Latest implementation is using full RAW_NODE, but not all records contains it!
        String matchRawNode = ChangesetMapping.RAW_NODE + " = ? ";

        // 3. Previous implementation has used NODE, but it is mix in some cases it is short version, in some cases it is full version
        //  translate NODE like '123%' to NODE >= '123' AND NODE < '123g' for Postgresql. MySQL and MS SQL Server could do it implicitly
        String matchNode = " ( " + ChangesetMapping.NODE + " >= ? AND " + ChangesetMapping.NODE + " < ? )";

        String shortNode = changeset.getNode().substring(0, 12);
        String shortNodeNext = changeset.getNode().substring(0, 12) + 'g';
        ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, "(" + hasRawNode + " AND " + matchRawNode + " ) OR ( NOT "
                + hasRawNode + " AND " + matchNode + " ) ", changeset.getRawNode(), shortNode, shortNodeNext);

        if (mappings.length > 1)
        {
            log.warn("More changesets with same Node. Same changesets count: {}, Node: {}, Repository: {}", new Object[] { mappings.length,
                    changeset.getNode(), changeset.getRepositoryId() });
        }
        return (ArrayUtils.isNotEmpty(mappings)) ? mappings[0] : null;
    }

    private void fillProperties(Changeset changeset, ChangesetMapping chm)
    {
        // we need to remove null characters '\u0000' because PostgreSQL cannot store String values with such
        // characters
        // todo: remove NULL Chars before call setters
        chm.setNode(changeset.getNode());
        chm.setRawAuthor(ActiveObjectsUtils.stripToLimit(changeset.getRawAuthor(), 255));
        chm.setAuthor(changeset.getAuthor());
        chm.setDate(changeset.getDate());
        chm.setRawNode(changeset.getRawNode());
        chm.setBranch(ActiveObjectsUtils.stripToLimit(changeset.getBranch(), 255));
        chm.setMessage(changeset.getMessage());
        chm.setAuthorEmail(ActiveObjectsUtils.stripToLimit(changeset.getAuthorEmail(), 255));
        chm.setSmartcommitAvailable(changeset.isSmartcommitAvaliable());

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

        // cleaning up deprecated files data
        chm.setFilesData(null);
        chm.setFileCount(changeset.getAllFileCount());
        chm.setFileDetailsJson(ChangesetFileDetails.toJSON(changeset.getFileDetails()));
        chm.setVersion(ChangesetMapping.LATEST_VERSION);
        chm.save();
    }

    private void associateIssuesToChangeset(ChangesetMapping changesetMapping, Set<String> extractedIssues)
    {
        // remove all assoc issues-changeset
        Query query = Query.select().where(IssueToChangesetMapping.CHANGESET_ID + " = ? ", changesetMapping);
        ActiveObjectsUtils.delete(activeObjects, IssueToChangesetMapping.class, query);

        // insert all
        for (String extractedIssue : extractedIssues)
        {
            final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
            map.put(IssueToChangesetMapping.ISSUE_KEY, extractedIssue);
            map.put(IssueToChangesetMapping.PROJECT_KEY, parseProjectKey(extractedIssue));
            map.put(IssueToChangesetMapping.CHANGESET_ID, changesetMapping.getID());
            activeObjects.create(IssueToChangesetMapping.class, map);
        }
    }

    private void associateRepositoryToChangeset(ChangesetMapping changesetMapping, int repositoryId)
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
        final ChangesetMapping changesetMapping = activeObjects.executeInTransaction(new TransactionCallback<ChangesetMapping>()
        {
            @Override
            public ChangesetMapping doInTransaction()
            {
                Query query = Query.select()
                        .from(ChangesetMapping.class)
                        .alias(ChangesetMapping.class, "chm")
                        .alias(RepositoryToChangesetMapping.class, "rtchm")
                        .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                        .where("chm." + ChangesetMapping.NODE + " = ? AND rtchm." + RepositoryToChangesetMapping.REPOSITORY_ID + " = ? ", changesetNode, repositoryId);


                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, query);
                return mappings.length != 0 ? mappings[0] : null;
            }
        });

        final Changeset changeset = transform(changesetMapping, repositoryId);

        return changeset;
    }

    @Override
    public List<Changeset> getByIssueKey(final Iterable<String> issueKeys, final boolean newestFirst)
    {
        return getByIssueKey(issueKeys, null, newestFirst);
    }

    @Override
    public List<Changeset> getByIssueKey(Iterable<String> issueKeys, @Nullable String dvcsType, final boolean newestFirst)
    {
        if (qdslFeatureHelper.isRetrievalUsingQDSLEnabled())
        {
            return changesetQDSL.getByIssueKey(issueKeys, dvcsType, newestFirst);
        }
        else
        {
            List<ChangesetMapping> changesetMappings = getChangesetMappingsByIssueKey(issueKeys, newestFirst);
            return transform(changesetMappings, dvcsType);
        }
    }

    @Override
    public List<Changeset> getByRepository(final int repositoryId)
    {
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class,
                        Query.select()
                                .alias(ChangesetMapping.class, "CHANGESET")
                                .alias(RepositoryToChangesetMapping.class, "REPO")
                                .join(RepositoryToChangesetMapping.class, "CHANGESET.ID = REPO." + RepositoryToChangesetMapping.CHANGESET_ID)
                                .where("REPO.ID = ?", repositoryId));

                return Arrays.asList(mappings);
            }
        });

        return transform(changesetMappings);
    }

    private List<ChangesetMapping> getChangesetMappingsByIssueKey(Iterable<String> issueKeys, final boolean newestFirst)
    {
        final GlobalFilter gf = new GlobalFilter();
        gf.setInIssues(issueKeys);
        final SqlAndParams baseWhereClause = new GlobalFilterQueryWhereClauseBuilder(gf).build();
        final List<ChangesetMapping> changesetMappings = activeObjects.executeInTransaction(new TransactionCallback<List<ChangesetMapping>>()
        {
            @Override
            public List<ChangesetMapping> doInTransaction()
            {
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class,
                        Query.select()
                                .alias(ChangesetMapping.class, "CHANGESET")
                                .alias(IssueToChangesetMapping.class, "ISSUE")
                                .join(IssueToChangesetMapping.class, "CHANGESET.ID = ISSUE." + IssueToChangesetMapping.CHANGESET_ID)
                                .where(baseWhereClause.getSql(), baseWhereClause.getParams())
                                .order(ChangesetMapping.DATE + (newestFirst ? " DESC" : " ASC"))
                                .limit(MAXIMUM_ENTITIES_PER_ISSUE_KEY));

                return Arrays.asList(mappings);
            }
        });

        return changesetMappings;
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
                SqlAndParams baseWhereClause = new GlobalFilterQueryWhereClauseBuilder(gf).build();
                Query query = Query.select()
                        .alias(ChangesetMapping.class, "CHANGESET")
                        .alias(IssueToChangesetMapping.class, "ISSUE")
                        .join(IssueToChangesetMapping.class, "CHANGESET.ID = ISSUE." + IssueToChangesetMapping.CHANGESET_ID)
                        .where(baseWhereClause.getSql(), baseWhereClause.getParams()).limit(maxResults).order(ChangesetMapping.DATE + " DESC");
                ChangesetMapping[] mappings = activeObjects.find(ChangesetMapping.class, query);
                return Arrays.asList(mappings);
            }
        });

        return transform(changesetMappings);
    }

    @Override
    public void forEachLatestChangesetsAvailableForSmartcommitDo(final int repositoryId, final String[] columns, final ForEachChangesetClosure closure)
    {
        Query query = createLatestChangesetsAvailableForSmartcommitQuery(repositoryId, columns);
        activeObjects.stream(ChangesetMapping.class, query, new EntityStreamCallback<ChangesetMapping, Integer>()
        {
            @Override
            public void onRowRead(ChangesetMapping mapping)
            {
                closure.execute(mapping);
            }
        });
    }

    @Override
    public int getNumberOfIssueKeysToChangeset()
    {
        Query query = Query.select(IssueToChangesetMapping.ISSUE_KEY)
                .from(IssueToChangesetMapping.class);
        return activeObjects.count(IssueToChangesetMapping.class, query);
    }

    public boolean forEachIssueKeyMapping(final Organization organization, final Repository repository,
            final int pageSize, IssueToMappingFunction function)
    {
        int currentPage = 0;
        IssueToChangesetMapping[] mappings;
        boolean result;
        int repositoryId = repository.getId();

        do
        {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Query issueQuery = Query.select()
                    .from(IssueToChangesetMapping.class)
                    .alias(IssueToChangesetMapping.class, "ic")
                    .alias(ChangesetMapping.class, "cm")
                    .join(ChangesetMapping.class, "ic.CHANGESET_ID = cm.ID")
                    .alias(RepositoryToChangesetMapping.class, "rtoc")
                    .join(RepositoryToChangesetMapping.class, "cm.ID = rtoc.CHANGESET_ID")
                    .alias(RepositoryMapping.class, "rm")
                    .join(RepositoryMapping.class, "rm.ID = rtoc.REPOSITORY_ID")
                    .where("rm.ID = ?", repositoryId)
                    .limit(pageSize)
                    .offset(currentPage * pageSize);

            mappings = activeObjects.find(IssueToChangesetMapping.class, issueQuery);
            currentPage++;

            ImmutableSet.Builder<String> setBuilder = ImmutableSet.builder();

            for (IssueToChangesetMapping mapping : mappings)
            {
                setBuilder.add(mapping.getIssueKey());
            }

            final ImmutableSet<String> issueKeys = setBuilder.build();
            result = function.execute(organization.getDvcsType(), repositoryId, issueKeys);
            log.info("processing page {} with this many elements {} took {} and had the result {}",
                    new Object[] { currentPage, issueKeys.size(), stopWatch, result });
        }
        while (mappings.length > 0 && result);
        return result;
    }

    private Query createLatestChangesetsAvailableForSmartcommitQuery(int repositoryId, final String[] columns)
    {
        // this query is to be used with stream, we have to be explicit about which columns we want
        return Query.select(StringUtils.join(columns, ','))
                .from(ChangesetMapping.class)
                .alias(ChangesetMapping.class, "chm")
                .alias(RepositoryToChangesetMapping.class, "rtchm")
                .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                .where("rtchm." + RepositoryToChangesetMapping.REPOSITORY_ID + " = ? and chm." + ChangesetMapping.SMARTCOMMIT_AVAILABLE + " = ? ", repositoryId, Boolean.TRUE)
                .order(ChangesetMapping.DATE + " DESC");
    }

    @Override
    public Set<String> findReferencedProjects(int repositoryId)
    {
        Query query = Query.select(IssueToChangesetMapping.PROJECT_KEY).distinct()
                .alias(ProjectKey.class, "pk")
                .alias(ChangesetMapping.class, "chm")
                .alias(RepositoryToChangesetMapping.class, "rtchm")
                .join(ChangesetMapping.class, "chm.ID = pk." + IssueToChangesetMapping.CHANGESET_ID)
                .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                .where("rtchm." + RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId)
                .order(IssueToChangesetMapping.PROJECT_KEY);


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

    @Override
    public Set<String> findEmails(int repositoryId, String author)
    {
        Query query = Query.select(ChangesetMapping.AUTHOR_EMAIL).distinct()
                .from(ChangesetMapping.class)
                .alias(ChangesetMapping.class, "chm")
                .alias(RepositoryToChangesetMapping.class, "rtchm")
                .join(RepositoryToChangesetMapping.class, "chm.ID = rtchm." + RepositoryToChangesetMapping.CHANGESET_ID)
                .where("rtchm." + RepositoryToChangesetMapping.REPOSITORY_ID + " = ? and chm." + ChangesetMapping.AUTHOR + " = ? ", repositoryId, author).limit(1);

        final Set<String> emails = new HashSet<String>();

        activeObjects.stream(AuthorEmail.class, query, new EntityStreamCallback<AuthorEmail, String>()
        {
            @Override
            public void onRowRead(AuthorEmail mapping)
            {
                emails.add(mapping.getAuthorEmail());
            }
        });

        return emails;
    }

    @Table ("ChangesetMapping")
    static interface AuthorEmail extends RawEntity<String>
    {

        @PrimaryKey (ChangesetMapping.AUTHOR_EMAIL)
        String getAuthorEmail();

        void setAuthorEmail();
    }

    @Table ("IssueToChangeset")
    static interface ProjectKey extends RawEntity<String>
    {

        @PrimaryKey (IssueToChangesetMapping.PROJECT_KEY)
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

    @Override
    public int getChangesetCount(final int repositoryId)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<Integer>()
        {
            @Override
            public Integer doInTransaction()
            {
                Query query = Query.select().where(RepositoryToChangesetMapping.REPOSITORY_ID + " = ?", repositoryId);

                return activeObjects.count(RepositoryToChangesetMapping.class, query);
            }
        });
    }

}
