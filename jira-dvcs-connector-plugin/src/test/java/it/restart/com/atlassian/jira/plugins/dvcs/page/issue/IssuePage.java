package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

/**
 * Represents "Issue" page.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuePage implements Page
{

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
     * {@inheritDoc}
     */
    @Override
    public String getUrl()
    {
        return "/browse/" + issueKey;
    }

}
