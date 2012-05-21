package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import org.apache.commons.lang.StringUtils;

/**
 *
 */
public class GlobalFilterQueryWhereClauseBuilder
{
    private GlobalFilter gf;

    public GlobalFilterQueryWhereClauseBuilder(GlobalFilter gf)
    {
        this.gf = gf;
    }

    public String build()
    {
        StringBuilder whereClauseProjectsSb = new StringBuilder();
        StringBuilder whereClauseIssueKyesSb = new StringBuilder();
        StringBuilder whereClauseUsersSb = new StringBuilder();
        if (gf != null)
        {

            if (gf.getInProjects() != null && gf.getInProjects().iterator().hasNext())
            {
                whereClauseProjectsSb.append("PROJECT_KEY in ").append(joinStrigsToSet(gf.getInProjects())).append(" ");
            }
            if (gf.getNotInProjects() != null && gf.getNotInProjects().iterator().hasNext())
            {
                if (whereClauseProjectsSb.length() != 0)
                {
                    whereClauseProjectsSb.append(" AND ");
                }


                whereClauseProjectsSb.append("PROJECT_KEY not in ").append(joinStrigsToSet(gf.getNotInProjects())).append(" ");
            }

            if (gf.getInIssues() != null && gf.getInIssues().iterator().hasNext())
            {
                whereClauseIssueKyesSb.append("ISSUE_KEY in ").append(joinStrigsToSet(gf.getInIssues())).append(" ");
            }
            if (gf.getNotInIssues() != null && gf.getNotInIssues().iterator().hasNext())
            {
                if (whereClauseIssueKyesSb.length() != 0)
                {
                    whereClauseIssueKyesSb.append(" AND ");
                }


                whereClauseIssueKyesSb.append("ISSUE_KEY not in ").append(joinStrigsToSet(gf.getNotInIssues())).append(" ");
            }

            if (gf.getInUsers() != null && gf.getInUsers().iterator().hasNext())
            {
                whereClauseUsersSb.append("AUTHOR in ").append(joinStrigsToSet(gf.getInUsers())).append(" ");
            }
            if (gf.getNotInUsers() != null && gf.getNotInUsers().iterator().hasNext())
            {
                whereClauseUsersSb.append("AUTHOR not in ").append(joinStrigsToSet(gf.getInUsers())).append(" ");
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

        // if no filter applyied than "no" where clause should be used
        if (whereClauseSb.length() == 0)
        {
            whereClauseSb.append(" true ");
        }
        return whereClauseSb.toString();
    }

    private StringBuilder joinStrigsToSet(Iterable<String> strings)
    {
        StringBuilder builder = new StringBuilder("(");
        for (String string : strings)
        {
            if(StringUtils.isEmpty(string)) {
                continue;
            }
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append("'").append(string).append("'");
        }
        builder.append(")");

        return builder;
    }
}
