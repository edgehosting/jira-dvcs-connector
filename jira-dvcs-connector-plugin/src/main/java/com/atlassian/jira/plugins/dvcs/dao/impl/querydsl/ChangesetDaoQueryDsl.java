package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Function2;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingFunction;
import com.atlassian.jira.plugins.dvcs.dao.impl.ChangesetDaoImpl;
import com.atlassian.jira.plugins.dvcs.dao.impl.DAOConstants;
import com.atlassian.jira.plugins.dvcs.dao.impl.QueryDslFeatureHelper;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QIssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QOrganizationMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryToChangesetMapping;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.atlassian.pocketknife.api.querydsl.SelectQuery;
import com.atlassian.pocketknife.api.querydsl.StreamyResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mysema.query.Tuple;
import com.mysema.query.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of {@link com.atlassian.jira.plugins.dvcs.dao.ChangesetDao} that delegates to the original AO based
 * implementation for most calls except #getByIssueKey which will use Query DSL if the dark feature is set.
 */
@SuppressWarnings ("SpringJavaAutowiringInspection")
@Component ("changesetDaoQueryDsl")
public class ChangesetDaoQueryDsl implements ChangesetDao
{
    private final QueryFactory queryFactory;
    private final SchemaProvider schemaProvider;
    private final ChangesetDaoImpl changesetDao;
    private final QueryDslFeatureHelper queryDslFeatureHelper;

    @Autowired
    public ChangesetDaoQueryDsl(final QueryFactory queryFactory, final SchemaProvider schemaProvider,
            final ChangesetDaoImpl changesetDao, final QueryDslFeatureHelper queryDslFeatureHelper)
    {
        this.queryFactory = checkNotNull(queryFactory);
        this.schemaProvider = checkNotNull(schemaProvider);
        this.changesetDao = checkNotNull(changesetDao);
        this.queryDslFeatureHelper = checkNotNull(queryDslFeatureHelper);
    }

    @Override
    public void removeAllInRepository(final int repositoryId)
    {
        changesetDao.removeAllInRepository(repositoryId);
    }

    @Override
    public Changeset create(final Changeset changeset, final Set<String> extractedIssues)
    {
        return changesetDao.create(changeset, extractedIssues);
    }

    @Override
    public boolean createOrAssociate(final Changeset changeset, final Set<String> extractedIssues)
    {
        return changesetDao.createOrAssociate(changeset, extractedIssues);
    }

    @Override
    public Changeset update(final Changeset changeset)
    {
        return changesetDao.update(changeset);
    }

    @Override
    public Changeset migrateFilesData(final Changeset changeset, final String dvcsType)
    {
        return changesetDao.migrateFilesData(changeset, dvcsType);
    }

    @Override
    public Changeset getByNode(final int repositoryId, final String changesetNode)
    {
        return changesetDao.getByNode(repositoryId, changesetNode);
    }

    @Override
    public List<Changeset> getByIssueKey(final Iterable<String> issueKeys, final boolean newestFirst)
    {
        return changesetDao.getByIssueKey(issueKeys, newestFirst);
    }

    @Override
    public List<Changeset> getByRepository(final int repositoryId)
    {
        return changesetDao.getByRepository(repositoryId);
    }

    @Override
    public List<Changeset> getLatestChangesets(final int maxResults, final GlobalFilter gf)
    {
        return changesetDao.getLatestChangesets(maxResults, gf);
    }

    @Override
    public void forEachLatestChangesetsAvailableForSmartcommitDo(final int repositoryId, final String[] columns, final ForEachChangesetClosure closure)
    {
        changesetDao.forEachLatestChangesetsAvailableForSmartcommitDo(repositoryId, columns, closure);
    }

    @Override
    public int getNumberOfIssueKeysToChangeset()
    {
        return changesetDao.getNumberOfIssueKeysToChangeset();
    }

    @Override
    public boolean forEachIssueKeyMapping(final Organization organization, final Repository repository, final int pageSize, final IssueToMappingFunction function)
    {
        return changesetDao.forEachIssueKeyMapping(organization, repository, pageSize, function);
    }

    @Override
    public void markSmartcommitAvailability(final int id, final boolean available)
    {
        changesetDao.markSmartcommitAvailability(id, available);
    }

