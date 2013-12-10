package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

/**
 * AO representation of the {@link GitHubEvent}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GIT_HUB_EVENT")
public interface GitHubEventMapping extends Entity
{

    /**
     * @see #getRepository()
     */
    public static String REPOSITORY = "REPOSITORY_ID";

    /**
     * @see #getGitHubId()
     */
    public static String GIT_HUB_ID = "GIT_HUB_ID";

    /**
     * @see #getCreatedAt()
     */
    public static String CREATED_AT = "CREATED_AT";

    /**
     * @see #isSavePoint()
     */
    public static String SAVE_POINT = "SAVE_POINT";

    /**
     * @return {@link GitHubEvent#getRepository()}
     */
    @NotNull
    RepositoryMapping getRepository();

    /**
     * @return remote id of GithubEvent
     */
    @NotNull
    String getGitHubId();

    /**
     * @param gitHubId
     *            {@link #getGitHubId()}
     */
    void setGitHubId(String gitHubId);

    /**
     * @return date of event creation
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return true if this event can be considered to be a save point - it means all previous records was already proceed
     */
    boolean isSavePoint();

    /**
     * @param savePoint
     *            {@link #isSavePoint()}
     */
    void setSavePoint(boolean savePoint);

}
