package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.hamcrest.text.StringStartsWith;
import org.openqa.selenium.By;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketRepository;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import com.atlassian.webdriver.jira.JiraTestedProduct;

/**
 * Represents the page to link repositories to projects
 */
public abstract class BaseConfigureRepositoriesPage implements Page
{
    @Inject
    PageBinder pageBinder;

    @ElementBy(id = "linkRepositoryButton")
    PageElement linkRepositoryButton;

    @ElementBy(id = "Submit")
    PageElement addRepositoryButton;

    @ElementBy(name = "projectKey")
    SelectElement projectSelect;

    @ElementBy(id = "url")
    PageElement urlTextbox;

    @ElementBy(className = "gh_messages")
    PageElement syncStatusDiv;

    @ElementBy(className = "gh_table")
    PageElement projectsTable;

    @ElementBy(id = "addedRepositoryId")
    PageElement addedRepositoryIdSpan;

    @ElementBy(id = "aui-message-bar")
    PageElement messageBarDiv;

    protected JiraTestedProduct jiraTestedProduct;


    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureBitbucketRepositories!default.jspa";
    }

//    @WaitUntil
//    public void waitUntilReady()
//    {
//        Poller.waitUntilTrue(addRepositoryButton.timed().isPresent());
//    }


    /**
     * Returns a list of <tt>BitBucketRepository</tt> with the current list of repositories linked.
     *
     * @return List of <tt>BitBucketRepository</tt>
     */
    public List<BitBucketRepository> getRepositories()
    {
        List<BitBucketRepository> list = new ArrayList<BitBucketRepository>();
        String projectKey = null;
        for (PageElement row : projectsTable.findAll(By.tagName("tr")))
        {
            if (row.find(By.className("gh_table_project_key")).isPresent())
            {
                String projectKeyBracketed = row.find(By.className("gh_table_project_key")).getText();
                projectKey = projectKeyBracketed.substring(1, projectKeyBracketed.length() - 1);
            }
            if (row.getText().contains("Force Sync"))
            {
                list.add(pageBinder.bind(BitBucketRepository.class, row, projectKey));
            }
        }

        return list;
    }

    /**
     * Deletes all repositories
     *
     * @return BitBucketConfigureRepositoriesPage
     */
    public BaseConfigureRepositoriesPage deleteAllRepositories()
    {
        List<BitBucketRepository> repos;
        while (!(repos = getRepositories()).isEmpty())
        {
            repos.get(0).delete();
        }

        return this;
    }

    /**
     * Whether a repository is currently linked to a given project
     *
     * @param projectKey The JIRA project key
     * @param url        The repository url
     * @return True if repository is linked, false otherwise
     */
    public boolean isRepositoryPresent(String projectKey, String url)
    {
        boolean commitFound = false;
        for (BitBucketRepository repo : getRepositories())
        {
            if (repo.getProjectKey().equals(projectKey) && repo.getUrl().equals(url))
            {
                commitFound = true;
                break;
            }
        }

        return commitFound;
    }

    /**
     * @param matcher
     */
    public void assertThatSyncMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(syncStatusDiv.timed().getText(), matcher);
    }

    public void assertThatSuccessMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("success")).timed().getText(), matcher);
    }

    public void assertThatWarningMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("warning")).timed().getText(), matcher);
    }

    public void assertThatErrorMessage(Matcher<String> matcher)
    {
        Poller.waitUntil(messageBarDiv.find(By.className("error")).timed().getText(), matcher);
    }

    protected void checkSyncProcessSuccess()
    {
        // isPresent = true => repositories list is shown
        TimedCondition isMsgVisibleCond = syncStatusDiv.timed().isPresent();
        Poller.waitUntilTrue("Expected sync status message to appear.", isMsgVisibleCond);

        // isVisible = true => started sync => we will wait for result
        if (syncStatusDiv.timed().isVisible().now())
        {
            TimedQuery<String> syncFinishedCond = syncStatusDiv.timed().getText();
            Poller.waitUntil("Expected sync status message to be 'Sync Finished'", syncFinishedCond, new StringStartsWith("last commit"));
        }
    }

    protected void waitFormBecomeVisible()
    {
        try
        {
            Thread.sleep(2000);
        } catch (InterruptedException e)
        {
            // not important state
        }
    }

    /**
     * The current error status message
     *
     * @return error status message
     */

    public String getErrorStatusMessage()
    {
        return messageBarDiv.find(By.className("error")).timed().getText().now();
    }

    public abstract BaseConfigureRepositoriesPage addRepoToProjectFailingStep1(String projectKey, String url);

    public abstract BaseConfigureRepositoriesPage addRepoToProjectFailingStep2(String projectKey, String url);

    public abstract BaseConfigureRepositoriesPage addRepoToProjectFailingPostcommitService(String projectKey, String url);

    public abstract BaseConfigureRepositoriesPage addRepoToProjectSuccessfully(String projectKey, String url);

    public abstract String addPublicRepoToProjectAndInstallService(String projectKey, String url, String adminUsername, String adminPassword);

    public void setJiraTestedProduct(JiraTestedProduct jiraTestedProduct)
    {
        this.jiraTestedProduct = jiraTestedProduct;
    }

    public void clearForm()
    {
        urlTextbox.clear();
    }

}
