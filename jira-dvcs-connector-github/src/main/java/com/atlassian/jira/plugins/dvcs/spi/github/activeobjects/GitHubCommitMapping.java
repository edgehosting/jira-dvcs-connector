package com.atlassian.jira.plugins.dvcs.spi.github.activeobjects;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;
import net.java.ao.schema.Unique;

import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.util.NotNull;

/**
 * AO of the {@link GitHubCommit}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
@Table("GitHubCommit")
public interface GitHubCommitMapping extends Entity
{

    /**
     * @see #getSha()
     */
    String COLUMN_SHA = "SHA";

    /**
     * @see #getCreatedAt()
     */
    String COLUMN_CREATED_AT = "CREATED_AT";

    /**
     * @see #getCreatedBy()
     */
    String COLUMN_CREATED_BY = "CREATED_BY";

    /**
     * @see #getMessage()
     */
    String COLUMN_MESSAGE = "MESSAGE";

    /**
     * @return {@link GitHubCommit#getSha()}
     */
    @Unique
    @NotNull
    String getSha();

    /**
     * @param sha
     *            {@link #getSha()}
     */
    void setSha(String sha);

    /**
     * @return {@link GitHubCommit#getCreatedAt()}
     */
    @NotNull
    Date getCreatedAt();

    /**
     * @param createdAt
     *            {@link #getCreatedAt()}
     */
    void setCreatedAt(Date createdAt);

    /**
     * @return {@link GitHubCommit#getCreatedBy()}
     */
    @NotNull
    String getCreatedBy();

    /**
     * @param createdBy
     *            {@link #getCreatedBy()}
     */
    void setCreatedBy(String createdBy);

    /**
     * @return {@link GitHubCommit#getMessage()}
     */
    @StringLength(StringLength.UNLIMITED)
    String getMessage();

    /**
     * @param message
     *            {@link #getMessage()}
     */
    void setMessage(String message);

}
