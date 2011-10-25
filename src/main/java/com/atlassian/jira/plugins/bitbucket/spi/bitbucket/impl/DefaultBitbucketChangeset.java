package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;

/**
 * Details on a changeset found in Bitbucket.
 */
public class DefaultBitbucketChangeset implements Changeset
{
    static final String COMMIT_URL_PATTERN = "https://bitbucket.org/{0}/{1}/changeset/{2}";
    private final String node;
    private final String rawAuthor;
    private final String author;
    private final String timestamp;
    private final String rawNode;
    private final String branch;
    private final String message;
    private final List<String> parents;
    private final List<ChangesetFile> files;

	private final int repositoryId;

    public DefaultBitbucketChangeset( int repositoryId, 
                                     String node, String rawAuthor, String author, String timestamp,
                                     String rawNode, String branch, String message,
                                     List<String> parents, List<ChangesetFile> files)
    {
		this.repositoryId = repositoryId;
        this.node = node;
        this.rawAuthor = rawAuthor;
        this.author = author;
        this.timestamp = timestamp;
        this.rawNode = rawNode;
        this.branch = branch;
        this.message = message;
        this.parents = parents;
        this.files = files;
    }

    public int getRepositoryId()
    {
    	return repositoryId;
    }

	public String getNode()
    {
        return node;
    }

    public String getRawAuthor()
    {
        return rawAuthor;
    }

    public String getAuthor()
    {
        return author;
    }

    public String getTimestamp()
    {
        return timestamp;
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

    public String getCommitURL(SourceControlRepository repository)
    {
    	RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
        return MessageFormat.format(COMMIT_URL_PATTERN, uri.getOwner(), uri.getSlug(), node);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultBitbucketChangeset that = (DefaultBitbucketChangeset) o;

        return new EqualsBuilder()
        	.append(author, that.author)
			.append(branch, that.branch)
			.append(files, that.files)
			.append(message, that.message)
			.append(node, that.node)
			.append(parents,that.parents)
			.append(rawAuthor,that.rawAuthor)
			.append(rawNode, that.rawNode)
			.append(repositoryId, that.repositoryId)
			.append(timestamp, that.timestamp)
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
			.append(timestamp).hashCode();
    }

}