    @Override
    public Set<String> findReferencedProjects(final int repositoryId)
    {
        return changesetDao.findReferencedProjects(repositoryId);
    }

    @Override
    public int getChangesetCount(final int repositoryId)
    {
        return changesetDao.getChangesetCount(repositoryId);
    }

    @Override
    public Set<String> findEmails(final int repositoryId, final String author)
    {
        return changesetDao.findEmails(repositoryId, author);
    }

    @Override
    public List<Changeset> getByIssueKey(@Nonnull final Iterable<String> issueKeys, @Nullable final String dvcsType,
            final boolean newestFirst)
    {
        if (queryDslFeatureHelper.isRetrievalUsingQueryDslDisabled())
        {
            return changesetDao.getByIssueKey(issueKeys, dvcsType, newestFirst);
        }
        if (Iterables.isEmpty(issueKeys))
        {
            return Collections.emptyList();
        }

        ByIssueKeyClosure closure = new ByIssueKeyClosure(dvcsType, issueKeys, schemaProvider, newestFirst);
        Map<Integer, Changeset> changesetsById = queryFactory.halfStreamyFold(new HashMap<Integer, Changeset>(), closure);

        // Still need to sort the result as we have a map of changesets, even though the results are also sorted
        final ArrayList<Changeset> result = new ArrayList<Changeset>(changesetsById.values());
        Collections.sort(result, new ChangesetDateComparator(newestFirst));
        return result;
    }

    @VisibleForTesting
    static class ByIssueKeyClosure implements QueryFactory.HalfStreamyFoldClosure<Map<Integer, Changeset>>
    {
        final String dvcsType;
        final Iterable<String> issueKeys;
        final QChangesetMapping changesetMapping;
        final QIssueToChangesetMapping issueToChangesetMapping;
        final QRepositoryToChangesetMapping rtcMapping;
        final QRepositoryMapping repositoryMapping;
        final QOrganizationMapping orgMapping;
        final boolean newestFirst;

        ByIssueKeyClosure(final String dvcsType, final Iterable<String> issueKeys, final SchemaProvider schemaProvider, final boolean newestFirst)
        {
            super();
            this.dvcsType = dvcsType;
            this.issueKeys = issueKeys;
            this.newestFirst = newestFirst;
            this.changesetMapping = QChangesetMapping.withSchema(schemaProvider);
            this.issueToChangesetMapping = QIssueToChangesetMapping.withSchema(schemaProvider);
            this.rtcMapping = QRepositoryToChangesetMapping.withSchema(schemaProvider);
            this.repositoryMapping = QRepositoryMapping.withSchema(schemaProvider);
            this.orgMapping = QOrganizationMapping.withSchema(schemaProvider);
        }

        @Override
        public Function<SelectQuery, StreamyResult> getQuery()
        {
            return new Function<SelectQuery, StreamyResult>()
            {
                @Override
                public StreamyResult apply(@Nullable final SelectQuery select)
                {
                    Predicate issueKeyPredicate = IssueKeyPredicateFactory.buildIssueKeyPredicate(issueKeys, issueToChangesetMapping);

                    SelectQuery sql = select
                            .from(changesetMapping)
                            .join(issueToChangesetMapping)
                            .on(changesetMapping.ID.eq(issueToChangesetMapping.CHANGESET_ID))
                            .join(rtcMapping)
                            .on(changesetMapping.ID.eq(rtcMapping.CHANGESET_ID))
                            .join(repositoryMapping)
                            .on(repositoryMapping.ID.eq(rtcMapping.REPOSITORY_ID))
                            .join(orgMapping)
                            .on(orgMapping.ID.eq(repositoryMapping.ORGANIZATION_ID))
                            .where(repositoryMapping.DELETED.eq(false).and(repositoryMapping.LINKED.eq(true))
                                    .and(issueKeyPredicate))
                            .orderBy(newestFirst ? changesetMapping.DATE.desc() : changesetMapping.DATE.asc());

                    if (StringUtils.isNotBlank(dvcsType))
                    {
                        sql = sql.where(orgMapping.DVCS_TYPE.eq(dvcsType));
                    }

                    return sql.stream(repositoryMapping.ID, issueToChangesetMapping.ISSUE_KEY,
                            changesetMapping.FILE_DETAILS_JSON, changesetMapping.NODE, changesetMapping.RAW_AUTHOR,
                            changesetMapping.AUTHOR, changesetMapping.DATE, changesetMapping.RAW_NODE,
                            changesetMapping.BRANCH, changesetMapping.MESSAGE, changesetMapping.PARENTS_DATA,
                            changesetMapping.FILE_COUNT, changesetMapping.AUTHOR_EMAIL, changesetMapping.ID,
                            changesetMapping.VERSION, changesetMapping.SMARTCOMMIT_AVAILABLE);
                }
            };
        }

