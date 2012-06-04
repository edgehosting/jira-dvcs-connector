package com.atlassian.jira.plugins.dvcs;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.dvcs.webwork.IssueLinker;
import com.atlassian.jira.plugins.dvcs.webwork.IssueLinkerImpl;
import com.atlassian.sal.api.ApplicationProperties;

public class TestIssueLinker
{
    @Mock
    ApplicationProperties applicationProperty;
    
    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(applicationProperty.getBaseUrl()).thenReturn("http://jirabaseurl/jira");
    }
    
    @Test
    public void anonymous()
    {
        IssueLinker issueLinker = new IssueLinkerImpl(applicationProperty);
        
        assertEquals("<a href=\"http://jirabaseurl/jira/browse/XXX-1\">XXX-1</a>",issueLinker.createLinks("XXX-1"));
        assertEquals("<a href=\"http://jirabaseurl/jira/browse/XXX-1\">XXX-1</a> <a href=\"http://jirabaseurl/jira/browse/XYZ-1\">XYZ-1</a>",issueLinker.createLinks("XXX-1 XYZ-1"));
        assertEquals(
            "Fixing issues <a href=\"http://jirabaseurl/jira/browse/XXX-1\">XXX-1</a>,<a href=\"http://jirabaseurl/jira/browse/XXX-2\">XXX-2</a> and <a href=\"http://jirabaseurl/jira/browse/XX-3\">XX-3</a>, done.",
            issueLinker.createLinks("Fixing issues XXX-1,XXX-2 and XX-3, done."));
        
    }
}
