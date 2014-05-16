package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.google.common.base.Predicate;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Represents "Issue" page.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuePage implements Page
{

    /**
     * Injected webDriver dependency.
     */
    @Inject
    private WebDriver webDriver;

    /**
     * Issue key of this page - it is used by URL generation.
     */
    private final String issueKey;

    /**
     * Reference of "Pull Request" tab link.
     */
    @ElementBy(id = "dvcs-pullrequets-tabpanel")
    private PageElement prTabLink;

    /**
     * Reference of "Progress bar" of a tab.
     */
    @ElementBy(xpath = "//div[contains(concat(' ', @class, ' '), 'issuePanelProgress')]")
    private PageElement issuePanelProgressBar;

    /**
     * @see #getIssuePagePullRequestTab()
     */
    @ElementBy(id = "issue_actions_container")
    private IssuePagePullRequestTab issuePagePullRequestTab;

    /**
     * Constructor.
     * 
     * @param issueKey
     *            of this page - it used by URL generation
     */
    public IssuePage(String issueKey)
    {
        this.issueKey = issueKey;
    }

    /**
     * Open "Pull Request" tab.
     */
    public void openPRTab()
    {
        prTabLink.click();

        // AJAX progress bar displaying takes some time
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            // nothing to do
        }

        // wait until progress bar is finished
        new WebDriverWait(webDriver, 15).until(new Predicate<WebDriver>()
        {

            @Override
            public boolean apply(@Nullable WebDriver input)
            {
                return !issuePanelProgressBar.getAttribute("class").matches(".*\\bloading\\b.*");
            }

        });
        
        // AJAX DOM update takes some time
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            // nothing to do
        }

        // refreshing the page because of https://jdog.atlassian.net/browse/JDEV-24925
        webDriver.navigate().refresh();
    }

    /**
     * @return Reference to "Pull request" tab, which is displayed after {@link #openPRTab()} fire.
     */
    public IssuePagePullRequestTab getIssuePagePullRequestTab()
    {
        return issuePagePullRequestTab;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl()
    {
        return "/browse/" + issueKey;
    }

}
