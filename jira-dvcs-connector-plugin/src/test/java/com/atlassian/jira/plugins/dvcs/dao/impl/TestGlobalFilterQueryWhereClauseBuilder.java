package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.jira.plugins.dvcs.dao.impl.GlobalFilterQueryWhereClauseBuilder.SqlAndParams;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.api.Assertions.assertThat;

public class TestGlobalFilterQueryWhereClauseBuilder
{
    @Test
    public void nullGlobalFilter()
    {
        final String expected = " true ";
        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(null);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(0);
    }

    @Test
    public void emptyListsGlobalFilter()
    {
        final String expected = " true ";
        GlobalFilter gf = new GlobalFilter();
        gf.setInProjects(Collections.<String>emptyList());
        gf.setNotInProjects(Collections.<String>emptyList());
        gf.setInIssues(Collections.<String>emptyList());
        gf.setNotInIssues(Collections.<String>emptyList());
        gf.setInUsers(Collections.<String>emptyList());
        gf.setNotInUsers(Collections.<String>emptyList());

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(0);
    }

    @Test
    public void emptyGlobalFilter()
    {
        final String expected = " true ";
        GlobalFilter gf = new GlobalFilter();
        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(0);
    }

    @Test
    public void fullGlobalFilter()
    {
        final String expected = "(ISSUE.PROJECT_KEY IN (?)  AND ISSUE.PROJECT_KEY NOT IN (?) ) AND (ISSUE.ISSUE_KEY IN (?, ?)  AND ISSUE.ISSUE_KEY NOT IN (?, ?) ) AND (CHANGESET.AUTHOR IN (?) CHANGESET.AUTHOR NOT IN (?) )";
        GlobalFilter gf = new GlobalFilter();
        gf.setInProjects(Arrays.asList("projectIn"));
        gf.setNotInProjects(Arrays.asList("projectNotIn"));
        gf.setInIssues(Arrays.asList("issueIn1", "issueIn2"));
        gf.setNotInIssues(Arrays.asList("issueNotIn1", "issueNotIn2"));
        gf.setInUsers(Arrays.asList("userIn"));
        gf.setNotInUsers(Arrays.asList("userNotIn"));

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(8);
        assertThat((Object[]) built.getParams()).containsSequence(new Object [] {"projectIn", "projectNotIn", "issueIn1", "issueIn2", "issueNotIn1", "issueNotIn2", "userIn", "userNotIn"});
    }

    @Test
    public void testGlobalFilterWorksForIssueKeysOnly() throws Exception
    {
        final String expected = "(ISSUE.ISSUE_KEY IN (?, ?) )";
        GlobalFilter gf = new GlobalFilter();
        gf.setInIssues(Arrays.asList("issueIn", "issueIn2"));

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(2);
        assertThat((Object[]) built.getParams()).containsSequence(new Object [] {"issueIn", "issueIn2"});
    }
    
    @Test
    public void testGlobalFilterWorksForProjectKeysOnly() throws Exception
    {
        final String expected = "(ISSUE.PROJECT_KEY IN (?, ?) )";
        GlobalFilter gf = new GlobalFilter();
        gf.setInProjects(Arrays.asList("p1", "p2"));

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(2);
        assertThat((Object[]) built.getParams()).containsSequence(new Object [] {"p1", "p2"});
    }
    
    @Test
    public void testGlobalFilterWorksForUsersOnly() throws Exception
    {
        final String expected = "(CHANGESET.AUTHOR IN (?, ?) )";
        GlobalFilter gf = new GlobalFilter();
        gf.setInUsers(Arrays.asList("aut", "aut2"));

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(2);
        assertThat((Object[]) built.getParams()).containsSequence(new Object [] {"aut", "aut2"});
    }
    
    @Test
    public void testGlobalFilterWorksForNotInIssueKeysOnly() throws Exception
    {
        final String expected = "(ISSUE.ISSUE_KEY NOT IN (?, ?) )";
        GlobalFilter gf = new GlobalFilter();
        gf.setNotInIssues(Arrays.asList("issueIn", "issueIn2"));

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(2);
        assertThat((Object[]) built.getParams()).containsSequence(new Object [] {"issueIn", "issueIn2"});
    }
    
    @Test
    public void testGlobalFilterWorksForNotInProjectKeysOnly() throws Exception
    {
        final String expected = "(ISSUE.PROJECT_KEY NOT IN (?, ?) )";
        GlobalFilter gf = new GlobalFilter();
        gf.setNotInProjects(Arrays.asList("p1", "p2"));

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(2);
        assertThat((Object[]) built.getParams()).containsSequence(new Object [] {"p1", "p2"});
    }
    
    @Test
    public void testGlobalFilterWorksForNotInUsersOnly() throws Exception
    {
        final String expected = "(CHANGESET.AUTHOR NOT IN (?, ?) )";
        GlobalFilter gf = new GlobalFilter();
        gf.setNotInUsers(Arrays.asList("aut", "aut2"));

        GlobalFilterQueryWhereClauseBuilder globalFilterQueryWhereClauseBuilder = new GlobalFilterQueryWhereClauseBuilder(gf);
        SqlAndParams built = globalFilterQueryWhereClauseBuilder.build();
        assertThat(built.getSql()).isEqualTo(expected);
        assertThat(built.getParams().length).isEqualTo(2);
        assertThat((Object[]) built.getParams()).containsSequence(new Object [] {"aut", "aut2"});
    }
}
