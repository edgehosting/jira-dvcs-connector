package com.atlassian.jira.plugins.dvcs.model.dev;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestChangeset
{
    private RestUser author;
    @XmlElement (name = "authorTimestamp")
    private long authorTimestamp;
    @XmlElement(name = "displayId")
    private String displayId;
    private String id;
    @XmlElement(name = "fileCount")
    private int fileCount;
    private String message;
    private String url;
    private boolean merge;

    public RestChangeset()
    {
    }

    public RestUser getAuthor()
    {
        return author;
    }

    public void setAuthor(final RestUser author)
    {
        this.author = author;
    }

    public long getAuthorTimestamp()
    {
        return authorTimestamp;
    }

    public void setAuthorTimestamp(final long authorTimestamp)
    {
        this.authorTimestamp = authorTimestamp;
    }

    public String getDisplayId()
    {
        return displayId;
    }

    public void setDisplayId(final String displayId)
    {
        this.displayId = displayId;
    }

    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    public int getFileCount()
    {
        return fileCount;
    }

    public void setFileCount(final int fileCount)
    {
        this.fileCount = fileCount;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(final String message)
    {
        this.message = message;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(final String url)
    {
        this.url = url;
    }

    public boolean isMerge()
    {
        return merge;
    }

    public void setMerge(final boolean merge)
    {
        this.merge = merge;
    }
}
