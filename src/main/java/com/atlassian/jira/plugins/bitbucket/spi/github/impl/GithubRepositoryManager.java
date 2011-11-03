package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.sal.api.ApplicationProperties;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubRepositoryManager extends DvcsRepositoryManager
{

    public GithubRepositoryManager(RepositoryPersister repositoryPersister,@Qualifier("githubCommunicator") Communicator communicator, Encryptor encryptor, ApplicationProperties applicationProperties)
    {
        super(communicator, repositoryPersister, encryptor, applicationProperties);
    }

    public boolean canHandleUrl(String url)
    {
        Pattern p = Pattern.compile("^(https|http)://github.com/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
        Matcher m = p.matcher(url);
        return m.matches();

    }

    public List<Changeset> parsePayload(SourceControlRepository repository, String payload)
    {
        // todo
        return new ArrayList<Changeset>();
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
