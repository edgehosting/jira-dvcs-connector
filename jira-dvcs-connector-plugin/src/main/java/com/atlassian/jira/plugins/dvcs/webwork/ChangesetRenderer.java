package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueAction;

import java.util.List;


public interface ChangesetRenderer {
    
    String DEFAULT_MESSAGE_TXT = "No commits found.";
    String DEFAULT_MESSAGE_GH_TXT = "There are no commits";
    GenericMessageAction DEFAULT_MESSAGE = new GenericMessageAction(DEFAULT_MESSAGE_TXT);


    List<IssueAction> getAsActions(Issue issue);

}
