package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Changeset
{
    private final int repositoryId;
    private final String node;
    private final String issueKey;
    private final String rawAuthor;
    private final String author;
    private final Date date;
    private final String rawNode;
    private final String branch;
    private final String message;
    private final List<String> parents;
    private final List<ChangesetFile> files;

    private final int allFileCount;

    public Changeset(int repositoryId, String node, String issueKey, String message, Date timestamp)
    {
        this(repositoryId, node, issueKey, "", "", timestamp, "", "", message, Collections.<String>emptyList(), Collections.<ChangesetFile>emptyList(), 0);
    }


    public Changeset(int repositoryId,String node, String issueKey,
                     String rawAuthor, String author, Date date,
                     String rawNode, String branch, String message,
                     List<String> parents, List<ChangesetFile> files, int allFileCount)
    {
        this.repositoryId = repositoryId;
        this.node = node;
        this.issueKey = issueKey;
        this.rawAuthor = rawAuthor;
        this.author = author;
        this.date = date;
        this.rawNode = rawNode;
        this.branch = branch;
        this.message = message;
        this.parents = parents;
        this.files = files;
        this.allFileCount = allFileCount;
    }

    public int getRepositoryId()
    {
        return repositoryId;
    }

    public String getNode()
    {
        return node;
    }

    public String getIssueKey()
    {
        return issueKey;
    }

    public String getRawAuthor()
    {
        return rawAuthor;
    }

    public String getAuthor()
    {
        return author;
    }

    public Date getDate()
    {
        return date;
    }

    public String getRawNode()
    {
        return rawNode;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getMessage()
    {
        return message;
    }

    public List<String> getParents()
    {
        return parents;
    }

    public List<ChangesetFile> getFiles()
    {
        return files;
    }

    public int getAllFileCount()
    {
        return allFileCount;
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
                .append(issueKey, that.issueKey)
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
                .append(issueKey)
                .append(parents)
                .append(rawAuthor)
                .append(rawNode)
                .append(repositoryId)
                .append(date)
                .append(allFileCount)
                .hashCode();
    }

}
