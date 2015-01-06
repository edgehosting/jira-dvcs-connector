package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Function2;
import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.dao.impl.BranchDaoImpl;
import com.atlassian.jira.plugins.dvcs.dao.impl.QueryDslFeatureHelper;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QBranchMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QIssueToBranchMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QOrganizationMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.atlassian.pocketknife.api.querydsl.SelectQuery;
import com.atlassian.pocketknife.api.querydsl.StreamyResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.mysema.query.Tuple;
import com.mysema.query.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.dao.impl.DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * An implementation of {@link com.atlassian.jira.plugins.dvcs.dao.BranchDao} that has a delegate {@link
 * com.atlassian.jira.plugins.dvcs.dao.impl.BranchDaoImpl} and uses Query DSL for #getBranchesForIssue if the dark
 * feature is set.
 */
@SuppressWarnings ("SpringJavaAutowiringInspection")
@Component ("branchDaoQueryDsl")
public class BranchDaoQueryDsl implements BranchDao
{
    private final QueryFactory queryFactory;
    private final SchemaProvider schemaProvider;
    private final BranchDaoImpl branchDao;
    private final QueryDslFeatureHelper queryDslFeatureHelper;

    @Autowired
    public BranchDaoQueryDsl(@Nonnull final QueryFactory queryFactory, @Nonnull final SchemaProvider schemaProvider,
            @Nonnull final BranchDaoImpl branchDao, @Nonnull final QueryDslFeatureHelper queryDslFeatureHelper)
    {
        this.queryFactory = checkNotNull(queryFactory);
        this.schemaProvider = checkNotNull(schemaProvider);
        this.branchDao = checkNotNull(branchDao);
        this.queryDslFeatureHelper = checkNotNull(queryDslFeatureHelper);
    }

    @Override
    public void createBranchHead(final int repositoryId, final BranchHead branchHead)
    {
        branchDao.createBranchHead(repositoryId, branchHead);
    }

    @Override
    public List<BranchHead> getBranchHeads(final int repositoryId)
    {
        return branchDao.getBranchHeads(repositoryId);
    }

    @Override
    public void removeBranchHead(final int repositoryId, final BranchHead branchHead)
    {
        branchDao.removeBranchHead(repositoryId, branchHead);
    }

    @Override
    public void removeAllBranchHeadsInRepository(final int repositoryId)
    {
        branchDao.removeAllBranchHeadsInRepository(repositoryId);
    }

    @Override
    public List<Branch> getBranchesForIssue(final Iterable<String> issueKeys)
    {
        return branchDao.getBranchesForIssue(issueKeys);
    }

    @Override
    public List<Branch> getBranches(final int repositoryId)
    {
        return branchDao.getBranches(repositoryId);
    }

    @Override
    public void createBranch(final int repositoryId, final Branch branch, final Set<String> issueKeys)
    {
        branchDao.createBranch(repositoryId, branch, issueKeys);
    }

    @Override
    public void removeBranch(final int repositoryId, final Branch branch)
    {
        branchDao.removeBranch(repositoryId, branch);
    }

    @Override
    public void removeAllBranchesInRepository(final int repositoryId)
    {
        branchDao.removeAllBranchesInRepository(repositoryId);
    }

    @Override
    public List<Branch> getBranchesForRepository(final int repositoryId)
    {
        return branchDao.getBranchesForRepository(repositoryId);
    }

    /**
     * Retrieve branches associated with the supplied issue keys and dvcsType, if the Query DSL dark feature is enabled
     * it /* will use Query DSL otherwise it will use the delegate.
     *
     * @param issueKeys The issue keys to query by
     * @param dvcsType The optional dvcsType to restrict to
     * @return Branches associated with the supplied issue keys and dvcsType
     */
    @Nonnull
    @Override
    public List<Branch> getBranchesForIssue(@Nonnull final Iterable<String> issueKeys, @Nullable final String dvcsType)
    {
        if (queryDslFeatureHelper.isRetrievalUsingQueryDSLEnabled())
        {
            PullRequestByIssueKeyClosure closure = new PullRequestByIssueKeyClosure(dvcsType, issueKeys, schemaProvider);
            Map<Integer, Branch> result = queryFactory.halfStreamyFold(new HashMap<Integer, Branch>(), closure);

            return ImmutableList.copyOf(result.values());
        }
        return branchDao.getBranchesForIssue(issueKeys, dvcsType);
    }

