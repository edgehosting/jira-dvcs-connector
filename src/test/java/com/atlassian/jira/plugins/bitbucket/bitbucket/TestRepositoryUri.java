package com.atlassian.jira.plugins.bitbucket.bitbucket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link RepositoryUri}
 */
public class TestRepositoryUri
{

    @Test
    public void testParseRepositoryUri() {
        RepositoryUri repositoryUri = RepositoryUri.parse("owner/slug/default");
        assertEquals("owner", repositoryUri.getOwner());
        assertEquals("slug", repositoryUri.getSlug());
        assertEquals("default", repositoryUri.getBranch());
    }

    @Test
    public void testParseRepositoryFullUrl() {
        RepositoryUri repositoryUri = RepositoryUri.parse("http://bitbucket.org/owner/slug/default");
        assertEquals("owner", repositoryUri.getOwner());
        assertEquals("slug", repositoryUri.getSlug());
        assertEquals("default", repositoryUri.getBranch());
    }

}
