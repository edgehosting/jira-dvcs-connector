package com.atlassian.jira.plugins.bitbucket.bitbucket.remote;

import com.atlassian.jira.plugins.bitbucket.bitbucket.Bitbucket;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketRepository;
import com.atlassian.util.concurrent.LazyReference;

/**
 * A lazy loaded remote bitbucket repository.  Will only load the remote repository if the
 * details that aren't stored locally are required.
 */
public class LazyLoadedRemoteBitbucketRepository implements BitbucketRepository
{
    private final LazyReference<BitbucketRepository> lazyReference;
    private final String owner;
    private final String slug;

    public LazyLoadedRemoteBitbucketRepository(
            final Bitbucket bitbucket, final BitbucketAuthentication auth,
            final String owner, final String slug)
    {
        this.lazyReference = new LazyReference<BitbucketRepository>()
        {
            protected BitbucketRepository create() throws Exception
            {
                return bitbucket.getRepository(auth, owner, slug);
            }
        };
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

    private BitbucketRepository getBitbucketRepository()
    {
        return lazyReference.get();
    }

    public String getWebsite()
    {
        return getBitbucketRepository().getWebsite();
    }

    public String getName()
    {
        return getBitbucketRepository().getName();
    }

    public int getFollowers()
    {
        return getBitbucketRepository().getFollowers();
    }

    public String getResourceUri()
    {
        return getBitbucketRepository().getResourceUri();
    }

    public String getLogo()
    {
        return getBitbucketRepository().getLogo();
    }

    public String getDescription()
    {
        return getBitbucketRepository().getDescription();
    }
}
