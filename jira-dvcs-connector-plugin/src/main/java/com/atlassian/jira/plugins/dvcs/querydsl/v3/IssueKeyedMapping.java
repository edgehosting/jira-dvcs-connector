package com.atlassian.jira.plugins.dvcs.querydsl.v3;

import com.mysema.query.types.expr.SimpleExpression;

public interface IssueKeyedMapping
{
    SimpleExpression getIssueKeyExpression();
}
