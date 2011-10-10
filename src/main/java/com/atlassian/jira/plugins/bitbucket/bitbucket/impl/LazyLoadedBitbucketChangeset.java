package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.Authentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFile;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.util.concurrent.LazyReference;

import java.text.MessageFormat;
import java.util.List;

/**
 * A lazy loaded remote bitbucket changeset.  Will only load the changeset details if the
 * details that aren't stored locally are required.
 */
public class LazyLoadedBitbucketChangeset implements Changeset
{

    private final LazyReference<Changeset> lazyReference;
    private final String nodeId;
	private final String repositoryUrl;

    public LazyLoadedBitbucketChangeset(final Bitbucket bitbucket, final String repositoryUrl, 
    		 final Authentication auth, final String nodeId)
    {
        this.repositoryUrl = repositoryUrl;
		this.lazyReference = new LazyReference<Changeset>()
        {
            protected Changeset create() throws Exception
            {
                return bitbucket.getChangeset(repositoryUrl, auth, nodeId);
            }
        };
        this.nodeId = nodeId;
    }

    private Changeset getBitbucketChangeset()
    {
        return lazyReference.get();
    }

    public String getNode()
    {
        return nodeId;
    }

    public String getRawAuthor()
    {
        return getBitbucketChangeset().getRawAuthor();
    }

    public String getAuthor()
    {
        return getBitbucketChangeset().getAuthor();
    }

    public String getTimestamp()
    {
        return getBitbucketChangeset().getTimestamp();
    }

    public String getRawNode()
    {
        return getBitbucketChangeset().getRawNode();
    }

    public String getBranch()
    {
        return getBitbucketChangeset().getBranch();
    }

    public String getMessage()
    {
        return getBitbucketChangeset().getMessage();
    }

    public List<String> getParents()
    {
        return getBitbucketChangeset().getParents();
    }

    public List<BitbucketChangesetFile> getFiles()
    {
        return getBitbucketChangeset().getFiles();
    }

    public String getRevision()
    {
        return getBitbucketChangeset().getRevision();
    }
    
	public String getRepositoryUrl()
	{
		return repositoryUrl;
	}

    public String getCommitURL()
    {
    	RepositoryUri uri = RepositoryUri.parse(repositoryUrl);
        return MessageFormat.format(DefaultBitbucketChangeset.COMMIT_URL_PATTERN, uri.getOwner(), uri.getSlug(), nodeId);
    }
}
