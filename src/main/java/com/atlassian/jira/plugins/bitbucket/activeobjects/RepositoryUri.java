package com.atlassian.jira.plugins.bitbucket.activeobjects;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Used to identify a repository, contains an owner, a slug and optionally, a branch.
 * @deprecated used in migration only
 */
class RepositoryUri
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

    public RepositoryUri(String owner, String slug)
    {
        this.owner = owner;
        this.slug = slug;
        this.branch = null;
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
        return owner + "/" + slug + ((branch!=null)?("/" + branch):"");
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
        return !(branch != null ? !branch.equals(that.branch) : that.branch != null)
                && !(owner != null ? !owner.equals(that.owner) : that.owner != null)
                && !(slug != null ? !slug.equals(that.slug) : that.slug != null);
    }

    @Override
    public int hashCode()
    {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (slug != null ? slug.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return getRepositoryUri();
    }
}