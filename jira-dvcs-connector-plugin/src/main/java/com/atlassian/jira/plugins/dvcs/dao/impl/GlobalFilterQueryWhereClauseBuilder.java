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
                for (String projectKey : gf.getInProjects())
                {
                    if (StringUtils.isBlank(projectKey))
                    {
                        continue;
                    }
                    if (whereClauseProjectsSb.length() != 0)
                    {
                        whereClauseProjectsSb.append(" OR ");
                    }
                    whereClauseProjectsSb.append("ISSUE_KEY like '").append(projectKey).append("-%'");
                }
            }
            if (gf.getNotInProjects() != null && gf.getNotInProjects().iterator().hasNext())
            {
                for (String projectKey : gf.getNotInProjects())
                {
                    if (StringUtils.isBlank(projectKey))
                    {
                        continue;
                    }
                    if (whereClauseProjectsSb.length() != 0)
                    {
                        whereClauseProjectsSb.append(" AND ");
                    }
                    whereClauseProjectsSb.append("ISSUE_KEY not like '").append(projectKey).append("-%'");
                }
            }

            if (gf.getInIssues() != null && gf.getInIssues().iterator().hasNext())
            {
                for (String issueKey : gf.getInIssues())
                {
                    if (StringUtils.isBlank(issueKey))
                    {
                        continue;
                    }
                    if (whereClauseIssueKyesSb.length() != 0)
                    {
                        whereClauseIssueKyesSb.append(" OR ");
                    }
                    whereClauseIssueKyesSb.append("ISSUE_KEY like '").append(issueKey.toUpperCase()).append("'");
                }
            }
            if (gf.getNotInIssues() != null && gf.getNotInIssues().iterator().hasNext())
            {
                for (String issueKey : gf.getNotInIssues())
                {
                    if (StringUtils.isBlank(issueKey))
                    {
                        continue;
                    }
                    if (whereClauseIssueKyesSb.length() != 0)
                    {
                        whereClauseIssueKyesSb.append(" AND ");
                    }
                    whereClauseIssueKyesSb.append("ISSUE_KEY not like '").append(issueKey.toUpperCase()).append("'");
                }
            }

            if (gf.getInUsers() != null && gf.getInUsers().iterator().hasNext())
            {
                for (String username : gf.getInUsers())
                {
                    if (StringUtils.isBlank(username))
                    {
                        continue;
                    }
                    if (whereClauseUsersSb.length() != 0)
                    {
                        whereClauseUsersSb.append(" OR ");
                    }
                    whereClauseUsersSb.append("AUTHOR like '").append(username).append("'");
                }
            }
            if (gf.getNotInUsers() != null && gf.getNotInUsers().iterator().hasNext())
            {
                for (String username : gf.getNotInUsers())
                {
                    if (StringUtils.isBlank(username))
                    {
                        continue;
                    }
                    if (whereClauseUsersSb.length() != 0)
                    {
                        whereClauseUsersSb.append(" AND ");
                    }
                    whereClauseUsersSb.append("AUTHOR not like '").append(username).append("'");
                }
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
}
