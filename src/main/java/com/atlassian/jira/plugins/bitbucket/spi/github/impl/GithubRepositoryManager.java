package com.atlassian.jira.plugins.bitbucket.spi.github.impl;

import com.atlassian.jira.plugins.bitbucket.api.*;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.DvcsRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.sal.api.ApplicationProperties;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GithubRepositoryManager extends DvcsRepositoryManager
{

    public GithubRepositoryManager(RepositoryPersister repositoryPersister,@Qualifier("githubCommunicator") Communicator communicator, Encryptor encryptor, ApplicationProperties applicationProperties)
    {
        super(communicator, repositoryPersister, encryptor, applicationProperties);
    }

    public boolean canHandleUrl(String url)
    {
        // todo like in bitbucket...
        return false;
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
