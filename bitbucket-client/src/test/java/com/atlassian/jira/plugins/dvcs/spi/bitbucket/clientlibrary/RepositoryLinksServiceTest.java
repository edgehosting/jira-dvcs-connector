package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary;


import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;


/**
 * @author Martin Skurla
 */
public class RepositoryLinksServiceTest {

    private static final String BITBUCKET_API_URL       = "https://bitbucket.org/!api/1.0";
    private static final String BITBUCKET_REPO_USERNAME = "jirabitbucketconnector";
    private static final String BITBUCKET_REPO_PASSWORD = "jirabitbucketconnector1";
    private static final String BITBUCKET_REPO_OWNER    = "jirabitbucketconnector";
    private static final String BITBUCKET_REPO_SLUG     = "public-hg-repo";
    
    private static final String BITBUCKET_REPO_LINK_HANDLER = "jira";
    private static final String BITBUCKET_REPO_LINK_URL     = "http://localhost:1234/jira";
    private static final String BITBUCKET_REPO_LINK_KEY     = "XYZ";

    private static RepositoryLinksService repositoryLinksService;
    
    private static Set<Integer> addedRepositoryLinksIds = new LinkedHashSet<Integer>();


    @BeforeClass
    public static void initializeRepositoryLinksService()
    {
        BitbucketClient bitbucketClient = new BitbucketClient(BITBUCKET_API_URL);
        bitbucketClient.setAuthorisation(BITBUCKET_REPO_USERNAME, BITBUCKET_REPO_PASSWORD);

        repositoryLinksService = new RepositoryLinksService(bitbucketClient);
    }
    
    @AfterClass
    public static void cleanupAddedRepositoryLinks() throws BitbucketClientException
    {
        for (Integer addedRepositoryLinkId : addedRepositoryLinksIds)
        {
            repositoryLinksService.removeRepositoryLink(BITBUCKET_REPO_OWNER, BITBUCKET_REPO_SLUG, addedRepositoryLinkId);
        }
    }


    @Test(timeout=5000)
    public void gettingRepositoryLinks_ShouldNotThrowException() throws BitbucketClientException
    {
        repositoryLinksService.getRepositoryLinks(BITBUCKET_REPO_OWNER, BITBUCKET_REPO_SLUG);
    }

    @Test(timeout=5000)
    public void afterAddingRepositoryLink_ShouldBeAbleToQueryTheRepositoryLinkById() throws BitbucketClientException
    {
        // needed because you cannot add repository link with the same KEY multiple times => 400 status code
        String repositoryLinkKey = BITBUCKET_REPO_LINK_KEY + new Random(System.currentTimeMillis()).nextInt();
        
        RepositoryLink addedRepositoryLink = repositoryLinksService.addRepositoryLink(BITBUCKET_REPO_OWNER,
                                                                                      BITBUCKET_REPO_SLUG,
                                                                                      BITBUCKET_REPO_LINK_HANDLER,
                                                                                      BITBUCKET_REPO_LINK_URL,
                                                                                      repositoryLinkKey);
               
        RepositoryLink queriedRepositoryLink = repositoryLinksService.getRepositoryLink(BITBUCKET_REPO_OWNER,
                                                                                        BITBUCKET_REPO_SLUG,
                                                                                        addedRepositoryLink.getId());
        addedRepositoryLinksIds.add(queriedRepositoryLink.getId()); // for cleanup
        
        assertThat(addedRepositoryLink.getId())     .isEqualTo(queriedRepositoryLink.getId());
        assertThat(addedRepositoryLink.getHandler()).isEqualsToByComparingFields(queriedRepositoryLink.getHandler());
    }
    
    @Test(timeout=5000)
    public void removingAlreadyAddedRepositoryLink_ShouldNotThrowException() throws BitbucketClientException
    {
        // needed because you cannot add repository link with the same KEY multiple times => 400 status code
        String repositoryLinkKey = BITBUCKET_REPO_LINK_KEY + new Random(System.currentTimeMillis()).nextInt();
        
        RepositoryLink addedRepositoryLink = repositoryLinksService.addRepositoryLink(BITBUCKET_REPO_OWNER,
                                                                                      BITBUCKET_REPO_SLUG,
                                                                                      BITBUCKET_REPO_LINK_HANDLER,
                                                                                      BITBUCKET_REPO_LINK_URL,
                                                                                      repositoryLinkKey);
        
        repositoryLinksService.removeRepositoryLink(BITBUCKET_REPO_OWNER, BITBUCKET_REPO_SLUG, addedRepositoryLink.getId());
    }
}
