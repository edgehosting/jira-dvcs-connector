package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.mysema.query.types.expr.SimpleExpression;

/**
 *  Interface that can be added to a QueryDSL mapping that indicates it has an 'issueKey' property, used in
 *  {@link com.atlassian.jira.plugins.dvcs.dao.impl.querydsl.IssueKeyPredicateFactory}
 */
public interface IssueKeyedMapping
{
    /**
     * Return the Expression that corresponds to the issue key
     * @return the Expression that corresponds to the issue key
     */
    SimpleExpression getIssueKeyExpression();
}
