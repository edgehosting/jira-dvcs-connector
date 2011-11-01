package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketRepository;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.CheckboxElement;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 * Represents the page to link repositories to projects
 */
public class BitBucketConfigureRepositoriesPage implements Page
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

    @ElementBy(id = "repoVisibility")
    SelectElement visibilitySelect;

    @ElementBy(id = "addPostCommitService")
    CheckboxElement addPostCommitServiceCheckbox;

    @ElementBy(name = "sync_status_message")
    PageElement syncStatusDiv;

    @ElementBy(className = "gh_table")
    PageElement projectsTable;

    @ElementBy(id = "addedRepositoryId")
    PageElement addedRepositoryIdSpan;

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
     * Links a public repository to the given JIRA project
     * @param projectKey The JIRA project key
     * @param url The url to the bitucket public repo
     * @param adminUsername username used to install the service (postcommit hook)
     * @param adminPassword password used to install the service (postcommit hook)
     * @return BitBucketConfigureRepositoriesPage
     */
    public String addPublicRepoToProjectAndInstallService(String projectKey, String url, String adminUsername,
            String adminPassword)
    {
        urlTextbox.clear().type(url);
        projectSelect.select(Options.value(projectKey));
        visibilitySelect.select(Options.value("public"));
        // postcommit hook
        addPostCommitServiceCheckbox.click();
        adminUsernameTextbox.clear().type(adminUsername);
        adminPasswordTextbox.clear().type(adminPassword);
        // add
        addRepositoryButton.click();

        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());

        Poller.waitUntilTrue("Expected sync status message to be 'Sync Processing Complete'",
                syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Finished:"));

        return addedRepositoryIdSpan.timed().getValue().byDefaultTimeout();
    }

    /**
     * Links a public repository to the given JIRA project
     * @param projectKey The JIRA project key
     * @param url The url to the bitucket public repo
     * @return BitBucketConfigureRepositoriesPage
     */
    public BitBucketConfigureRepositoriesPage addPublicRepoToProject(String projectKey, String url)
    {
        projectSelect.select(Options.value(projectKey));
        visibilitySelect.select(Options.value("public"));
        urlTextbox.clear().type(url);
        addRepositoryButton.click();

        Poller.waitUntilTrue("Expected sync status message to appear.", syncStatusDiv.timed().isVisible());

        Poller.waitUntilTrue("Expected sync status message to be 'Sync Processing Complete'",
                syncStatusDiv.find(By.tagName("strong")).timed().hasText("Sync Finished:"));

        return this;
    }

    /**
     * Returns a list of <tt>BitBucketRepository</tt> with the current list of repositories linked.
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
     * @return BitBucketConfigureRepositoriesPage
     */
    public BitBucketConfigureRepositoriesPage deleteAllRepositories()
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
     * @param projectKey The JIRA project key
     * @param url The repository url
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
     * @return Sync status message
     */

    public String getSyncStatusMessage()
    {
        return syncStatusDiv.getText();
    }

}
