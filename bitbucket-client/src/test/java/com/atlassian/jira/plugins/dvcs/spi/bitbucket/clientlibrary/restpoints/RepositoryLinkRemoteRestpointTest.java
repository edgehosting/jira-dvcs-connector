package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;


import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;

import static org.fest.assertions.api.Assertions.*;


/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class RepositoryLinkRemoteRestpointTest
{
    private static final String BITBUCKET_REPO_USERNAME = "jirabitbucketconnector";
    private static final String BITBUCKET_REPO_PASSWORD = "jirabitbucketconnector1";
    private static final String BITBUCKET_REPO_OWNER    = "jirabitbucketconnector";
    private static final String BITBUCKET_REPO_SLUG     = "public-hg-repo";
    
    private static final String BITBUCKET_REPO_LINK_HANDLER = "jira";
    private static final String BITBUCKET_REPO_LINK_URL     = "http://localhost:1234/jira";
    private static final String BITBUCKET_REPO_LINK_KEY     = "XYZ";

    private static RepositoryLinkRemoteRestpoint repositoryLinkREST;
    
    private static Set<Integer> addedRepositoryLinksIds = new LinkedHashSet<Integer>();


    @BeforeClass
    public static void initializeRepositoryLinksREST()
    {
        BitbucketRemoteClient bitbucketRemoteClient =
                new BitbucketRemoteClient(new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                    BITBUCKET_REPO_USERNAME,
                                                                    BITBUCKET_REPO_PASSWORD));
        
        repositoryLinkREST = bitbucketRemoteClient.getRepositoryLinksRest();
    }
    
    @AfterClass
    public static void cleanupAddedRepositoryLinks()
    {
        for (Integer addedRepositoryLinkId : addedRepositoryLinksIds)
        {
            repositoryLinkREST.removeRepositoryLink(BITBUCKET_REPO_OWNER, BITBUCKET_REPO_SLUG, addedRepositoryLinkId);
        }
    }


    @Test(timeout=5000)
    public void gettingRepositoryLinks_ShouldNotThrowException()
    {
        repositoryLinkREST.getRepositoryLinks(BITBUCKET_REPO_OWNER, BITBUCKET_REPO_SLUG);
    }

    @Test(timeout=5000)
    public void afterAddingRepositoryLink_ShouldBeAbleToQueryTheRepositoryLinkById()
    {
        // needed because you cannot add repository link with the same KEY multiple times => 400 status code
        String repositoryLinkKey = BITBUCKET_REPO_LINK_KEY + new Random(System.currentTimeMillis()).nextInt();
        
        BitbucketRepositoryLink addedRepositoryLink = repositoryLinkREST.addRepositoryLink(BITBUCKET_REPO_OWNER,
                                                                                           BITBUCKET_REPO_SLUG,
                                                                                           BITBUCKET_REPO_LINK_HANDLER,
                                                                                           BITBUCKET_REPO_LINK_URL,
                                                                                           repositoryLinkKey);
               
        BitbucketRepositoryLink queriedRepositoryLink = repositoryLinkREST.getRepositoryLink(BITBUCKET_REPO_OWNER,
                                                                                             BITBUCKET_REPO_SLUG,
                                                                                             addedRepositoryLink.getId());
        addedRepositoryLinksIds.add(queriedRepositoryLink.getId()); // for cleanup
        
        assertThat(addedRepositoryLink.getId())     .isEqualTo(queriedRepositoryLink.getId());
        assertThat(addedRepositoryLink.getHandler()).isEqualsToByComparingFields(queriedRepositoryLink.getHandler());
    }
    
    @Test(timeout=5000)
    public void removingAlreadyAddedRepositoryLink_ShouldNotThrowException()
    {
        // needed because you cannot add repository link with the same KEY multiple times => 400 status code
        String repositoryLinkKey = BITBUCKET_REPO_LINK_KEY + new Random(System.currentTimeMillis()).nextInt();
        
        BitbucketRepositoryLink addedRepositoryLink = repositoryLinkREST.addRepositoryLink(BITBUCKET_REPO_OWNER,
                                                                                           BITBUCKET_REPO_SLUG,
                                                                                           BITBUCKET_REPO_LINK_HANDLER,
                                                                                           BITBUCKET_REPO_LINK_URL,
                                                                                           repositoryLinkKey);
        
        repositoryLinkREST.removeRepositoryLink(BITBUCKET_REPO_OWNER, BITBUCKET_REPO_SLUG, addedRepositoryLink.getId());
    }
}
