package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import net.java.ao.Entity;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.util.NotNull;

/**
 * AO of the {@link GitHubPullRequest}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
@Table("GitHubPullRequestMapping")
public interface GitHubPullRequestMapping extends Entity
{

    /**
     * AO map key of the {@link #getGitHubId()}.
     */
    String KEY_GIT_HUB_ID = "GIT_HUB_ID";

    /**
     * AO map key of the {@link #getTitle()}.
     */
    String KEY_TITLE = "TITLE";

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

}
