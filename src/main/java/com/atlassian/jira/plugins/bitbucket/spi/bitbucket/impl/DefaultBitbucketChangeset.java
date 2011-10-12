package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.text.MessageFormat;
import java.util.List;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
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
    private final String revision;
    private final List<String> parents;
    private final List<ChangesetFile> files;

	private final String repositoryUrl;

    public DefaultBitbucketChangeset(String repositoryUrl, 
                                     String node, String rawAuthor, String author, String timestamp,
                                     String rawNode, String branch, String message, String revision,
                                     List<String> parents, List<ChangesetFile> files)
    {
		this.repositoryUrl = repositoryUrl;
        this.node = node;
        this.rawAuthor = rawAuthor;
        this.author = author;
        this.timestamp = timestamp;
        this.rawNode = rawNode;
        this.branch = branch;
        this.message = message;
        this.revision = revision;
        this.parents = parents;
        this.files = files;
    }

    public String getRepositoryUrl()
    {
    	return repositoryUrl;
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

    public String getRevision()
    {
        return revision;
    }

    public String getCommitURL()
    {
    	RepositoryUri uri = RepositoryUri.parse(repositoryUrl);
        return MessageFormat.format(COMMIT_URL_PATTERN, uri.getOwner(), uri.getSlug(), node);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultBitbucketChangeset that = (DefaultBitbucketChangeset) o;

        if (revision != that.revision) return false;
        if (!author.equals(that.author)) return false;
        if (!branch.equals(that.branch)) return false;
        if (!files.equals(that.files)) return false;
        if (!message.equals(that.message)) return false;
        if (!node.equals(that.node)) return false;
        if (!parents.equals(that.parents)) return false;
        if (!rawAuthor.equals(that.rawAuthor)) return false;
        if (!rawNode.equals(that.rawNode)) return false;
        if (!repositoryUrl.equals(that.repositoryUrl)) return false;
        if (!timestamp.equals(that.timestamp)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = repositoryUrl.hashCode();
        result = 31 * result + node.hashCode();
        result = 31 * result + rawAuthor.hashCode();
        result = 31 * result + author.hashCode();
        result = 31 * result + timestamp.hashCode();
        result = 31 * result + rawNode.hashCode();
        result = 31 * result + branch.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + revision.hashCode();
        result = 31 * result + parents.hashCode();
        result = 31 * result + files.hashCode();
        return result;
    }

}
