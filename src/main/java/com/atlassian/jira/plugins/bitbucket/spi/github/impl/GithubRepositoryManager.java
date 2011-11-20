package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.sal.api.ApplicationProperties;

public class GithubRepositoryManager extends DvcsRepositoryManager
{

    public static final String GITHUB = "github";

    public GithubRepositoryManager(RepositoryPersister repositoryPersister,@Qualifier("githubCommunicator") Communicator communicator, Encryptor encryptor, ApplicationProperties applicationProperties)
    {
        super(communicator, repositoryPersister, encryptor, applicationProperties);
    }

    @Override
    public List<Changeset> parsePayload(SourceControlRepository repository, String payload)
    {
        // TODO
        return new ArrayList<Changeset>();
    }

    @Override
    public String getRepositoryType()
    {
        return GITHUB;
    }

    @Override
    public void setupPostcommitHook(SourceControlRepository repo)
    {
        // TODO

    }

    @Override
    public void removePostcommitHook(SourceControlRepository repo)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public RepositoryUri getRepositoryUri(String urlString)
    {
        try
        {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            String hostname = url.getHost();
            String path = url.getPath();
            String[] split = StringUtils.split(path, "/");
            if (split.length<2)
            {
                throw new SourceControlException("Expected url is https://domainname.com/username/repository");
            }
            String owner = split[0];
            String slug = split[1];
            return new GithubRepositoryUri(protocol, hostname, owner, slug);
        }
        catch (MalformedURLException e)
        {
            throw new SourceControlException("Invalid url ["+urlString+"]");
        }
    }
}
