package com.atlassian.jira.plugins.dvcs.model.dev;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Pull request commit
 *
 */
@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestPrCommit
{
    @SerializedName("rawAuthor")
    private String rawAuthor;
    private String author;
    private String node;
    private String message;
    private Date date;
    @SerializedName("authorAvatarUrl")
    private String authorAvatarUrl;

    public String getRawAuthor()
    {
        return rawAuthor;
    }

    public void setRawAuthor(final String rawAuthor)
    {
        this.rawAuthor = rawAuthor;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(final String author)
    {
        this.author = author;
    }

    public String getNode()
    {
        return node;
    }

    public void setNode(final String node)
    {
        this.node = node;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(final String message)
    {
        this.message = message;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(final Date date)
    {
        this.date = date;
    }

    public String getAuthorAvatarUrl()
    {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(final String authorAvatarUrl)
    {
        this.authorAvatarUrl = authorAvatarUrl;
    }
}
