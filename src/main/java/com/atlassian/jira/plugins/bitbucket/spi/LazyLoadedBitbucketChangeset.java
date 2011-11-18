package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.util.concurrent.LazyReference;

import java.util.Date;
import java.util.List;

/**
 * A lazy loaded remote changeset.  Will only load the changeset details if the
 * details that aren't stored locally are required.
 */
public class LazyLoadedBitbucketChangeset implements Changeset {

    private final LazyReference<Changeset> lazyReference;
    private final String nodeId;
    private final SourceControlRepository repository;

    public LazyLoadedBitbucketChangeset(final Communicator communicator, final SourceControlRepository repository, final String nodeId)
    {
		this.repository = repository;
		this.lazyReference = new LazyReference<Changeset>()
        {
            @Override
			protected Changeset create() throws Exception
            {
                return communicator.getChangeset(repository, nodeId);
            }
        };
        this.nodeId = nodeId;
    }

    private Changeset getChangesetDelegate() {
        return lazyReference.get();
    }

    public int getRepositoryId() {
        return repository.getId();
    }

    public String getNode() {
        return nodeId;
    }

    public String getRawAuthor() {
        return getChangesetDelegate().getRawAuthor();
    }

    public String getAuthor() {
        return getChangesetDelegate().getAuthor();
    }

    public Date getTimestamp() {
        return getChangesetDelegate().getTimestamp();
    }

    public String getRawNode() {
        return getChangesetDelegate().getRawNode();
    }

    public String getBranch() {
        return getChangesetDelegate().getBranch();
    }

    public String getMessage() {
        return getChangesetDelegate().getMessage();
    }

    public List<String> getParents() {
        return getChangesetDelegate().getParents();
    }

    public List<ChangesetFile> getFiles() {
        return getChangesetDelegate().getFiles();
    }

    public String getCommitURL(SourceControlRepository repository) {
        return repository.getRepositoryUri().getCommitUrl(nodeId);
    }
}
