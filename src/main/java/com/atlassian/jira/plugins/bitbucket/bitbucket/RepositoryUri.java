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
        return new RepositoryUri(split[0], split[1]);
    }

    private final String owner;
    private final String slug;

    public RepositoryUri(String owner, String slug)
    {
        this.owner = owner;
        this.slug = slug;
    }

    public String getOwner()
    {
        return owner;
    }

    public String getSlug()
    {
        return slug;
    }

    public String getRepositoryUri()
    {
        return owner + "/" + slug;
    }

    public String getRepositoryUrl()
    {
        return "https://bitbucket.org/"+getRepositoryUri();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryUri that = (RepositoryUri) o;
        return  !(owner != null ? !owner.equals(that.owner) : that.owner != null)
                && !(slug != null ? !slug.equals(that.slug) : that.slug != null);
    }

    @Override
    public int hashCode()
    {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (slug != null ? slug.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return getRepositoryUri();
    }
}
