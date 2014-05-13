package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.google.common.collect.Lists;

import java.util.Collection;

/**
 *
 */
public class GlobalFilterQueryWhereClauseBuilder
{
    private final GlobalFilter gf;

    public GlobalFilterQueryWhereClauseBuilder(GlobalFilter gf)
    {
        this.gf = gf;
    }

    public SqlAndParams build()
    {
        Collection<Object> params = Lists.newArrayList();

        StringBuilder whereClauseProjectsSb = new StringBuilder();
        StringBuilder whereClauseIssueKyesSb = new StringBuilder();
        StringBuilder whereClauseUsersSb = new StringBuilder();

        if (gf != null)
        {

            if (gf.getInProjects() != null && gf.getInProjects().iterator().hasNext())
            {
                whereClauseProjectsSb.append(renderSqlIn("ISSUE." + IssueToChangesetMapping.PROJECT_KEY, gf.getInProjects())).append(" ");
                params.addAll(Lists.newArrayList(gf.getInProjects()));
            }
            //
            if (gf.getNotInProjects() != null && gf.getNotInProjects().iterator().hasNext())
            {
                if (whereClauseProjectsSb.length() != 0)
                {
                    whereClauseProjectsSb.append(" AND ");
                }
                whereClauseProjectsSb.append(renderSqlNotIn("ISSUE." + IssueToChangesetMapping.PROJECT_KEY, gf.getNotInProjects())).append(" ");
                params.addAll(Lists.newArrayList(gf.getNotInProjects()));
            }
            //
            if (gf.getInIssues() != null && gf.getInIssues().iterator().hasNext())
            {
                whereClauseIssueKyesSb.append(renderSqlIn("ISSUE." + IssueToChangesetMapping.ISSUE_KEY, gf.getInIssues())).append(" ");
                params.addAll(Lists.newArrayList(gf.getInIssues()));
            }
            //
            if (gf.getNotInIssues() != null && gf.getNotInIssues().iterator().hasNext())
            {
                if (whereClauseIssueKyesSb.length() != 0)
                {
                    whereClauseIssueKyesSb.append(" AND ");
                }
                whereClauseIssueKyesSb.append(renderSqlNotIn("ISSUE." + IssueToChangesetMapping.ISSUE_KEY, gf.getNotInIssues())).append(" ");
                params.addAll(Lists.newArrayList(gf.getNotInIssues()));
            }
            //
            if (gf.getInUsers() != null && gf.getInUsers().iterator().hasNext())
            {
                whereClauseUsersSb.append(renderSqlIn("CHANGESET." + ChangesetMapping.AUTHOR, gf.getInUsers())).append(" ");
                params.addAll(Lists.newArrayList(gf.getInUsers()));
            }
            //
            if (gf.getNotInUsers() != null && gf.getNotInUsers().iterator().hasNext())
            {
                whereClauseUsersSb.append(renderSqlNotIn("CHANGESET." + ChangesetMapping.AUTHOR, gf.getNotInUsers())).append(" ");
                params.addAll(Lists.newArrayList(gf.getNotInUsers()));
            }
        }
       
        StringBuilder whereClauseSb = new StringBuilder();
        if (whereClauseProjectsSb.length() != 0)
        {
            whereClauseSb.append("(").append(whereClauseProjectsSb.toString()).append(")");
        }
        if (whereClauseIssueKyesSb.length() != 0)
        {
            if (whereClauseSb.length() != 0)
            {
                whereClauseSb.append(" AND ");
            }
            whereClauseSb.append("(").append(whereClauseIssueKyesSb.toString()).append(")");
        }
        if (whereClauseUsersSb.length() != 0)
        {
            if (whereClauseSb.length() != 0)
            {
                whereClauseSb.append(" AND ");
            }
            whereClauseSb.append("(").append(whereClauseUsersSb.toString()).append(")");
        }

        // if no filter applied than "no" where clause should be used
        if (whereClauseSb.length() == 0)
        {
            whereClauseSb.append(" true ");
        }

        return new SqlAndParams(whereClauseSb.toString(), params.toArray());
    }

    private StringBuilder renderSqlNotIn(final String column, final Iterable<String> values)
    {
        return ActiveObjectsUtils.renderListOperator(column, "NOT IN", "AND", values);
    }

    private StringBuilder renderSqlIn(final String column, final Iterable<String> values)
    {
        return ActiveObjectsUtils.renderListOperator(column, "IN", "OR", values);
    }
    
    static class SqlAndParams
    {
        private String sql;
        private Object [] params;
        private SqlAndParams(String sql, Object[] params)
        {
            super();
            this.sql = sql;
            this.params = params;
        }
        public String getSql()
        {
            return sql;
        }
        public Object[] getParams()
        {
            return params;
        }
    }
}
