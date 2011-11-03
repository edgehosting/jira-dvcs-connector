package com.atlassian.jira.plugins.bitbucket.spi.github;

import java.util.List;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;

public class GithubCommunicator implements Communicator
{

    public SourceControlUser getUser(SourceControlRepository repository, String username)
    {
        return null;
    }

    public Changeset getChangeset(SourceControlRepository repository, String id)
    {
        return null;
    }

    public List<Changeset> getChangesets(SourceControlRepository repository, String startNode, int limit)
    {
        return null;
    }

    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
