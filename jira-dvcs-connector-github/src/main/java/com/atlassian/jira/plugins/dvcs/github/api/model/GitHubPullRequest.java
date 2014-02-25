package com.atlassian.jira.plugins.dvcs.github.api.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * GitHub pull request.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@XmlRootElement
@XmlType(propOrder = { "number", "state", "title", "createdAt", "user", "assignee", "updatedAt", "mergedAt", "mergedBy", "base", "head",
        "comments", "reviewComments", "htmlUrl" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequest
{

    /**
     * @see #getNumber()
     */
    private int number;

    /**
     * @see #getState()
     */
    private String state;

    /**
     * @see #getTitle()
     */
    private String title;

    /**
     * @see #getCreatedAt()
     */
    @XmlElement(name = "created_at")
    private Date createdAt;

    /**
     * @see #getUser()
     */
    private GitHubUser user;

    /**
     * @see #getAssignee()
     */
    private GitHubUser assignee;

    /**
     * @see #getUpdatedAt()
     */
    @XmlElement(name = "updated_at")
    private Date updatedAt;

    /**
     * @see #getMergedAt()
     */
    @XmlElement(name = "merged_at")
    private Date mergedAt;

    /**
     * @see #getMergedBy()
     */
    @XmlElement(name = "merged_by")
    public GitHubUser mergedBy;

    /**
     * @see #getBase()
     */
    private GitHubPullRequestMarker base;

    /**
     * @see #getHead()
     */
    private GitHubPullRequestMarker head;

    /**
     * @see #getComments()
     */
    private int comments;

    /**
     * @see #getReviewComments()
     */
    @XmlElement(name = "review_comments")
    private int reviewComments;

    /**
     * @see #getHtmlUrl()
     */
    @XmlElement(name = "html_url")
    private String htmlUrl;

    /**
     * Constructor.
     */
    public GitHubPullRequest()
    {
    }

    /**
     * @return Number of pull request.
     */
    public int getNumber()
    {
        return number;
    }

    /**
     * @param number
     *            {@link #getNumber()}
     */
    public void setNumber(int number)
    {
        this.number = number;
    }

    /**
     * @return State of this Pull Request. Either open or closed.
     */
    public String getState()
    {
        return state;
    }

    /**
     * @param state
     *            {@link #getState()}
     */
    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * @return Title of pull request.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title
     *            {@link #getTitle()}
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @return Date when this pull request was created.
     */
    public Date getCreatedAt()
    {
        return createdAt;
    }

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    public void setCreatedAt(Date createdAt)
    {
        this.createdAt = createdAt;
    }

    /**
     * @return Author of this pull request.
     */
    public GitHubUser getUser()
    {
        return user;
    }

    /**
     * @param user
     *            {@link #getUser()}
     */
    public void setUser(GitHubUser user)
    {
        this.user = user;
    }

    /**
     * @return Current user to who is assigned this pull request.
     */
    public GitHubUser getAssignee()
    {
        return assignee;
    }

    /**
     * @param assignee
     *            {@link #getAssignee()}
     */
    public void setAssignee(GitHubUser assignee)
    {
        this.assignee = assignee;
    }

    /**
     * @return Date when this pull request was updated last time.
     */
    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    /**
     * @param updatedAt
     *            {@link #getUpdatedAt()}
     */
    public void setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    /**
     * @return Date when this pull request was merged - or null if it was closed without merge (declined).
     */
    public Date getMergedAt()
    {
        return mergedAt;
    }

    /**
     * @param mergedAt
     *            {@link #getMergedAt()}
     */
    public void setMergedAt(Date mergedAt)
    {
        this.mergedAt = mergedAt;
    }

    /**
     * @return User, who merged this pull request.
     */
    public GitHubUser getMergedBy()
    {
        return mergedBy;
    }

    /**
     * @param mergedBy
     *            {@link #getMergedBy()}
     */
    public void setMergedBy(GitHubUser mergedBy)
    {
        this.mergedBy = mergedBy;
    }

    /**
     * @return The name of the branch you want your changes pulled into.
     */
    public GitHubPullRequestMarker getBase()
    {
        return base;
    }

    /**
     * @param base
     *            {@link #getBase()}
     */
    public void setBase(GitHubPullRequestMarker base)
    {
        this.base = base;
    }

    /**
     * @return The name of the branch where your changes are implemented.
     */
    public GitHubPullRequestMarker getHead()
    {
        return head;
    }

    /**
     * @param head
     *            {@link #getHead()}
     */
    public void setHead(GitHubPullRequestMarker head)
    {
        this.head = head;
    }

    /**
     * @return The API location of this Pull Request’s comments.
     */
    public int getComments()
    {
        return comments;
    }

    /**
     * @param comments
     *            {@link #getComments()}
     */
    public void setComments(int comments)
    {
        this.comments = comments;
    }

    /**
     * @return The API location of this Pull Request’s review comments.
     */
    public int getReviewComments()
    {
        return reviewComments;
    }

    /**
     * @param reviewComments
     *            {@link #getReviewComments()}
     */
    public void setReviewComments(int reviewComments)
    {
        this.reviewComments = reviewComments;
    }

    /**
     * @return URL to HTML (GitHub web site) version of this pull request.
     */
    public String getHtmlUrl()
    {
        return htmlUrl;
    }

    /**
     * @param htmlUrl
     *            {@link #getHtmlUrl()}
     */
    public void setHtmlUrl(String htmlUrl)
    {
        this.htmlUrl = htmlUrl;
    }

}
