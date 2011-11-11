package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.util.concurrent.LazyReference;

import java.util.Date;
import java.util.List;

/**
 * A lazy loaded remote bitbucket changeset.  Will only load the changeset details if the
 * details that aren't stored locally are required.
 */
public class LazyLoadedBitbucketChangeset implements Changeset {

    private final LazyReference<Changeset> lazyReference;
    private final String nodeId;
    private final SourceControlRepository repository;

    public LazyLoadedBitbucketChangeset(final Communicator bitbucket, final SourceControlRepository repository, final String nodeId) {
        this.repository = repository;
        this.lazyReference = new LazyReference<Changeset>() {
            @Override
            protected Changeset create() throws Exception {
                return bitbucket.getChangeset(repository, nodeId);
            }
        };
        this.nodeId = nodeId;
    }

    private Changeset getBitbucketChangeset() {
        return lazyReference.get();
    }

    public int getRepositoryId() {
        return repository.getId();
    }

    public String getNode() {
        return nodeId;
    }

    public String getRawAuthor() {
        return getBitbucketChangeset().getRawAuthor();
    }

    public String getAuthor() {
        return getBitbucketChangeset().getAuthor();
    }

    public Date getTimestamp() {
        return getBitbucketChangeset().getTimestamp();
    }

    public String getRawNode() {
        return getBitbucketChangeset().getRawNode();
    }

    public String getBranch() {
        return getBitbucketChangeset().getBranch();
    }

    public String getMessage() {
        return getBitbucketChangeset().getMessage();
    }

    public List<String> getParents() {
        return getBitbucketChangeset().getParents();
    }

    public List<ChangesetFile> getFiles() {
        return getBitbucketChangeset().getFiles();
    }

    public String getCommitURL(SourceControlRepository repository) {
        return repository.getRepositoryUri().getCommitUrl(nodeId);
    }
}
