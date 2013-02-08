package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.schema.Table;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;

/**
 * AO of the {@link GitHubPush}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubPush")
public interface GitHubPushMapping extends Entity
{

    /**
     * AO map key of the {@link #getCreatedAt()}.
     */
    String COLUMN_CREATED_AT = "CREATED_AT";

    /**
     * AO map key of the {@link #getRef()}.
     */
    String COLUMN_REF = "REF";

    /**
     * AO map key of the {@link #getBefore()}.
     */
    String COLUMN_BEFORE = "BEFORE";

    /**
     * AO map key of the {@link #getHead()}.
     */
    String COLUMN_HEAD = "HEAD";

    /**
     * AO map key of the {@link #getCommits()}.
     */
    String COLUMN_COMMITS = "COMMITS";

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
    @OneToMany
    GitHubCommitMapping[] getCommits();

}
