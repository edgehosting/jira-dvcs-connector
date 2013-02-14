package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import net.java.ao.OneToMany;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;

/**
 * AO of the {@link GitHubPullRequest}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubPullRequest")
public interface GitHubPullRequestMapping extends GitHubEntityMapping
{

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
     * @return {@link GitHubPullRequest#getBaseRepository()}
     */
    @NotNull
    GitHubRepositoryMapping getBaseRepository();

    /**
     * @param baseRepository
     *            {@link #getBaseRepository()}
     */
    void setBaseRepository(GitHubRepositoryMapping baseRepository);

    /**
     * @return {@link GitHubPullRequest#getBaseSha()}
     */
    @NotNull
    String getBaseSha();

    /**
     * @param baseSha
     *            {@link #getBaseSha()}
     */
    void setBaseSha(String baseSha);

    /**
     * @return {@link GitHubPullRequest#getHeadRepository()}
     */
    @NotNull
    GitHubRepositoryMapping getHeadRepository();

    /**
     * @param headRepository
     *            {@link #getHeadRepository()}
     */
    void setHeadRepository(GitHubRepositoryMapping headRepository);

    /**
     * @return {@link GitHubPullRequest#getHeadSha()}
     */
    @NotNull
    String getHeadSha();

    /**
     * @param sha
     *            {@link #getHeadSha()}
     */
    void setHeadSha(String sha);

    /**
     * @return {@link GitHubPullRequest#getNumber()}
     */
    @NotNull
    int getPullRequestNumber();

    /**
     * @param pullRequestNumber
     *            {@link #getPullRequestNumber()}
     */
    void setPullRequestNumber(int pullRequestNumber);

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
     * @return {@link GitHubPullRequest#getText()}
     */
    String getText();

    /**
     * @param text
     *            {@link #getText()}
     */
    void setText(String text);

    /**
     * @return {@link GitHubPullRequest#getUrl()}
     */
    String getUrl();

    /**
     * @param url
     *            {@link #getUrl()}
     */
    void setUrl(String url);

    /**
     * @return {@link GitHubPullRequestMapping#getActions()}
     */
    @OneToMany
    GitHubPullRequestActionMapping[] getActions();

}
