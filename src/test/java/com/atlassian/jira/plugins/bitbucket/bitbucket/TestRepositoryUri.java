package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link RepositoryUri}
 */
public class TestRepositoryUri
{
    @Test
    public void testParseRepositoryFullUrlWithBranch() {

        String repositoryUrl = "http://bitbucket.org/owner/slug/default";
        BitbucketRepositoryManager brm = new BitbucketRepositoryManager(null, null, null, null, null);
        RepositoryUri repositoryUri = brm.getRepositoryUri(repositoryUrl);

        assertEquals("owner", repositoryUri.getOwner());
        assertEquals("slug", repositoryUri.getSlug());
    }

    @Test
    public void testParseRepositoryFullUrl() {

        String repositoryUrl = "http://bitbucket.org/owner/slug";
        BitbucketRepositoryManager brm = new BitbucketRepositoryManager(null, null, null, null, null);
        RepositoryUri repositoryUri = brm.getRepositoryUri(repositoryUrl);

        assertEquals("owner", repositoryUri.getOwner());
        assertEquals("slug", repositoryUri.getSlug());
    }

}
