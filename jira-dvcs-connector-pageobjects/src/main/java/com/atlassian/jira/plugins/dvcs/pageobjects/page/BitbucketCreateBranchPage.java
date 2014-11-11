package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.Options;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;

import java.util.concurrent.TimeUnit;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.core.Is.is;

/**
 * Represents bitbucket's 'create branch' page and allows creating branches for a repository via the web ui
 */
public class BitbucketCreateBranchPage implements Page
{
    @ElementBy (id = "id_repository")
    SelectElement repositorySelect;
    @ElementBy (id = "branch-dropdown")
    SelectElement branchSelect;
    @ElementBy (id = "id_branch_name")
    PageElement branchNameField;
    @ElementBy (cssSelector = ".buttons-container .submit")
    PageElement createBranchButton;

    public BitbucketCreateBranchPage()
    {
    }

    @WaitUntil
    public void waitUntilVisible() {
        waitUntilTrue(repositorySelect.timed().isVisible());
        waitUntilTrue(branchSelect.timed().isVisible());
        waitUntilTrue(branchNameField.timed().isVisible());
        waitUntilTrue(createBranchButton.timed().isVisible());
    }

    public String getUrl()
    {
        return "https://bitbucket.org/branch/create";
    }

    public void createBranch(String repository, String baseBranch, String branchName)
    {
        repositorySelect.select(Options.value(repository));
        waitUntilTrue(branchSelect.timed().isEnabled());
        branchSelect.select(Options.value(baseBranch));
        this.branchNameField.clear().type(branchName);
        waitUntilTrue(createBranchButton.timed().isEnabled());
        createBranchButton.click();
        Poller.waitUntil(createBranchButton.timed().isPresent(), is(false), by(15, TimeUnit.SECONDS));
    }
}
