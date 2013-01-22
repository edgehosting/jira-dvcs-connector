package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;

/**
 * AO of the {@link GitHubPush}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public interface GitHubPushMapping extends Entity
{

    /**
     * AO map key of the {@link #getCreatedAt()}.
     */
    String KEY_CREATED_AT = "CREATED_AT";

    /**
     * AO map key of the {@link #getRef()}.
     */
    String KEY_REF = "REF";

    /**
     * AO map key of the {@link #getBefore()}.
     */
    String KEY_BEFORE = "BEFORE";

    /**
     * AO map key of the {@link #getHead()}.
     */
    String KEY_HEAD = "HEAD";

    /**
     * AO map key of the {@link #getCommits()}.
     */
    String KEY_COMMITS = "COMMITS";

    /**
     * @return {@link GitHubPushMapping#getCreatedAt()}
     */
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * 
     * @return {@link GitHubPushMapping#getRef()}
     */
    String getRef();

    /**
     * @param ref
     *            {@link #getRef()}
     */
    void setRef(String ref);

    /**
     * 
     * @return {@link GitHubPushMapping#getBefore()}
     */
    String getBefore();

    /**
     * @param before
     *            {@link #getBefore()}
     */
    void setBefore(String before);

    /**
     * 
     * @return {@link GitHubPushMapping#getHead()}
     */
    String getHead();

    /**
     * @param head
     *            {@link #getHead()}
     */
    void setHead(String head);

    /**
     * @return {@link GitHubPush#getCommits()}
     */
    GitHubCommitMapping[] getCommits();

    /**
     * @param commits
     *            {@link #getCommits()}
     */
    void setCommits(GitHubCommitMapping[] commits);

}