        @Override
        public Function2<Map<Integer, Changeset>, Tuple, Map<Integer, Changeset>> getFoldFunction()
        {
            return new Function2<Map<Integer, Changeset>, Tuple, Map<Integer, Changeset>>()
            {
                @Override
                public Map<Integer, Changeset> apply(Map<Integer, Changeset> changesetsById, Tuple tuple)
                {
                    final Integer changesetId = tuple.get(changesetMapping.ID);
                    final Integer repositoryId = tuple.get(repositoryMapping.ID);
                    final String issueKey = tuple.get(issueToChangesetMapping.ISSUE_KEY);

                    Changeset changeset = changesetsById.get(changesetId);

                    if (changeset == null)
                    {
                        // If we have found enough changsets then we can just skip this entry
                        if (changesetsById.size() >= DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY)
                        {
                            return changesetsById;
                        }
                        List<ChangesetFileDetail> fileDetails = ChangesetFileDetails.fromJSON(tuple
                                .get(changesetMapping.FILE_DETAILS_JSON));

                        changeset = new Changeset(repositoryId, tuple.get(changesetMapping.NODE),
                                tuple.get(changesetMapping.RAW_AUTHOR), tuple.get(changesetMapping.AUTHOR),
                                tuple.get(changesetMapping.DATE), tuple.get(changesetMapping.RAW_NODE),
                                tuple.get(changesetMapping.BRANCH), tuple.get(changesetMapping.MESSAGE),
                                ChangesetTransformer.parseParentsData(tuple.get(changesetMapping.PARENTS_DATA)),
                                fileDetails != null ? ImmutableList.<ChangesetFile>copyOf(fileDetails) : null,
                                tuple.get(changesetMapping.FILE_COUNT), tuple.get(changesetMapping.AUTHOR_EMAIL));

                        changeset.setId(changesetId);
                        changeset.setVersion(tuple.get(changesetMapping.VERSION));
                        changeset.setSmartcommitAvaliable(tuple.get(changesetMapping.SMARTCOMMIT_AVAILABLE));

                        changeset.setFileDetails(fileDetails);
                        changesetsById.put(changesetId, changeset);
                    }

                    if (!changeset.getRepositoryIds().contains(repositoryId))
                    {
                        changeset.getRepositoryIds().add(repositoryId);
                    }
                    if (!changeset.getIssueKeys().contains(issueKey))
                    {
                        changeset.getIssueKeys().add(issueKey);
                    }
                    return changesetsById;
                }
            };
        }
    }


    private static class ChangesetDateComparator implements Comparator<Changeset>
    {
        private final boolean newestFirst;

        private ChangesetDateComparator(final boolean newestFirst)
        {
            this.newestFirst = newestFirst;
        }

        @Override
        public int compare(final Changeset o1, final Changeset o2)
        {
            if (newestFirst)
            {
                // Invert the result so that newest come first
                return -1 * compareInternal(o1, o2);
            }
            else
            {
                return compareInternal(o1, o2);
            }
        }

        /**
         * Generally the dates should not be null but as we are sorting in the application now we need to be safe
         */
        private int compareInternal(final Changeset cs1, final Changeset cs2)
        {
            final Date cs1Date = cs1.getDate();
            final Date cs2Date = cs2.getDate();
            if (cs1Date == null && cs2Date == null)
            {
                return 0;
            }
            if (cs2Date == null)
            {
                return -1;
            }
            if (cs1Date == null)
            {
                return 1;
            }

            return cs1Date.compareTo(cs2Date);
        }
    }
}
