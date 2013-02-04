package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.util.NotNull;

/**
 * AO of the {@link GitHubPullRequest}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubPullRequest")
public interface GitHubPullRequestMapping extends Entity
{

    /**
     * @see #getSynchronizedAt()
     */
    String COLUMN_GIT_SYNCHRONIZED_AT = "SYNCHRONIZED_AT";

    /**
     * @see #getGitHubId()
     */
    String COLUMN_GIT_HUB_ID = "GIT_HUB_ID";

    /**
     * @see #getTitle()
     */
    String COLUMN_TITLE = "TITLE";

    /**
     * @see #getActions()
     */
    String COLUMN_ACTIONS = "GIT_HUB";

    /**
     * @return {@link GitHubPullRequest#getSynchronizedAt()}
     */
    @NotNull
    Date getSynchronizedAt();

    /**
     * @param synchronizedAt
     *            {@link #getSynchronizedAt()}
     */
    void setSynchronizedAt(Date synchronizedAt);

    /**
     * @return {@link GitHubPullRequest#getGitHubId()}
     */
    @Unique
    @NotNull
    long getGitHubId();

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    void setGitHubId(long gitHubId);

    /**
     * @return {@link GitHubPullRequest#getTitle()}
     */
    String getTitle();

    /**
     * @param title
     *            {@link #getTitle()}
     */
    void setTitle(String title);

    /**
     * @return {@link GitHubPullRequestMapping#getActions()}
     */
    @OneToMany
    GitHubPullRequestActionMapping[] getActions();

}
