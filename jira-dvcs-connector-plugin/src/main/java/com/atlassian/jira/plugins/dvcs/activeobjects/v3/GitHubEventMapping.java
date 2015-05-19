package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import net.java.ao.Entity;
import net.java.ao.schema.Default;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Table;

import java.util.Date;

/**
 * AO representation of the {@link GitHubEventMapping}.
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
     * @return {@link GitHubEventMapping#getRepository()}
     */
    @NotNull
    @Indexed
    RepositoryMapping getRepository();

    /**
     * @return remote id of GithubEvent
     */
    @Indexed
    @NotNull
    @Default ("0") // this has no effect other than to work around FUSE-705/AO-490
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
