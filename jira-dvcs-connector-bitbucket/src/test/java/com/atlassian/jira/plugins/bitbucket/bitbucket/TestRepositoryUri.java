package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketRepositoryManager;

/**
 * Unit tests for {@link RepositoryUri}
 */
@Ignore
@Deprecated // TO BE DELETED SOON
public class TestRepositoryUri
{
    @Test
    public void testParseRepositoryFullUrlWithBranch() {

        String repositoryUrl = "http://bitbucket.org/owner/slug/default";
        BitbucketRepositoryManager brm = new BitbucketRepositoryManager(null, null, null, null, null, null, null);
        RepositoryUri repositoryUri = brm.getRepositoryUri(repositoryUrl);

        assertEquals("owner", repositoryUri.getOwner());
        assertEquals("slug", repositoryUri.getSlug());
    }

    @Test
    public void testParseRepositoryFullUrl() {

        String repositoryUrl = "http://bitbucket.org/owner/slug";
        BitbucketRepositoryManager brm = new BitbucketRepositoryManager(null, null, null, null, null, null, null);
        RepositoryUri repositoryUri = brm.getRepositoryUri(repositoryUrl);

        assertEquals("owner", repositoryUri.getOwner());
        assertEquals("slug", repositoryUri.getSlug());
    }

}
