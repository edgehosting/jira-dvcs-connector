package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFile;
import com.atlassian.util.concurrent.LazyReference;

import java.text.MessageFormat;
import java.util.List;

/**
 * A lazy loaded remote bitbucket changeset.  Will only load the changeset details if the
 * details that aren't stored locally are required.
 */
public class LazyLoadedBitbucketChangeset implements BitbucketChangeset
{

    private final LazyReference<BitbucketChangeset> lazyReference;
    private final String owner;
    private final String slug;
    private final String nodeId;

    public LazyLoadedBitbucketChangeset(final Bitbucket bitbucket, final BitbucketAuthentication auth,
                                        final String owner, final String slug, final String nodeId)
    {
        this.lazyReference = new LazyReference<BitbucketChangeset>()
        {
            protected BitbucketChangeset create() throws Exception
            {
                return bitbucket.getChangeset(auth, owner, slug, nodeId);
            }
        };
        this.owner = owner;
        this.slug = slug;
        this.nodeId = nodeId;
    }

    private BitbucketChangeset getBitbucketChangeset()
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

    public int getRevision()
    {
        return getBitbucketChangeset().getRevision();
    }

    public String getRepositoryOwner()
    {
        return owner;
    }

    public String getRepositorySlug()
    {
        return slug;
    }

    public String getCommitURL()
    {
        return MessageFormat.format(DefaultBitbucketChangeset.COMMIT_URL_PATTERN, owner, slug, nodeId);
    }
}
