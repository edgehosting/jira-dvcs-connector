package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetails;
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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

@Component
public class ChangesetQDSL
{
    private final Logger log = LoggerFactory.getLogger(ChangesetQDSL.class);

    private ConnectionProvider connectionProvider;
    private QueryFactory queryFactory;

    public ChangesetQDSL()
    {

    }

    @Autowired
    public ChangesetQDSL(ConnectionProvider connectionProvider, QueryFactory queryFactory)
    {
        this.connectionProvider = connectionProvider;
        this.queryFactory = queryFactory;
    }

    public List<Changeset> getByIssueKey(final Iterable<String> issueKeys, final String dvcsType, final boolean newestFirst)
            throws JSONException
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
                            .where(
                                    repositoryMapping.DELETED.eq(false)
                                            .and(repositoryMapping.LINKED.eq(true))
                                            .and(orgMapping.DVCS_TYPE.eq(dvcsType))
                                            .and(issueToChangesetMapping.ISSUE_KEY.in(issueKeysCollection)));

                    return sql.stream(changesetMapping.FILE_DETAILS_JSON,
                            repositoryMapping.ID,
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
                            changesetMapping.SMART_COMMIT_AVAILABLE);
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
                        changeset.setSmartcommitAvaliable(input.get(changesetMapping.SMART_COMMIT_AVAILABLE));

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
}
