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
@Table("GitHubCommitMapping")
public interface GitHubCommitMapping extends Entity
{

    /**
     * @see #getSha()
     */
    String KEY_SHA = "SHA";

    /**
     * @see #getDate()
     */
    String KEY_DATE = "DATE";

    /**
     * @see #getAuthor()
     */
    String KEY_AUTHOR = "AUTHOR";

    /**
     * @see #getMessage()
     */
    String KEY_MESSAGE = "MESSAGE";

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
     * @return {@link GitHubCommit#getDate()}
     */
    @NotNull
    Date getDate();

    /**
     * @param date
     *            {@link #getDate()}
     */
    void setDate(Date date);

    /**
     * @return {@link GitHubCommit#getAuthor()}
     */
    @NotNull
    String getAuthor();

    /**
     * @param author
     *            {@link #getAuthor()}
     */
    void setAuthor(String author);

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
