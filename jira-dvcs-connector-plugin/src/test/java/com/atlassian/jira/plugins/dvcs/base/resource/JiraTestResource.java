package com.atlassian.jira.plugins.dvcs.base.resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.base.AbstractTestListener;
import com.atlassian.jira.plugins.dvcs.base.TestListenerDelegate;
import com.atlassian.jira.rest.api.issue.IssueCreateResponse;
import com.atlassian.jira.testkit.client.Backdoor;
import com.atlassian.jira.testkit.client.restclient.Issue;
import com.atlassian.jira.testkit.client.restclient.SearchRequest;
import com.atlassian.jira.testkit.client.restclient.SearchResult;
import com.atlassian.jira.testkit.client.util.TestKitLocalEnvironmentData;

/**
 * Provides JIRA test resource related functionality.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class JiraTestResource
{

    /**
     * Used for summary issue generation.
     */
    private final TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    /**
     * JIRA backdoor for direct REST access.
     */
    private final Backdoor backdoor = new Backdoor(new TestKitLocalEnvironmentData(new Properties(), "."));

    /**
     * All project keys, which were touched during tests. All touched project keys will be checked for expired issues - issues, which are
     * not necessary anymore by tests.
     */
    private final Set<String> projectKeys = new HashSet<String>();

    /**
     * Issues which was created within {@link Lifetime}.
     */
    private final Map<Lifetime, Set<String>> issueKeys = new HashMap<Lifetime, Set<String>>();

    /**
     * Lifetime of generated repository.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    public enum Lifetime
    {
        DURING_TEST_METHOD
    }

    /**
     * Constructor.
     * 
     * @param testListenerDelegate
     */
    public JiraTestResource(TestListenerDelegate testListenerDelegate)
    {
        testListenerDelegate.register(new AbstractTestListener()
        {

            @Override
            public void beforeMethod()
            {
                super.beforeMethod();
                JiraTestResource.this.beforeMethod();
            }

            @Override
            public void afterMethod()
            {
                super.afterMethod();
                JiraTestResource.this.afterMethod();
            }

            @Override
            public void afterClass()
            {
                super.afterClass();
                JiraTestResource.this.afterClass();
            }

        });
    }

    /**
     * Prepares staff related to single test method.
     */
    public void beforeMethod()
    {
        issueKeys.put(Lifetime.DURING_TEST_METHOD, new HashSet<String>());
    }

    /**
     * Cleans up staff related to single test method.
     */
    public void afterMethod()
    {
        for (String issueKey : issueKeys.remove(Lifetime.DURING_TEST_METHOD))
        {
            backdoor.issues().deleteIssue(issueKey, true);
        }
    }

    /**
     * Cleaning staff related to this resource.
     */
    public void afterClass()
    {
        for (String projectKey : projectKeys)
        {
            removeExpiredIssues(projectKey);
        }
    }

    /**
     * Creates issue for provided information.
     * 
     * @param projectKey
     *            under which project
     * @param summaryPrefix
     *            prefix for issue summary
     * @param lifetime
     *            of created issue
     * @param expirationDuration
     *            duration (expiration time), when can be removed the issue, even by some other test (if cleaning failed, this can be used
     *            by cleaning retry)
     * @return key of created issue
     */
    public String addIssue(String projectKey, String summaryPrefix, Lifetime lifetime, int expirationDuration)
    {
        projectKeys.add(projectKey);

        String summary = timestampNameTestResource.randomName(summaryPrefix, expirationDuration);
        IssueCreateResponse issue = backdoor.issues().createIssue(projectKey, summary);
        issueKeys.get(lifetime).add(issue.key);
        return issue.key;
    }

    /**
     * Removes all issues for provided project, which already expired.
     * 
     * @param projectKey
     *            for which project
     */
    private void removeExpiredIssues(String projectKey)
    {
        SearchResult result = backdoor.search().getSearch(new SearchRequest().jql("project = " + projectKey));
        for (Issue issue : result.issues)
        {
            if (timestampNameTestResource.isExpired(issue.fields.summary))
            {
                backdoor.issues().deleteIssue(issue.key, true);
            }
        }
    }
}
