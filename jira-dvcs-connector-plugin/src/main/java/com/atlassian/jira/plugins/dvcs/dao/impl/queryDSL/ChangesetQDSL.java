package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.ChangesetTransformer;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
import com.atlassian.jira.plugins.dvcs.model.FileData;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QIssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QOrganizationMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryToChangesetMapping;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.pocketknife.api.querydsl.CloseableIterable;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SelectQuery;
import com.atlassian.pocketknife.api.querydsl.StreamyResult;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.dml.SQLUpdateClause;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;

@Component
public class ChangesetQDSL
{
    private final Logger log = LoggerFactory.getLogger(ChangesetQDSL.class);

    private final ConnectionProvider connectionProvider;
    private final QueryFactory queryFactory;

    @Autowired
    public ChangesetQDSL(ConnectionProvider connectionProvider, QueryFactory queryFactory)
    {
        this.connectionProvider = connectionProvider;
        this.queryFactory = queryFactory;
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
        final Connection connection = connectionProvider.borrowConnection();

        try
        {
            final QChangesetMapping changesetMapping = new QChangesetMapping("CSM", "", QChangesetMapping.AO_TABLE_NAME);
            final QIssueToChangesetMapping issueToChangesetMapping = new QIssueToChangesetMapping("ITCS", "", QIssueToChangesetMapping.AO_TABLE_NAME);
            final QRepositoryToChangesetMapping rtcMapping = new QRepositoryToChangesetMapping("RTC", "", QRepositoryToChangesetMapping.AO_TABLE_NAME);
            final QRepositoryMapping repositoryMapping = new QRepositoryMapping("REPO", "", QRepositoryMapping.AO_TABLE_NAME);
            final QOrganizationMapping orgMapping = new QOrganizationMapping("ORG", "", QOrganizationMapping.AO_TABLE_NAME);

            StreamyResult resultStream = queryFactory.select(new Function<SelectQuery, StreamyResult>()
            {
                @Override
                public StreamyResult apply(@Nullable final SelectQuery select)
                {
                    final Collection<String> issueKeysCollection = Lists.newArrayList(issueKeys);
                    SelectQuery sql = select.from(changesetMapping)
                            .join(issueToChangesetMapping).on(changesetMapping.ID.eq(issueToChangesetMapping.CHANGESET_ID))
                            .join(rtcMapping).on(changesetMapping.ID.eq(rtcMapping.CHANGESET_ID))
                            .join(repositoryMapping).on(repositoryMapping.ID.eq(rtcMapping.REPOSITORY_ID))
                            .join(orgMapping).on(orgMapping.ID.eq(repositoryMapping.ORGANIZATION_ID))
                            .where(repositoryMapping.DELETED.eq(false)
                                    .and(repositoryMapping.LINKED.eq(true))
                                    .and(issueToChangesetMapping.ISSUE_KEY.in(issueKeysCollection)));

                    if (StringUtils.isNotBlank(dvcsType))
                    {
                        sql = sql.where(orgMapping.DVCS_TYPE.eq(dvcsType));
                    }
                    sql = sql.orderBy(newestFirst ? changesetMapping.DATE.desc() : changesetMapping.DATE.asc());

                    return sql.stream(repositoryMapping.ID,
                            issueToChangesetMapping.ISSUE_KEY,
                            changesetMapping.FILE_DETAILS_JSON,
                            changesetMapping.NODE,
                            changesetMapping.RAW_AUTHOR,
                            changesetMapping.AUTHOR,
                            changesetMapping.DATE,
                            changesetMapping.RAW_NODE,
                            changesetMapping.BRANCH,
                            changesetMapping.MESSAGE,
                            changesetMapping.PARENTS_DATA,
                            changesetMapping.FILE_COUNT,
                            changesetMapping.AUTHOR_EMAIL,
                            changesetMapping.ID,
                            changesetMapping.VERSION,
                            changesetMapping.SMARTCOMMIT_AVAILABLE);
                }
            });
            try
            {
                CloseableIterable<Changeset> result = resultStream.map(new Function<Tuple, Changeset>()
                {
                    @Override
                    public Changeset apply(@Nullable final Tuple input)
                    {

                        List<ChangesetFileDetail> fileDetails = ChangesetFileDetails.fromJSON(input.get(changesetMapping.FILE_DETAILS_JSON));

                        final Changeset changeset = new Changeset(input.get(repositoryMapping.ID),
                                input.get(changesetMapping.NODE),
                                input.get(changesetMapping.RAW_AUTHOR),
                                input.get(changesetMapping.AUTHOR),
                                input.get(changesetMapping.DATE),
                                input.get(changesetMapping.RAW_NODE),
                                input.get(changesetMapping.BRANCH),
                                input.get(changesetMapping.MESSAGE),
                                parseParentsData(input.get(changesetMapping.PARENTS_DATA)),
                                fileDetails != null ? ImmutableList.<ChangesetFile>copyOf(fileDetails) : null,
                                input.get(changesetMapping.FILE_COUNT),
                                input.get(changesetMapping.AUTHOR_EMAIL));

                        changeset.setId(input.get(changesetMapping.ID));
                        changeset.setVersion(input.get(changesetMapping.VERSION));
                        changeset.setSmartcommitAvaliable(input.get(changesetMapping.SMARTCOMMIT_AVAILABLE));

                        changeset.setFileDetails(fileDetails);

                        return changeset;
                    }
                });

                return Lists.newArrayList(result);
            }
            finally
            {
                resultStream.close();
            }
        }
        finally
        {
            connectionProvider.returnConnection(connection);
        }
    }

