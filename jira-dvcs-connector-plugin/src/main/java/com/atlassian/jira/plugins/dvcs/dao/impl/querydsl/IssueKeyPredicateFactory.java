package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.fugue.Iterables;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.IssueKeyedMapping;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.google.common.collect.Lists;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;

import java.util.List;

/**
 * Factory to build Predicate objects given a collection of keys and a class that can supply the IssueKey expression
 */
public class IssueKeyPredicateFactory
{
    public static Predicate buildIssueKeyPredicate(final Iterable<String> issueKeys, final IssueKeyedMapping issueKeyedMapping)
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
