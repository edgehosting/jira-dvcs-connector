package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;
import com.atlassian.sal.api.ApplicationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubRepositoryManager extends DvcsRepositoryManager
{

    public GithubRepositoryManager(RepositoryPersister repositoryPersister, Communicator communicator, Encryptor encryptor, ApplicationProperties applicationProperties)
    {
        super(communicator, repositoryPersister, encryptor, applicationProperties);
    }

    public boolean canHandleUrl(String url)
    {
        // Valid URL and URL starts with bitbucket.org domain
        Pattern p = Pattern.compile("^(https|http)://github.com/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher m = p.matcher(url);
        return m.matches();

    }

    public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progress)
    {
        // todo
        return null;
    }

    public List<Changeset> parsePayload(SourceControlRepository repository, String payload)
    {
        // todo
        return new ArrayList<Changeset>();
    }

    public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset)
    {
        // todo
        return "<div></div>";
    }

    public String getRepositoryType()
    {
        return "github";
    }

    public void setupPostcommitHook(SourceControlRepository repo)
    {
        // TODO
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void removePostcommitHook(SourceControlRepository repo)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
