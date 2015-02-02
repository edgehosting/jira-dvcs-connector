package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Function2;
import com.atlassian.jira.plugins.dvcs.dao.impl.DAOConstants;
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
import com.google.common.collect.Iterables;
import com.mysema.query.Tuple;
import com.mysema.query.types.Predicate;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Retrieves Pull Requests using Query DSL and then maps them directly to API entities, does not implement {@link
 * com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao} because we need to return the entity used in
 * {@link com.atlassian.jira.plugins.dvcs.service.PullRequestService#getByIssueKeys(Iterable, String)}.
 */
@SuppressWarnings ("SpringJavaAutowiringInspection")
@Component
public class PullRequestDaoQueryDsl
{
    private final QueryFactory queryFactory;
    private final SchemaProvider schemaProvider;

    @Autowired
    public PullRequestDaoQueryDsl(final QueryFactory queryFactory, final SchemaProvider schemaProvider)
    {
        this.queryFactory = checkNotNull(queryFactory);
        this.schemaProvider = checkNotNull(schemaProvider);
    }

    /**
     * Retrieve up to {@link com.atlassian.jira.plugins.dvcs.dao.impl.DAOConstants#MAXIMUM_ENTITIES_PER_ISSUE_KEY} Pull
     * Requests based on the supplied issue keys
     *
     * @param issueKeys The issue keys that are associated with the pull request
     * @param dvcsType The (optional) dvcs type that we are restricting to
     * @return Pull requests associated with the issue key
     */
    @Nonnull
    public List<PullRequest> getByIssueKeys(@Nonnull final Iterable<String> issueKeys, @Nullable final String dvcsType)
    {
        if (Iterables.isEmpty(issueKeys))
        {
            return Collections.emptyList();
        }
        checkNotNull(issueKeys);
        PullRequestByIssueKeyClosure closure = new PullRequestByIssueKeyClosure(dvcsType, issueKeys, schemaProvider);
        Map<Integer, PullRequest> mapResult = queryFactory.halfStreamyFold(new HashMap<Integer, PullRequest>(), closure);

        return ImmutableList.copyOf(mapResult.values());
    }

    @VisibleForTesting
    static class PullRequestByIssueKeyClosure implements QueryFactory.HalfStreamyFoldClosure<Map<Integer, PullRequest>>
    {
        private final String dvcsType;
        private final Iterable<String> issueKeys;

        @VisibleForTesting
        final QRepositoryPullRequestMapping prMapping;
        @VisibleForTesting
        final QRepositoryPullRequestIssueKeyMapping issueMapping;
        @VisibleForTesting
        final QPullRequestParticipantMapping participantMapping;
        @VisibleForTesting
        final QRepositoryMapping repositoryMapping;
        @VisibleForTesting
        final QOrganizationMapping orgMapping;

        @Nonnull
        PullRequestByIssueKeyClosure(@Nullable final String dvcsType, @Nonnull final Iterable<String> issueKeys,
                @Nonnull final SchemaProvider schemaProvider)
        {
            this.dvcsType = dvcsType;
            this.issueKeys = checkNotNull(issueKeys);
            this.prMapping = QRepositoryPullRequestMapping.withSchema(schemaProvider);
            this.issueMapping = QRepositoryPullRequestIssueKeyMapping.withSchema(schemaProvider);
            this.participantMapping = QPullRequestParticipantMapping.withSchema(schemaProvider);
            this.repositoryMapping = QRepositoryMapping.withSchema(schemaProvider);
            this.orgMapping = QOrganizationMapping.withSchema(schemaProvider);
        }

        @Override
        @Nonnull
        public Function<SelectQuery, StreamyResult> getQuery()
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
                                    .and(predicate))
                            .orderBy(prMapping.CREATED_ON.desc());

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
                    final Integer pullRequestId = tuple.get(prMapping.ID);
                    PullRequest pullRequest = pullRequestsById.get(pullRequestId);

                    if (pullRequest == null)
                    {
                        // If we have reached the limit then we stop processing the PRs and return them, this is not applied in the query
                        // as the results are denormalised so we may see several rows for one PR
                        if (pullRequestsById.size() >= DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY)
                        {
                            return pullRequestsById;
                        }

                        pullRequest = buildPullRequest(pullRequestId, tuple);
                        pullRequestsById.put(pullRequestId, pullRequest);
                    }

                    addParticipant(pullRequest, tuple);
                    addIssueKey(pullRequest, tuple);

                    return pullRequestsById;
                }
            };
        }

        @Nonnull
        private PullRequest buildPullRequest(@Nonnull final Integer pullRequestId, @Nonnull final Tuple tuple)
        {
            PullRequest pullRequest = new PullRequest(pullRequestId);

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

            return pullRequest;
        }

        private void addParticipant(@Nonnull PullRequest pullRequest, @Nonnull final Tuple tuple)
        {
            final String username = tuple.get(participantMapping.USERNAME);
            final Boolean approved = tuple.get(participantMapping.APPROVED);
            final String role = tuple.get(participantMapping.ROLE);

            // We are left joining so only include the participant if it is available
            if (username != null || approved != null || role != null)
            {
                Participant participant = new Participant(username,
                        approved == null ? false : approved, role);

                if (pullRequest.getParticipants() == null)
                {
                    pullRequest.setParticipants(new ArrayList<Participant>());
                }

                if (!pullRequest.getParticipants().contains(participant))
                {
                    pullRequest.getParticipants().add(participant);
                }
            }
        }

        private void addIssueKey(@Nonnull PullRequest pullRequest, @Nonnull final Tuple tuple)
        {
            final String issueKey = tuple.get(issueMapping.ISSUE_KEY);

            if (!pullRequest.getIssueKeys().contains(issueKey))
            {
                pullRequest.getIssueKeys().add(issueKey);
            }
        }
    }
}
