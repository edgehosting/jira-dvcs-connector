package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents "Update" activity of {@link IssuePagePullRequestTab#getActivities()}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuePagePullRequestTabActivityUpdate extends IssuePagePullRequestTabActivity
{

    /**
     * Reference to "Author" link;
     */
    @ElementBy(xpath = "article/header//a[2]")
    private PageElement authorNameLink;

    /**
     * Reference to "Pull Request" link.
     */
    @ElementBy(xpath = "article/header//a[3]")
    private PageElement pullRequestLink;

    /**
     * Reference to span with "Pull Request State" information.
     */
    @ElementBy(xpath = "article/header//*[contains(concat(' ', @class, ' '), 'pull-request-state')]")
    private PageElement pullRequestStateSpan;

    /**
     * Reference to "Pull Request Commits" rows.
     */
    @ElementBy(xpath = "article//table[contains(concat(' ', @class , ' '), 'commit-list')]/tbody/tr", pageElementClass = IssuePagePullRequestTabActivityUpdateCommit.class)
    private Iterable<IssuePagePullRequestTabActivityUpdateCommit> pullRequestCommitRows;

    /**
     * Constructor.
     * 
     * @param parent
     * @param timeoutType
     */
    public IssuePagePullRequestTabActivityUpdate(WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(parent, timeoutType);
    }

    /**
     * @return Returns URL link of author name.
     */
    public String getAuthorName()
    {
        return authorNameLink.getText();
    }

    /**
     * @return Returns URL link of author profile.
     */
    public String getAuthorUrl()
    {
        return authorNameLink.getAttribute("href");
    }

    /**
     * @return Returns "Pull Request State" information.
     */
    public String getPullRequestState()
    {
        return pullRequestStateSpan.getText();
    }

    /**
     * @return Returns "Pull Request Name".
     */
    public String getPullRequestName()
    {
        return pullRequestLink.getText();
    }

    /**
     * @return Returns "Pull Request URL".
     */
    public String getPullRequestUrl()
    {
        return pullRequestLink.getAttribute("href");
    }

    /**
     * @return Returns collection of founded commits.
     */
    public List<IssuePagePullRequestTabActivityUpdateCommit> getPullRequestCommits()
    {
        List<IssuePagePullRequestTabActivityUpdateCommit> result = new LinkedList<IssuePagePullRequestTabActivityUpdateCommit>();

        Iterator<IssuePagePullRequestTabActivityUpdateCommit> iterator = pullRequestCommitRows.iterator();
        while (iterator.hasNext())
        {
            result.add(iterator.next());
        }

        return result;
    }

}
