package com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.streams.GlobalFilter;
import com.atlassian.jira.plugins.bitbucket.streams.GlobalFilterQueryWhereClauseBuilder;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

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
        final String expected = "(ISSUE_ID like 'projectIn-%' AND ISSUE_ID not like 'projectNotIn-%') AND (ISSUE_ID like 'ISSUEIN' AND ISSUE_ID not like 'ISSUENOTIN') AND (AUTHOR like 'userIn' AND AUTHOR not like 'userNotIn')";
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
