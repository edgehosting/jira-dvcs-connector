package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketRepository;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import org.hamcrest.Matcher;
import org.openqa.selenium.By;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.atlassian.pageobjects.elements.query.Poller.by;

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

    @ElementBy(id = "adminUsername")
    PageElement adminUsernameTextbox;

    @ElementBy(id = "adminPassword")
    PageElement adminPasswordTextbox;

    @ElementBy(id = "addPostCommitService")
    CheckboxElement addPostCommitServiceCheckbox;

    @ElementBy(name = "sync_status_message")
    PageElement syncStatusDiv;

    @ElementBy(className = "gh_table")
    PageElement projectsTable;

    @ElementBy(id = "addedRepositoryId")
    PageElement addedRepositoryIdSpan;

    @ElementBy(tagName = "h2")
    PageElement addedRepositoryH2;

    @ElementBy(id = "aui-message-bar")
    PageElement messageBarDiv;

    protected JiraTestedProduct jiraTestedProduct;


    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureBitbucketRepositories!default.jspa";
    }

    @WaitUntil
    public void waitUntilReady()
    {
        Poller.waitUntilTrue(addRepositoryButton.timed().isPresent());
    }


    /**
     * Returns a list of <tt>BitBucketRepository</tt> with the current list of repositories linked.
     *
     * @return List of <tt>BitBucketRepository</tt>
     */
    public List<BitBucketRepository> getRepositories()
    {
        List<BitBucketRepository> list = new ArrayList<BitBucketRepository>();
        for (PageElement row : projectsTable.findAll(By.tagName("tr")))
        {
            if (row.getText().contains("Force Sync"))
            {
                list.add(pageBinder.bind(BitBucketRepository.class, row));
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
        Poller.waitUntil(syncStatusDiv.timed().getText(), matcher, by(30000));
    }

    protected void checkSyncProcessSuccess()
    {
        final String statusXpath = "//div[@name='sync_status_message']/div[@class='content']/strong";
        TimedCondition isMsgVisibleCond = syncStatusDiv.find(By.xpath(statusXpath)).timed().isVisible();
        Poller.waitUntilTrue("Expected sync status message to appear.", isMsgVisibleCond);

        TimedCondition syncFinishedCond = syncStatusDiv.find(By.xpath(statusXpath)).timed().hasText("Sync Finished:");
        Poller.waitUntilTrue("Expected sync status message to be 'Sync Finished'", syncFinishedCond);
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

    public abstract BaseConfigureRepositoriesPage addPublicRepoToProjectSuccessfully(String projectKey, String url);

    public abstract BaseConfigureRepositoriesPage addRepoToProjectFailingStep1(String projectKey, String url);

    public abstract BaseConfigureRepositoriesPage addRepoToProjectFailingStep2(String projectKey, String url);

    public abstract BaseConfigureRepositoriesPage addPrivateRepoToProjectSuccessfully(String projectKey, String url);

    public abstract String addPublicRepoToProjectAndInstallService(String projectKey, String url, String adminUsername, String adminPassword);

    public void setJiraTestedProduct(JiraTestedProduct jiraTestedProduct)
    {
        this.jiraTestedProduct = jiraTestedProduct;
    }

}
