package com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.plugins.dvcs.webwork.IssueLinker;
import com.atlassian.jira.plugins.dvcs.webwork.IssueLinkerImpl;
import com.atlassian.sal.api.ApplicationProperties;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class TestIssueLinker
{
    @Mock
    ApplicationProperties applicationProperty;
    
    @BeforeMethod
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(applicationProperty.getBaseUrl()).thenReturn("http://jirabaseurl/jira");
    }
    
    @Test
    public void anonymous()
    {
        IssueLinker issueLinker = new IssueLinkerImpl(applicationProperty);
        
        assertThat(issueLinker.createLinks("XXX-1"))
                .isEqualTo("<a href=\"http://jirabaseurl/jira/browse/XXX-1\">XXX-1</a>");
        assertThat(issueLinker.createLinks("XXX-1 XYZ-1"))
                .isEqualTo("<a href=\"http://jirabaseurl/jira/browse/XXX-1\">XXX-1</a> <a href=\"http://jirabaseurl/jira/browse/XYZ-1\">XYZ-1</a>");
        assertThat(issueLinker.createLinks("Fixing issues XXX-1,XXX-2 and XX-3, done."))
                .isEqualTo("Fixing issues <a href=\"http://jirabaseurl/jira/browse/XXX-1\">XXX-1</a>,<a href=\"http://jirabaseurl/jira/browse/XXX-2\">XXX-2</a> and <a href=\"http://jirabaseurl/jira/browse/XX-3\">XX-3</a>, done.");
    }
}
