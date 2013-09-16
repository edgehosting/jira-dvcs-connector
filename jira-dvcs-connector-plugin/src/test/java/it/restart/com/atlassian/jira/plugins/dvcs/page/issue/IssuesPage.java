package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Predicate;

/**
 * Represents IssuesPage - provides way to filter issues.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuesPage implements Page
{

    /**
     * Injected {@link WebDriver} dependency.
     */
    @Inject
    private WebDriver webDriver;

    /**
     * Reference to "More criteria" dialog.
     */
    @ElementBy(xpath = "//li[contains(concat(' ', @class, ' '), 'criteria-actions')]/button[contains(concat(' ', @class, ' '), 'add-criteria')]")
    private PageElement moreCriteriaButton;

    @ElementBy(xpath = "//div[@class='ajs-layer box-shadow active']")
    private IssuesPageMoreCriteriaDialog moreCriteriaDialog;

    /**
     * Reference to "Summary criteria" button.
     */
    @ElementBy(xpath = "//ul[contains(concat(' ', @class, ' '), 'criteria-list')]/li[@data-id='summary']/button")
    private PageElement summaryCriteriaButton;

    /**
     * Reference to "Summary criteria" dialog.
     */
    @ElementBy(xpath = "//div[@class='ajs-layer box-shadow active']")
    private IssuesPageSummaryCriteriaDialog summaryCriteriaDialog;

    /**
     * Issues rows of table.
     */
    @ElementBy(pageElementClass = IssuesPageIssueRow.class, xpath = "//table[@id='issuetable']/tbody/tr")
    private Iterable<IssuesPageIssueRow> issueRows;

    /**
     * Reference to issue table
     */
    @ElementBy(xpath = "//table[@id='issuetable']")
    private PageElement issueTable;

    /**
     * Reference to confirmation delete dialog.
     */
    @ElementBy(id = "delete-issue-dialog")
    private IssuesPageIssueDeleteConfirmationDialog deleteConfirmationDialog;

    /**
     * Reference to layout switcher button
     */
    @ElementBy(id = "layout-switcher-button")
    private PageElement layoutSwitcherButton;

    /**
     * Reference to list view menu item
     */
    @ElementBy(xpath = "//div[@id='layout-switcher-button_drop']//a[@data-layout-key='list-view']")
    private PageElement listViewButton;

    /**
     * Fills search form by provided values.
     * 
     * @param projectKey
     *            key of project
     * @param summary
     *            issue summary
     */
    public void fillSearchForm(String projectKey, String summary)
    {
        new WebDriverWait(webDriver, 15).until(new Predicate<WebDriver>()
        {

            @Override
            public boolean apply(@Nullable WebDriver input)
            {
                return moreCriteriaButton.isVisible();
            }

        });
        
        moreCriteriaButton.click();
        moreCriteriaDialog.add("summary", "Summary");

        summaryCriteriaButton.click();
        summaryCriteriaDialog.fill(summary);
        summaryCriteriaDialog.update();
    }

    /**
     * @return returns count of issue rows
     */
    public List<IssuesPageIssueRow> getIssueRows()
    {
        List<IssuesPageIssueRow> result = new ArrayList<IssuesPageIssueRow>();

        Iterator<IssuesPageIssueRow> issueRowsIterator = issueRows.iterator();
        while (issueRowsIterator.hasNext())
        {
            result.add(issueRowsIterator.next());
        }

        return result;
    }

    /**
     * Deletes all issues, which are currently filtered on page.
     */
    public void deleteAll()
    {
        switchLayout();
        Iterator<IssuesPageIssueRow> issueRowsIterator = issueRows.iterator();
        while (issueRowsIterator.hasNext())
        {
            IssuesPageIssueRow nextIssueRow = issueRowsIterator.next();
            nextIssueRow.delete();
            deleteConfirmationDialog.confirm();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl()
    {
        return "/issues";
    }

    public void switchLayout()
    {
        if (layoutSwitcherButton.isPresent())
        {
            layoutSwitcherButton.click();
            Poller.waitUntilTrue(listViewButton.timed().isVisible());
            listViewButton.click();
            try
            {
                Poller.waitUntilTrue(issueTable.timed().isVisible());
            } catch (AssertionError e)
            {
                // no issue table, no results found
            }
        }
    }
}
