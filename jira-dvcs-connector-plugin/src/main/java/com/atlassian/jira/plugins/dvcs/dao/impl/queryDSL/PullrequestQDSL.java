package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Iterables;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.PullRequestTransformer;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.RepositoryTransformer;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.PullRequestRef;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QOrganizationMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QPullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.pocketknife.api.querydsl.CloseableIterable;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SelectQuery;
import com.atlassian.pocketknife.api.querydsl.StreamyResult;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@Component
public class PullRequestQDSL
{
    private final Logger log = LoggerFactory.getLogger(PullRequestQDSL.class);

    private final ConnectionProvider connectionProvider;
    private final QueryFactory queryFactory;

    @Autowired
    public PullRequestQDSL(ConnectionProvider connectionProvider, QueryFactory queryFactory)
    {
        this.connectionProvider = connectionProvider;
        this.queryFactory = queryFactory;
    }

    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys, final String dvcsType)
    {
        final Connection connection = connectionProvider.borrowConnection();

        try
        {
            final QRepositoryPullRequestMapping prMapping = new QRepositoryPullRequestMapping("PRM", "", QRepositoryPullRequestMapping.AO_TABLE_NAME);
            final QRepositoryPullRequestIssueKeyMapping issueMapping = new QRepositoryPullRequestIssueKeyMapping("PRIK", "", QRepositoryPullRequestIssueKeyMapping.AO_TABLE_NAME);
            final QRepositoryMapping repositoryMapping = new QRepositoryMapping("REPO", "", QRepositoryMapping.AO_TABLE_NAME);
            final QOrganizationMapping orgMapping = new QOrganizationMapping("ORG", "", QOrganizationMapping.AO_TABLE_NAME);
            final QPullRequestParticipantMapping participantMapping = new QPullRequestParticipantMapping("PRTM", "", QPullRequestParticipantMapping.AO_TABLE_NAME);

            final Predicate predicate = buildIssueKeyPredicate(issueKeys, issueMapping);

            StreamyResult resultStream = queryFactory.select(new Function<SelectQuery, StreamyResult>()
            {
                @Override
                public StreamyResult apply(@Nullable final SelectQuery select)
                {
                    SelectQuery sql = select.from(prMapping)
                            .join(issueMapping).on(prMapping.ID.eq(issueMapping.PULL_REQUEST_ID))
                            .join(repositoryMapping).on(repositoryMapping.ID.eq(prMapping.TO_REPOSITORY_ID))
                            .join(orgMapping).on(orgMapping.ID.eq(repositoryMapping.ORGANIZATION_ID))
                            .leftJoin(participantMapping).on(prMapping.ID.eq(participantMapping.PULL_REQUEST_ID))
                            .where(repositoryMapping.DELETED.eq(false)
                                    .and(repositoryMapping.LINKED.eq(true))
                                    .and(predicate));

                    if (StringUtils.isNotBlank(dvcsType))
                    {
                        sql = sql.where(orgMapping.DVCS_TYPE.eq(dvcsType));
                    }

                    return sql.stream(
                            prMapping.ID,
                            prMapping.TO_REPOSITORY_ID,
                            prMapping.REMOTE_ID,
                            prMapping.EXECUTED_BY,
                            prMapping.NAME,
                            prMapping.URL,
                            prMapping.LAST_STATUS,
                            prMapping.CREATED_ON,
                            prMapping.UPDATED_ON,
                            prMapping.AUTHOR,
                            prMapping.COMMENT_COUNT,
                            participantMapping.USERNAME,
                            participantMapping.APPROVED,
                            participantMapping.ROLE,
                            prMapping.SOURCE_BRANCH,
                            prMapping.SOURCE_REPO,
                            orgMapping.HOST_URL,
                            prMapping.DESTINATION_BRANCH,
                            orgMapping.NAME,
                            repositoryMapping.SLUG);
                }
            });
            try
            {
                final Map<Integer, PullRequest> pullRequestsById = new HashMap<Integer, PullRequest>();

                Function<Tuple, PullRequest> f = new Function<Tuple, PullRequest>()
                {
                    @Override
                    public PullRequest apply(@Nullable final Tuple input)
                    {
                        Integer pullRequestId = input.get(prMapping.ID);
                        PullRequest pullRequest = pullRequestsById.get(pullRequestId);
                        if (pullRequest == null)
                        {
                            pullRequest = new PullRequest(pullRequestId);
                            pullRequestsById.put(pullRequestId, pullRequest);

                            pullRequest.setRemoteId(input.get(prMapping.REMOTE_ID));
                            pullRequest.setRepositoryId(input.get(prMapping.TO_REPOSITORY_ID));
                            pullRequest.setName(input.get(prMapping.NAME));
                            pullRequest.setUrl(input.get(prMapping.URL));
                            pullRequest.setStatus(PullRequestStatus.fromRepositoryPullRequestMapping(input.get(prMapping.LAST_STATUS)));
                            pullRequest.setCreatedOn(input.get(prMapping.CREATED_ON));
                            pullRequest.setUpdatedOn(input.get(prMapping.UPDATED_ON));
                            pullRequest.setAuthor(input.get(prMapping.AUTHOR));
                            pullRequest.setCommentCount(input.get(prMapping.COMMENT_COUNT));
                            pullRequest.setExecutedBy(input.get(prMapping.EXECUTED_BY));

                            String sourceBranch = input.get(prMapping.SOURCE_BRANCH);
                            String sourceRepo = input.get(prMapping.SOURCE_REPO);
                            String orgHostUrl = input.get(orgMapping.HOST_URL);

                            pullRequest.setSource(new PullRequestRef(sourceBranch, sourceRepo,
                                    PullRequestTransformer.createRepositoryUrl(orgHostUrl, sourceRepo)));

                            String destinationBranch = input.get(prMapping.DESTINATION_BRANCH);
                            String orgName = input.get(orgMapping.NAME);
                            String slug = input.get(repositoryMapping.SLUG);

                            String repositoryLabel = PullRequestTransformer.createRepositoryLabel(orgName, slug);
                            String repositoryUrl = RepositoryTransformer.createRepositoryUrl(orgHostUrl, orgName, slug);

                            pullRequest.setDestination(new PullRequestRef(destinationBranch, repositoryLabel, repositoryUrl));
                        }

                        final String participantUsername = input.get(participantMapping.USERNAME);
                        final Boolean participantApproved = input.get(participantMapping.APPROVED);
                        final String participantRole = input.get(participantMapping.ROLE);

                        // We are left joining so only include the participant if it is available
                        if (participantUsername != null && participantApproved != null && participantRole != null)
                        {
                            Participant participant = new Participant(participantUsername,
                                    participantApproved, participantRole);

                            if (pullRequest.getParticipants() == null)
                            {
                                pullRequest.setParticipants(new ArrayList<Participant>());
                            }

                            if (!pullRequest.getParticipants().contains(participant))
                            {
                                pullRequest.getParticipants().add(participant);
                            }
                        }

//                        if (withCommits)
//                        {
//                            pullRequest.setCommits(transform(pullRequestMapping.getCommits()));
//                        }
                        return pullRequest;
                    }
                };

                CloseableIterable<PullRequest> resultIterable = resultStream.map(f);

                // Do the magic by iterating over to trigger the map call
                Lists.newArrayList(resultIterable);

                List<PullRequest> result = Lists.newArrayList(pullRequestsById.values());
                resultIterable.close();
                try
                {
                    connection.commit();
                }
                catch (SQLException e)
                {
                    throw new RuntimeException(e);
                }
                return result;
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

    private Predicate buildIssueKeyPredicate(final Iterable<String> issueKeys,
            final QRepositoryPullRequestIssueKeyMapping issueMapping)
    {
        final List<String> issueKeysList = Lists.newArrayList(issueKeys);

        if (issueKeysList.size() <= ActiveObjectsUtils.SQL_IN_CLAUSE_MAX)
        {
            return issueMapping.ISSUE_KEY.in(issueKeysList);
        }

        List<List<String>> partititionedIssueKeys = Lists.partition(issueKeysList, ActiveObjectsUtils.SQL_IN_CLAUSE_MAX);

        BooleanExpression issueKeyPredicate = issueMapping.ISSUE_KEY.in(partititionedIssueKeys.get(0));

        for (List<String> keys : Iterables.drop(1, partititionedIssueKeys))
        {
            issueKeyPredicate = issueKeyPredicate.or(issueMapping.ISSUE_KEY.in(keys));
        }

        return issueKeyPredicate;
    }
}
