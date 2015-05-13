package com.atlassian.jira.plugins.dvcs.pageobjects.component;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccountControlsDialog;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilFalse;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.is;

public class OrganizationDiv
{
    private static final String DYNAMIC_REPOSITORIES_PREFIX = "it.restart";

    @Inject
    private PageBinder pageBinder;

    @Inject
    private PageElementFinder elementFinder;

    private final PageElement rootElement;
    private final PageElement repositoriesTable;
    private final PageElement organizationType;
    private final PageElement organizationName;
    private PageElement controlsButton;

    public OrganizationDiv(PageElement row)
    {
        this.rootElement = row;
        this.repositoriesTable = rootElement.find(By.tagName("table"));
        this.organizationType = rootElement.find(By.xpath("div/h4"));
        this.organizationName = rootElement.find(By.xpath("div/h4/a"));
        this.controlsButton = rootElement.find(By.xpath(".//button[contains(concat(' ', @class, ' '), ' aui-dropdown2-trigger ')]"));
    }

    /**
     * Deletes this repository from the list
     */
    public void delete()
    {
        // add marker to wait for post complete
        PageElement ddButton = rootElement.find(By.className("aui-dropdown2-trigger"));
        ddButton.click();
        String dropDownMenuId = ddButton.getAttribute("aria-owns");
        PageElement deleteLink = elementFinder.find(By.id(dropDownMenuId)).find(By.className("dvcs-control-delete-org"));
        deleteLink.click();

        ConfirmationDialog dialog = elementFinder.find(By.id("confirm-dialog"), ConfirmationDialog.class, TimeoutType.DIALOG_LOAD);
        dialog.confirm();
        dialog.waitUntilVisible();
    }

    public List<RepositoryDiv> getRepositories()
    {
        return getRepositories(false);
    }

    public List<RepositoryDiv> getRepositories(boolean filterDynamicRepositories)
    {
        List<RepositoryDiv> list = new ArrayList<RepositoryDiv>();
        if (!repositoriesTable.isPresent()) {
            return list;
        }
        
        List<PageElement> trs = repositoriesTable.findAll(By.xpath("//table/tbody/tr[contains(concat(@class, ' '), 'dvcs-repo-row')]"));
        for (PageElement tr : trs)
        {

            RepositoryDiv repositoryDiv = pageBinder.bind(RepositoryDiv.class, tr);
            if (!filterDynamicRepositories || !repositoryDiv.getRepositoryName().startsWith(DYNAMIC_REPOSITORIES_PREFIX))
            {
                list.add(pageBinder.bind(RepositoryDiv.class, tr));
            }
        }
        return list;
    }

    public List<String> getRepositoryNames()
    {

        return Lists.transform(getRepositories(true), new Function<RepositoryDiv, String>()
        {
            @Override
            public String apply(final RepositoryDiv repositoryDiv)
            {
                return repositoryDiv.getRepositoryName();
            }
        });
    }

    public boolean containsRepository(String name)
    {
        return findRepository(name) != null;
    }

    public RepositoryDiv findRepository(String name)
    {
        for (RepositoryDiv repositoryDiv : getRepositories())
        {
            if (name.equals(repositoryDiv.getRepositoryName()))
            {
                return repositoryDiv;
            }
        }
        return null;
    }

    public String getOrganizationType()
    {
        // <h4 class="aui bitbucketLogo">
        return organizationType.getAttribute("class").replaceAll(".*aui (.*)Logo.*", "$1");
    }

    public String getOrganizationName()
    {
        return organizationName.getText();
    }

    public void refresh()
    {
        controlsButton.click();
        findControlDialog().refresh();
        // wait for popup to show up
        try
        {
            waitUntilTrue(elementFinder.find(By.id("refreshing-account-dialog")).timed().isVisible());
        }
        catch (AssertionError e)
        {
            // ignore, the refresh was probably very quick and the popup has been already closed.
        }
        waitUntil(elementFinder.find(By.id("refreshing-account-dialog")).timed().isVisible(), is(false), by(30000));
    }

    private AccountsPageAccountControlsDialog findControlDialog()
    {
        String dropDownMenuId = controlsButton.getAttribute("aria-owns");
        return elementFinder.find(By.id(dropDownMenuId), AccountsPageAccountControlsDialog.class);
    }

    public void sync()
    {
        enableAllRepos();
        // click sync icon
        for(RepositoryDiv repoDiv: getRepositories())
        {
            waitUntilTrue(Conditions.and(repoDiv.getSyncIcon().timed().isEnabled(),
                    repoDiv.getSyncIcon().timed().isVisible()));
            repoDiv.getSyncIcon().click();
        }
    }

    public void enableAllRepos()
    {
        // enable repos
        for(RepositoryDiv repoDiv: getRepositories())
        {
            repoDiv.enableSync();
            dismissNotificationDialogIfExist();
        }
    }

    private void dismissNotificationDialogIfExist()
    {
        PageElement button = elementFinder.find(By.cssSelector("div.dialog-components .submit"));
        button.timed().isPresent().byDefaultTimeout();
        if (button.isPresent() && button.isVisible())
        {
            button.click();
            waitUntilFalse("dialog should be dismissed",
                    elementFinder.find(By.className("dialog-components")).timed().isVisible());
        }
    }
}
