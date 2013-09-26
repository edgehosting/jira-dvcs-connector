package com.atlassian.jira.plugins.dvcs.model.dev;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType (XmlAccessType.FIELD)
public class RestChangeset
{
    private String id;
    private String displayId;
    private RestAuthor author;
    private long authorTimestamp;
    private String message;
    private List<RestChangeset> parents;
    private String branch;
    private int fileCount;
    private String url;
    private boolean isMerge;

    public RestChangeset()
    {
    }

    public String getId()
    {
        return id;
    }

    public void setId(final String id)
    {
        this.id = id;
    }

    public String getDisplayId()
    {
        return displayId;
    }

    public void setDisplayId(final String displayId)
    {
        this.displayId = displayId;
    }

    public RestAuthor getAuthor()
    {
        return author;
    }

    public void setAuthor(final RestAuthor author)
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

    public String getMessage()
    {
        return message;
    }

    public void setMessage(final String message)
    {
        this.message = message;
    }

    public List<RestChangeset> getParents()
    {
        return parents;
    }

    public void setParents(final List<RestChangeset> parents)
    {
        this.parents = parents;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(final String branch)
    {
        this.branch = branch;
    }

    public int getFileCount()
    {
        return fileCount;
    }

    public void setFileCount(final int fileCount)
    {
        this.fileCount = fileCount;
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
        return isMerge;
    }

    public void setMerge(final boolean merge)
    {
        isMerge = merge;
    }
}
