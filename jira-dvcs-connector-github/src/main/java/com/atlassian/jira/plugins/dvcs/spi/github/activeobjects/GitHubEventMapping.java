package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;

/**
 * AO representation of the {@link GitHubEvent}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubEvent")
public interface GitHubEventMapping extends Entity
{

    /**
     * @see #getGitHubId()
     */
    String COLUMN_GIT_HUB_ID = "GIT_HUB_ID";

    /**
     * @see #getCreatedAt()
     */
    String COLUMN_CREATED_AT = "CREATED_AT";

    /**
     * @see #isSavePoint()
     */
    String COLUMN_SAVE_POINT = "SAVE_POINT";

    /**
     * @return {@link GitHubEvent#getGitHubId()}
     */
    @NotNull
    @Unique
    String getGitHubId();

    /**
     * @param gitHubId
     */
    void setGitHubId(String gitHubId);

    /**
     * @return {@link GitHubEvent#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubEvent#isSavePoint()}
     */
    @NotNull
    boolean isSavePoint();

    /**
     * @param savePoint
     *            {@link #isSavePoint()}
     */
    void setSavePoint(boolean savePoint);

}
