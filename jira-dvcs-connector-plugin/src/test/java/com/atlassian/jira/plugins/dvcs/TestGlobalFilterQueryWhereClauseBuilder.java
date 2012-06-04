package com.atlassian.jira.plugins.dvcs;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.atlassian.jira.plugins.dvcs.dao.impl.GlobalFilterQueryWhereClauseBuilder;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;

/**
 *
 */
public class TestGlobalFilterQueryWhereClauseBuilder
{
    @Test
    public void emptyGlobalFilter()
    {
        final String expected = " true ";
        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(null);
        Assert.assertEquals("Expected clause '" + expected + "'", expected, globalFilterQueryWhereClauseBuilder.build());

        globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(null);
        Assert.assertEquals("Expected clause '" + expected + "'", expected, globalFilterQueryWhereClauseBuilder.build());
    }

    @Test
    public void fullGlobalFilter()
    {
        final String expected = "(PROJECT_KEY in ('projectIn')  AND PROJECT_KEY not in ('projectNotIn') ) AND (ISSUE_KEY in ('issueIn')  AND ISSUE_KEY not in ('issueNotIn') ) AND (AUTHOR in ('userIn') AUTHOR not in ('userNotIn') )";
        GlobalFilter gf = new GlobalFilter();
        gf.setInProjects(Arrays.asList("projectIn"));
        gf.setNotInProjects(Arrays.asList("projectNotIn"));
        gf.setInIssues(Arrays.asList("issueIn"));
        gf.setNotInIssues(Arrays.asList("issueNotIn"));
        gf.setInUsers(Arrays.asList("userIn"));
        gf.setNotInUsers(Arrays.asList("userNotIn"));

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        Assert.assertEquals("Expected clause '" + expected + "'", expected, globalFilterQueryWhereClauseBuilder.build());
    }
}