    @VisibleForTesting
    static class PullRequestByIssueKeyClosure implements QueryFactory.HalfStreamyFoldClosure<Map<Integer, Branch>>
    {
        private final String dvcsType;
        private final Iterable<String> issueKeys;
        private final QBranchMapping branchMapping;
        private final QIssueToBranchMapping issueMapping;
        private final QRepositoryMapping repositoryMapping;
        private final QOrganizationMapping orgMapping;

        PullRequestByIssueKeyClosure(@Nullable final String dvcsType, @Nonnull final Iterable<String> issueKeys,
                @Nonnull final SchemaProvider schemaProvider)
        {
            this.dvcsType = dvcsType;
            this.issueKeys = issueKeys;
            this.branchMapping = QBranchMapping.withSchema(checkNotNull(schemaProvider));
            this.issueMapping = QIssueToBranchMapping.withSchema(checkNotNull(schemaProvider));
            this.repositoryMapping = QRepositoryMapping.withSchema(checkNotNull(schemaProvider));
            this.orgMapping = QOrganizationMapping.withSchema(checkNotNull(schemaProvider));
        }

        @Override
        public Function<SelectQuery, StreamyResult> getQuery()
        {
            return new Function<SelectQuery, StreamyResult>()
            {
                @Override
                public StreamyResult apply(@Nullable final SelectQuery select)
                {
                    final Predicate predicate = IssueKeyPredicateFactory.buildIssueKeyPredicate(issueKeys, issueMapping);

                    SelectQuery sql = select.from(branchMapping)
                            .join(issueMapping).on(branchMapping.ID.eq(issueMapping.BRANCH_ID))
                            .join(repositoryMapping).on(repositoryMapping.ID.eq(branchMapping.REPOSITORY_ID))
                            .join(orgMapping).on(orgMapping.ID.eq(repositoryMapping.ORGANIZATION_ID))
                            .where(repositoryMapping.DELETED.eq(false)
                                    .and(repositoryMapping.LINKED.eq(true))
                                    .and(predicate))
                            .orderBy(branchMapping.NAME.asc());

                    if (isNotBlank(dvcsType))
                    {
                        sql = sql.where(orgMapping.DVCS_TYPE.eq(dvcsType));
                    }

                    return sql.stream(
                            branchMapping.ID,
                            branchMapping.NAME,
                            branchMapping.REPOSITORY_ID,
                            issueMapping.ISSUE_KEY);
                }
            };
        }

        @Override
        public Function2<Map<Integer, Branch>, Tuple, Map<Integer, Branch>> getFoldFunction()
        {
            return new Function2<Map<Integer, Branch>, Tuple, Map<Integer, Branch>>()
            {
                @Override
                public Map<Integer, Branch> apply(@Nonnull final Map<Integer, Branch> integerBranchMap, @Nonnull final Tuple tuple)
                {
                    Integer id = tuple.get(branchMapping.ID);
                    Branch branch = integerBranchMap.get(id);
                    if (branch == null)
                    {
                        // Due to the denormalised query to limit the result we skip any records we find after we reach the limit
                        if (integerBranchMap.size() >= MAXIMUM_ENTITIES_PER_ISSUE_KEY)
                        {
                            return integerBranchMap;
                        }

                        branch = new Branch(tuple.get(branchMapping.ID), tuple.get(branchMapping.NAME),
                                tuple.get(branchMapping.REPOSITORY_ID));
                        integerBranchMap.put(id, branch);
                    }
                    String issueKey = tuple.get(issueMapping.ISSUE_KEY);

                    if (!branch.getIssueKeys().contains(issueKey))
                    {
                        branch.getIssueKeys().add(issueKey);
                    }

                    return integerBranchMap;
                }
            };
        }
    }
}
