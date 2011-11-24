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
import com.atlassian.webdriver.jira.JiraTestedProduct;
import org.openqa.selenium.By;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the page to link repositories to projects
 */
public abstract class BaseConfigureRepositoriesPage implements Page
{
    @Inject
    PageBinder pageBinder;

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
     * The current sync status message
     *
     * @return Sync status message
     */

    public String getSyncStatusMessage()
    {
        return syncStatusDiv.getText();
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
    public abstract BaseConfigureRepositoriesPage addRepoToProjectFailing(String projectKey, String url);
    public abstract BaseConfigureRepositoriesPage addPrivateRepoToProjectSuccessfully(String projectKey, String url);
    public abstract String addPublicRepoToProjectAndInstallService(String projectKey, String url, String adminUsername, String adminPassword);

    public void setJiraTestedProduct(JiraTestedProduct jiraTestedProduct)
    {
        this.jiraTestedProduct = jiraTestedProduct;
    }
}
