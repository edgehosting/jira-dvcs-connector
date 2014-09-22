package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.PageController;

import java.util.List;

public class JiraViewIssuePageController implements PageController<JiraViewIssuePage>
{
    private final JiraViewIssuePage page;

    public JiraViewIssuePageController(JiraTestedProduct jira, String issueKey)
    {
        this.page = jira.visit(JiraViewIssuePage.class, issueKey);
    }

    @Override
    public JiraViewIssuePage getPage()
    {
        return page;
    }

    public List<BitBucketCommitEntry> getCommits(int expectedNumberOfCommits)
    {
        return page.openBitBucketPanel()
                .waitForNumberOfMessages(expectedNumberOfCommits, 1000L, 5);
    }
}
