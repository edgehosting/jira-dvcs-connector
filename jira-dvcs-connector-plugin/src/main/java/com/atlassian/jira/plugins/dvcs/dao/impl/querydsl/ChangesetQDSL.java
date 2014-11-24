package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Function2;
import com.atlassian.fugue.Iterables;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QIssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QOrganizationMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.atlassian.pocketknife.api.querydsl.SelectQuery;
import com.atlassian.pocketknife.api.querydsl.StreamyResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mysema.query.Tuple;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@Component
public class ChangesetQDSL
{
    private final Logger log = LoggerFactory.getLogger(ChangesetQDSL.class);

    @ComponentImport
    private final QueryFactory queryFactory;
    @ComponentImport
    private final SchemaProvider schemaProvider;

    @Autowired
    public ChangesetQDSL(QueryFactory queryFactory, final SchemaProvider schemaProvider)
    {
        this.queryFactory = queryFactory;
        this.schemaProvider = schemaProvider;
    }

    /**
     * Returns the changesets that are associated with the supplied issue key and are in a enabled, linked repository
     * for the specified type of dvcs NOTE that this method does not filter duplicates and you will receive one
     * Changeset per link to an issue key. This is intentional as it will allow us to modify this in future so that we
     * match changeset information with issue key information. Also note that it does not limit the number of issue
     * keys, if you supply more than the database can handle with an in it will fail.
     * <p/>
     * Recommended usage is to break up the keys prior to calling and de-duplicate on the outside as this provides us
     * with a path to replace with a call that does the issue key grouping (probably involves changing the return
     * signature as well).
     */
    public List<Changeset> getByIssueKey(final Iterable<String> issueKeys, @Nullable final String dvcsType,
            final boolean newestFirst)
    {
        ByIssueKeyClosure closure = new ByIssueKeyClosure(dvcsType, issueKeys, schemaProvider);
        StreamyResult streamyResult = queryFactory.select(closure.query());
        Map<Integer, Changeset> changesetsById = streamyResult.foldLeft(closure.getInitialValue(), closure.getFoldFunction());

        final ArrayList<Changeset> result = new ArrayList<Changeset>(changesetsById.values());
        Collections.sort(result, new ChangesetDateComparator(newestFirst));
        return result;
    }

    @VisibleForTesting
    static class ByIssueKeyClosure
    {
        final String dvcsType;
        final Iterable<String> issueKeys;
        final QChangesetMapping changesetMapping;
        final QIssueToChangesetMapping issueToChangesetMapping;
        final QRepositoryToChangesetMapping rtcMapping;
        final QRepositoryMapping repositoryMapping;
        final QOrganizationMapping orgMapping;

        ByIssueKeyClosure(final String dvcsType, final Iterable<String> issueKeys, final SchemaProvider schemaProvider)
        {
            super();
            this.dvcsType = dvcsType;
            this.issueKeys = issueKeys;
            this.changesetMapping = QChangesetMapping.withSchema(schemaProvider);
            this.issueToChangesetMapping = QIssueToChangesetMapping.withSchema(schemaProvider);
            this.rtcMapping = QRepositoryToChangesetMapping.withSchema(schemaProvider);
            this.repositoryMapping = QRepositoryMapping.withSchema(schemaProvider);
            this.orgMapping = QOrganizationMapping.withSchema(schemaProvider);
        }

        Function<SelectQuery, StreamyResult> query()
        {
            return new Function<SelectQuery, StreamyResult>()
            {
                @Override
                public StreamyResult apply(@Nullable final SelectQuery select)
                {
                    Predicate issueKeyPredicate = buildIssueKeyPredicate(issueKeys, issueToChangesetMapping);

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
                                    .and(issueKeyPredicate));

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

        Map<Integer, Changeset> getInitialValue()
        {
            return new HashMap<Integer, Changeset>();
        }

        Function2<Map<Integer, Changeset>, Tuple, Map<Integer, Changeset>> getFoldFunction()
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

        private Predicate buildIssueKeyPredicate(final Iterable<String> issueKeys, final QIssueToChangesetMapping issueToChangesetMapping)
        {
            final List<String> issueKeysList = Lists.newArrayList(issueKeys);

            if (issueKeysList.size() <= ActiveObjectsUtils.SQL_IN_CLAUSE_MAX)
            {
                return issueToChangesetMapping.ISSUE_KEY.in(issueKeysList);
            }

            List<List<String>> partititionedIssueKeys = Lists.partition(issueKeysList, ActiveObjectsUtils.SQL_IN_CLAUSE_MAX);

            BooleanExpression issueKeyPredicate = issueToChangesetMapping.ISSUE_KEY.in(partititionedIssueKeys.get(0));

            for (List<String> keys : Iterables.drop(1, partititionedIssueKeys))
            {
                issueKeyPredicate = issueKeyPredicate.or(issueToChangesetMapping.ISSUE_KEY.in(keys));
            }

            return issueKeyPredicate;
        }
    }


    private class ChangesetDateComparator implements Comparator<Changeset>
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
