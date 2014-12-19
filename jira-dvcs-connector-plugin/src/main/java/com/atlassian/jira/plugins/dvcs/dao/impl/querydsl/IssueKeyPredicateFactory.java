package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Iterables;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.IssueKeyedMapping;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.google.common.collect.Lists;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Factory to build Predicate objects given a collection of keys
 */
public class IssueKeyPredicateFactory
{
    private IssueKeyPredicateFactory()
    {
    }

    /**
     * Create a Predicate based on the supplied issue keys that respects the maximum size of an 'IN' statement as per
     * the constant defined in {!{@link com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils#SQL_IN_CLAUSE_MAX}}
     * @param issueKeys The issue keys to bind into the predicate
     * @param issueKeyedMapping The QueryDSL mapping class that supports issue key based queries
     * @return A predicate that is an 'IN' statement across the issue keys
     */
    @Nullable
    public static Predicate buildIssueKeyPredicate(@Nonnull final Iterable<String> issueKeys,
            @Nonnull final IssueKeyedMapping issueKeyedMapping)
    {
        final List<String> issueKeysList = Lists.newArrayList(issueKeys);

        if (issueKeysList.size() <= ActiveObjectsUtils.SQL_IN_CLAUSE_MAX)
        {
            return issueKeyedMapping.getIssueKeyExpression().in(issueKeysList);
        }

        List<List<String>> partititionedIssueKeys = Lists.partition(issueKeysList, ActiveObjectsUtils.SQL_IN_CLAUSE_MAX);

        BooleanExpression issueKeyPredicate = issueKeyedMapping.getIssueKeyExpression().in(partititionedIssueKeys.get(0));

        for (List<String> keys : Iterables.drop(1, partititionedIssueKeys))
        {
            issueKeyPredicate = issueKeyPredicate.or(issueKeyedMapping.getIssueKeyExpression().in(keys));
        }

        return issueKeyPredicate;
    }
}
