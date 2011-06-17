package com.atlassian.jira.plugins.bitbucket.bitbucket;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Unit tests for {@link Bitbucket}
 */
public class TestBitbucket
{
    @Test
    public void getAnonymousGetPublicRepository()
    {
        BitbucketRepository repo = new Bitbucket().getRepository("atlassian", "jira-bitbucket-connector");
        assertEquals(repo.getWebsite(), "https://plugins.atlassian.com/plugin/details/311676");
        assertEquals(repo.getOwner(), "atlassian");
        assertEquals(repo.getSlug(), "jira-bitbucket-connector");
        assertEquals(repo.getName(), "jira-bitbucket-connector");
    }

    @Test
    public void getAnonymousGetPublicRepositoryChangeset()
    {
        BitbucketChangeset changeset = new Bitbucket().
                getRepository("atlassian", "jira-bitbucket-connector").
                changeset("fe73ad602dd5");
        assertEquals(changeset.getAuthor(), "mbuckbee");
        assertEquals(changeset.getBranch(), "default");
    }

    @Test
    public void getAnonymousGetPublicRepositoryChangesetsList()
    {
        List<BitbucketChangeset> changesets = new Bitbucket().
                getRepository("atlassian", "jira-bitbucket-connector").
                changesets(25);
        assertEquals(25, changesets.size());
        BitbucketChangeset changeset = changesets.get(3);
        assertEquals("mbuckbee", changeset.getAuthor());
        assertEquals("default", changeset.getBranch());
        assertEquals("d703ac340a95706a86d6c107482eccd7a62c8723", changeset.getRawNode());
    }

    @Test
    public void getAnonymousGetPublicRepositoryChangesetsListPaging()
    {
        // test a range of sizes to make sure the internal paging doesn't effect the results
        assertEquals(BitbucketRepository.PAGE_SIZE-1, new Bitbucket().
                getRepository("atlassian", "jira-bitbucket-connector").
                changesets(BitbucketRepository.PAGE_SIZE-1).size());
        assertEquals(BitbucketRepository.PAGE_SIZE, new Bitbucket().
                getRepository("atlassian", "jira-bitbucket-connector").
                changesets(BitbucketRepository.PAGE_SIZE).size());
        assertEquals(BitbucketRepository.PAGE_SIZE+1, new Bitbucket().
                getRepository("atlassian", "jira-bitbucket-connector").
                changesets(BitbucketRepository.PAGE_SIZE+1).size());
    }

    @Test
    public void getAnonymousGetMissingPublicRepositoryChangesets()
    {
        try
        {
            new Bitbucket().
                    getRepository("atlassian", "jira-bitbucket-connector").
                    changeset("invalid");
            fail("BitbucketResourceNotFoundException expected but not thrown");
        }
        catch (BitbucketResourceNotFoundException e)
        {
            // expected
        }
    }
}
