package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Function2;
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
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.atlassian.pocketknife.api.querydsl.SelectQuery;
import com.atlassian.pocketknife.api.querydsl.StreamyResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.mysema.query.Tuple;
import com.mysema.query.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@SuppressWarnings ("SpringJavaAutowiringInspection")
@Component
public class PullRequestQDSL
{
    private final Logger log = LoggerFactory.getLogger(PullRequestQDSL.class);

    private final QueryFactory queryFactory;
    private final SchemaProvider schemaProvider;

    @Autowired
    public PullRequestQDSL(QueryFactory queryFactory, final SchemaProvider schemaProvider)
    {
        this.queryFactory = queryFactory;
        this.schemaProvider = schemaProvider;
    }

    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys, final String dvcsType)
    {
        PullRequestByIssueKeyClosure closure = new PullRequestByIssueKeyClosure(dvcsType, issueKeys, schemaProvider);
        Map<Integer, PullRequest> mapResult = queryFactory.streamyFold(closure);

        return ImmutableList.copyOf(mapResult.values());
    }

    @VisibleForTesting
    static class PullRequestByIssueKeyClosure implements QueryFactory.StreamyFoldClosure<Map<Integer, PullRequest>>
    {
        final String dvcsType;
        final Iterable<String> issueKeys;

        final QRepositoryPullRequestMapping prMapping;
        final QRepositoryPullRequestIssueKeyMapping issueMapping;
        final QPullRequestParticipantMapping participantMapping;
        final QRepositoryMapping repositoryMapping;
        final QOrganizationMapping orgMapping;

        PullRequestByIssueKeyClosure(final String dvcsType, final Iterable<String> issueKeys, final SchemaProvider schemaProvider)
        {
            super();
            this.dvcsType = dvcsType;
            this.issueKeys = issueKeys;
            this.prMapping = QRepositoryPullRequestMapping.withSchema(schemaProvider);
            this.issueMapping = QRepositoryPullRequestIssueKeyMapping.withSchema(schemaProvider);
            this.participantMapping = QPullRequestParticipantMapping.withSchema(schemaProvider);
            this.repositoryMapping = QRepositoryMapping.withSchema(schemaProvider);
            this.orgMapping = QOrganizationMapping.withSchema(schemaProvider);
        }

        @Override
        public Function<SelectQuery, StreamyResult> query()
        {
            return new Function<SelectQuery, StreamyResult>()
            {
                @Override
                public StreamyResult apply(@Nullable final SelectQuery select)
                {
                    final Predicate predicate = IssueKeyPredicateFactory.buildIssueKeyPredicate(issueKeys, issueMapping);

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
                            repositoryMapping.SLUG,
                            issueMapping.ISSUE_KEY);
                }
            };
        }

        @Override
        public Function2<Map<Integer, PullRequest>, Tuple, Map<Integer, PullRequest>> getFoldFunction()
        {
            return new Function2<Map<Integer, PullRequest>, Tuple, Map<Integer, PullRequest>>()
            {
                @Override
                public Map<Integer, PullRequest> apply(final Map<Integer, PullRequest> pullRequestsById, final Tuple tuple)
                {
                    Integer pullRequestId = tuple.get(prMapping.ID);
                    PullRequest pullRequest = pullRequestsById.get(pullRequestId);
                    if (pullRequest == null)
                    {
                        pullRequest = new PullRequest(pullRequestId);

                        final Long remoteId = tuple.get(prMapping.REMOTE_ID);
                        pullRequest.setRemoteId(remoteId == null ? -1 : remoteId);
                        final Integer repositoryId = tuple.get(prMapping.TO_REPOSITORY_ID);
                        pullRequest.setRepositoryId(repositoryId == null ? -1 : repositoryId);
                        pullRequest.setName(tuple.get(prMapping.NAME));
                        pullRequest.setUrl(tuple.get(prMapping.URL));
                        final String lastStatus = tuple.get(prMapping.LAST_STATUS);
                        if (lastStatus != null)
                        {
                            pullRequest.setStatus(PullRequestStatus.fromRepositoryPullRequestMapping(lastStatus));
                        }
                        pullRequest.setCreatedOn(tuple.get(prMapping.CREATED_ON));
                        pullRequest.setUpdatedOn(tuple.get(prMapping.UPDATED_ON));
                        pullRequest.setAuthor(tuple.get(prMapping.AUTHOR));
                        final Integer commentCount = tuple.get(prMapping.COMMENT_COUNT);
                        pullRequest.setCommentCount(commentCount == null ? 0 : commentCount);
                        pullRequest.setExecutedBy(tuple.get(prMapping.EXECUTED_BY));

                        String sourceBranch = tuple.get(prMapping.SOURCE_BRANCH);
                        String sourceRepo = tuple.get(prMapping.SOURCE_REPO);
                        String orgHostUrl = tuple.get(orgMapping.HOST_URL);

                        pullRequest.setSource(new PullRequestRef(sourceBranch, sourceRepo,
                                PullRequestTransformer.createRepositoryUrl(orgHostUrl, sourceRepo)));

                        String destinationBranch = tuple.get(prMapping.DESTINATION_BRANCH);
                        String orgName = tuple.get(orgMapping.NAME);
                        String slug = tuple.get(repositoryMapping.SLUG);

                        String repositoryLabel = PullRequestTransformer.createRepositoryLabel(orgName, slug);
                        String repositoryUrl = RepositoryTransformer.createRepositoryUrl(orgHostUrl, orgName, slug);

                        pullRequest.setDestination(new PullRequestRef(destinationBranch, repositoryLabel, repositoryUrl));

                        pullRequestsById.put(pullRequestId, pullRequest);
                    }

                    final String participantUsername = tuple.get(participantMapping.USERNAME);
                    final Boolean participantApproved = tuple.get(participantMapping.APPROVED);
                    final String participantRole = tuple.get(participantMapping.ROLE);

                    // We are left joining so only include the participant if it is available
                    if (participantUsername != null || participantApproved != null || participantRole != null)
                    {
                        boolean participantApprovedPrim = participantApproved == null ? false : participantApproved;
                        Participant participant = new Participant(participantUsername,
                                participantApprovedPrim, participantRole);

                        if (pullRequest.getParticipants() == null)
                        {
                            pullRequest.setParticipants(new ArrayList<Participant>());
                        }

                        if (!pullRequest.getParticipants().contains(participant))
                        {
                            pullRequest.getParticipants().add(participant);
                        }
                    }

                    final String issueKey = tuple.get(issueMapping.ISSUE_KEY);

                    if (!pullRequest.getIssueKeys().contains(issueKey))
                    {
                        pullRequest.getIssueKeys().add(issueKey);
                    }

                    // Not doing commits at this point as it is unecessary
//                        if (withCommits)
//                        {
//                            pullRequest.setCommits(transform(pullRequestMapping.getCommits()));
//                        }
                    return pullRequestsById;
                }
            };
        }

        @Override
        public Map<Integer, PullRequest> getInitialValue()
        {
            return new HashMap<Integer, PullRequest>();
        }
    }
}
