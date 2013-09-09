package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Represents commit row of {@link IssuePagePullRequestTabActivityUpdate}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuePagePullRequestTabActivityUpdateCommit extends WebDriverElement
{

    /**
     * Reference to "Author" link of commit.
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), 'author')]//a")
    private PageElement authorLink;

    /**
     * Reference to "Author name" of commit.
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), 'author')]//a/following-sibling::span")
    private PageElement authorSpan;

    /**
     * Reference to "Commit" link.
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), 'hash')]//a")
    private PageElement commitLink;

    /**
     * Reference to "Message" of commit.
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), 'text')]//div")
    private PageElement messageDiv;

    /**
     * Reference to "Date" of commit.
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), 'date')]//div/time")
    private PageElement dateTime;

    /**
     * Constructor.
     * 
     * @param parent
     * @param timeoutType
     */
    public IssuePagePullRequestTabActivityUpdateCommit(WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(parent, timeoutType);
    }

    /**
     * @return Author name of {@link #getCommitNode()}.
     */
    public String getAuthorName()
    {
        return authorSpan.getText();
    }

    /**
     * @return Author URL of {@link #getCommitNode()}.
     */
    public String getAuthorUrl()
    {
        return authorLink.getAttribute("href");
    }

    /**
     * @return Node/commit id/sha of commit.
     */
    public String getCommitNode()
    {
        return commitLink.getText();
    }

    /**
     * @return URL of commit.
     */
    public String getCommitUrl()
    {
        return commitLink.getAttribute("href");
    }

    /**
     * @return Appropriate commit message.
     */
    public String getMessage()
    {
        return messageDiv.getText();
    }

    /**
     * @return Date when commit was appeared.
     */
    public String getDate()
    {
        return dateTime.getText();
    }

}
