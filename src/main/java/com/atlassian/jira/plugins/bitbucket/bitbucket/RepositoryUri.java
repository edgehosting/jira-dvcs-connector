package com.atlassian.jira.plugins.bitbucket.bitbucket;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Used to identify a repository, contains an owner, a slug and optionally, a branch.
 */
public class RepositoryUri
{
    public static RepositoryUri parse(String uri)
    {
        try
        {
            URL url = new URL(uri);
            uri = url.getPath();
            uri = uri.substring(1);
        }
        catch (MalformedURLException e)
        {
            // assume the uri only includes a path
        }

        String[] split = uri.split("/");
        return new RepositoryUri(split[0], split[1], split.length > 2 ? split[2] : "default");
    }

    private final String owner;
    private final String slug;
    private final String branch;

    public RepositoryUri(String owner, String slug, String branch)
    {
        this.owner = owner;
        this.slug = slug;
        this.branch = branch;
    }

    public String getOwner()
    {
        return owner;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getRepositoryUri()
    {
        return owner + "/" + slug + "/" + branch;
    }

    public String getRepositoryUrl()
    {
        return "https://bitbucket.org/"+getRepositoryUri();
    }
}
