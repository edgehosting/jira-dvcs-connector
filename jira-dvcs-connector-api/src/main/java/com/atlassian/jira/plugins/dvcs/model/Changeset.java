package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Changeset
{
    public static final int MAX_VISIBLE_FILES = 5;

    private int id;
    private int repositoryId;
    private String node;
    private String rawAuthor;
    private String author;
    private Date date;
    private String rawNode;
    private String branch;
    private String message;
    private List<String> parents;
    private String authorEmail;
    private Boolean smartcommitAvaliable;

    private List<ChangesetFile> files;
    private int allFileCount;

    private Integer version;

    public Changeset(int repositoryId, String node, String message, Date timestamp)
    {
        this(repositoryId, node, "", "", timestamp, "", "", message, Collections.<String>emptyList(), Collections.<ChangesetFile>emptyList(), 0, "");
    }


    public Changeset(int repositoryId,String node,
                     String rawAuthor, String author, Date date,
                     String rawNode, String branch, String message,
                     List<String> parents, List<ChangesetFile> files, int allFileCount, String authorEmail)
    {
        this.repositoryId = repositoryId;
        this.node = node;
        this.rawAuthor = rawAuthor;
        this.author = author;
        this.date = date;
        this.rawNode = rawNode;
        this.branch = branch;
        this.message = message;
        this.parents = parents;
        this.files = files;
        this.allFileCount = allFileCount;
        this.authorEmail = authorEmail;
    }

    public int getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(int repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public String getNode()
    {
        return node;
    }

    public void setNode(String node)
    {
        this.node = node;
    }


    public String getRawAuthor()
    {
        return rawAuthor;
    }

    public void setRawAuthor(String rawAuthor)
    {
        this.rawAuthor = rawAuthor;
    }

    public String getAuthor()
    {
        return author;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public String getRawNode()
    {
        return rawNode;
    }

    public void setRawNode(String rawNode)
    {
        this.rawNode = rawNode;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public List<String> getParents()
    {
        return parents;
    }

    public void setParents(List<String> parents)
    {
        this.parents = parents;
    }

    public List<ChangesetFile> getFiles()
    {
        return files;
    }

    public void setFiles(List<ChangesetFile> files)
    {
        this.files = files;
    }

    public int getAllFileCount()
    {
        return allFileCount;
    }

    public void setAllFileCount(int allFileCount)
    {
        this.allFileCount = allFileCount;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Changeset that = (Changeset) o;

        return new EqualsBuilder()
                .append(author, that.author)
                .append(branch, that.branch)
                .append(files, that.files)
                .append(message, that.message)
                .append(node, that.node)
                .append(parents, that.parents)
                .append(rawAuthor, that.rawAuthor)
                .append(rawNode, that.rawNode)
                .append(repositoryId, that.repositoryId)
                .append(date, that.date)
                .append(allFileCount, that.allFileCount)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
                .append(author)
                .append(branch)
                .append(files)
                .append(message)
                .append(node)
                .append(parents)
                .append(rawAuthor)
                .append(rawNode)
                .append(repositoryId)
                .append(date)
                .append(allFileCount)
                .hashCode();
    }


	public String getAuthorEmail()
	{
		return authorEmail;
	}


	public void setAuthorEmail(String authorEmail)
	{
		this.authorEmail = authorEmail;
	}


	public Boolean isSmartcommitAvaliable()
	{
		return smartcommitAvaliable;
	}


	public void setSmartcommitAvaliable(Boolean smartcommitAvaliable)
	{
		this.smartcommitAvaliable = smartcommitAvaliable;
	}


	public int getId()
	{
		return id;
	}


	public void setId(int id)
	{
		this.id = id;
	}

}