    private List<String> parseParentsData(String parentsData)
    {
        if (ChangesetMapping.TOO_MANY_PARENTS.equals(parentsData))
        {
            return null;
        }

        List<String> parents = new ArrayList<String>();

        if (StringUtils.isBlank(parentsData))
        {
            return parents;
        }

        try
        {
            JSONArray parentsJson = new JSONArray(parentsData);
            for (int i = 0; i < parentsJson.length(); i++)
            {
                parents.add(parentsJson.getString(i));
            }
        }
        catch (JSONException e)
        {
            log.error("Failed parsing parents from ParentsJson data.");
        }

        return parents;
    }

    public void updateChangesetMappingsThatHaveOldFileData(final Iterable<String> issueKeys, @Nullable final String dvcsType)
    {
        final Connection connection = connectionProvider.borrowConnection();

        try
        {
            final QChangesetMapping changesetMapping = new QChangesetMapping("CSM", "", QChangesetMapping.AO_TABLE_NAME);
            final QIssueToChangesetMapping issueToChangesetMapping = new QIssueToChangesetMapping("ITCS", "", QIssueToChangesetMapping.AO_TABLE_NAME);
            final QRepositoryToChangesetMapping rtcMapping = new QRepositoryToChangesetMapping("RTC", "", QRepositoryToChangesetMapping.AO_TABLE_NAME);
            final QRepositoryMapping repositoryMapping = new QRepositoryMapping("REPO", "", QRepositoryMapping.AO_TABLE_NAME);
            final QOrganizationMapping orgMapping = new QOrganizationMapping("ORG", "", QOrganizationMapping.AO_TABLE_NAME);

            final Collection<String> issueKeysCollection = Lists.newArrayList(issueKeys);
            SQLQuery sql = queryFactory.select(connection).from(changesetMapping)
                    .join(issueToChangesetMapping).on(changesetMapping.ID.eq(issueToChangesetMapping.CHANGESET_ID))
                    .join(rtcMapping).on(changesetMapping.ID.eq(rtcMapping.CHANGESET_ID))
                    .join(repositoryMapping).on(repositoryMapping.ID.eq(rtcMapping.REPOSITORY_ID))
                    .join(orgMapping).on(orgMapping.ID.eq(repositoryMapping.ORGANIZATION_ID))
                    .where(repositoryMapping.DELETED.eq(false)
                            .and(repositoryMapping.LINKED.eq(true))
                            .and(changesetMapping.FILES_DATA.isNotNull())
                            .and(changesetMapping.FILE_COUNT.eq(0))
                            .and(issueToChangesetMapping.ISSUE_KEY.in(issueKeysCollection)))
                    .distinct();

            if (StringUtils.isNotBlank(dvcsType))
            {
                sql = sql.where(orgMapping.DVCS_TYPE.eq(dvcsType));
            }

            List<Tuple> result = sql.list(changesetMapping.ID,
                    changesetMapping.NODE,
                    changesetMapping.FILES_DATA,
                    changesetMapping.FILE_DETAILS_JSON);

            for (Tuple tuple : result)
            {
                String node = tuple.get(changesetMapping.NODE);
                Integer id = tuple.get(changesetMapping.ID);
                final FileData fileData = FileData.from(tuple.get(changesetMapping.FILES_DATA), tuple.get(changesetMapping.FILE_DETAILS_JSON));
                log.debug("Migrating file count from old file data structure for changeset ID {} Hash {}.", id, node);

                String fileDetailsJson = tuple.get(changesetMapping.FILE_DETAILS_JSON);
                SQLUpdateClause update = buildUpdateChangesetFileDetails(connection, dvcsType, fileDetailsJson, fileData, node, id);
                update.execute();
            }
            try
            {
                connection.commit();
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }
        finally
        {
            connectionProvider.returnConnection(connection);
        }
    }

    SQLUpdateClause buildUpdateChangesetFileDetails(final Connection connection, String dvcsType, String fileDetailsJson,
            final FileData fileData, String node, Integer id)
    {
        final QChangesetMapping updateChangesetMapping = new QChangesetMapping("CSV", "", QChangesetMapping.AO_TABLE_NAME);
        SQLUpdateClause update = queryFactory.update(connection, updateChangesetMapping);

        // we can use the file count in file data directly
        // https://jdog.jira-dev.com/browse/BBC-709 migrating file count from file data to separate column
        update = update.set(updateChangesetMapping.FILE_COUNT, fileData.getFileCount());

        if (BITBUCKET.equals(dvcsType) && fileData.getFileCount() == Changeset.MAX_VISIBLE_FILES + 1)
        {
            // file count in file data is 6 for Bitbucket, we need to refetch the diffstat to find out the correct number
            // https://jdog.jira-dev.com/browse/BBC-719 forcing file details to reload if changed files number is incorrect
            log.debug("Forcing to refresh file details for changeset ID {} Hash {}.", id, node);
            update = update.setNull(updateChangesetMapping.FILE_DETAILS_JSON);
        }
        else if (fileDetailsJson == null && fileData.hasDetails())
        {
            log.debug("Migrating file details from old file data structure for changeset ID {} Hash {}.", id, node);
            final String newFilesDetailsJson = ChangesetFileDetails.toJSON(ChangesetTransformer.transfromFileData(fileData));
            update = update.set(updateChangesetMapping.FILE_DETAILS_JSON, newFilesDetailsJson);
        }

        return update;
    }
}
