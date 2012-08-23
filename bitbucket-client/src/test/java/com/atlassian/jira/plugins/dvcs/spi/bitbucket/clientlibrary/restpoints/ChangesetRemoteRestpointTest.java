package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;


import static org.fest.assertions.api.Assertions.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.NoAuthAuthProvider;


/**
 * @author Martin Skurla mskurla@atlassian.com
 */
public class ChangesetRemoteRestpointTest {
    
    private static final String BITBUCKET_OWNER      = "dvcsconnectortest";
    private static final String BITBUCKET_REPO       = "testrepo";
    private static final String BITBUCKET_EMPTY_REPO = "testemptyrepo";

    private static final String THIRD_CHANGESET_NODE = "d2088255ee40";
    private static final String TIP_CHANGESET_NODE   = "cf40601136f6";
    
    
    private static BitbucketRemoteClient bitbucketRemoteClient;
    
    
    @BeforeClass
    public static void initializeBitbucketRemoteClient()
    {
        AuthProvider noAuthProvider = new NoAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL);
        
        bitbucketRemoteClient = new BitbucketRemoteClient(noAuthProvider);        
    }
    
    
    @DataProvider
    private Object[][] provideVariousChangesetPaginations()
    {
        return new Object[][] {
            { 1 }, { 2 }, { 3 }, { 4 }, { ChangesetRemoteRestpoint.DEFAULT_CHANGESETS_LIMIT }
        };
    }
    
   
    @Test(timeOut=10000)
    public void getChangesets_ShouldReturnAllChangesets()
    {       
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getAllChangesets(BITBUCKET_OWNER,
                                                                                         BITBUCKET_REPO);

        Set<String> changesetNodes = new HashSet<String>();
        int changesetCounter = 0;
        
        for (BitbucketChangeset bitbucketChangeset : changesets)
        {
            changesetCounter++;
            changesetNodes.add(bitbucketChangeset.getNode());
        }
        
        assertThat(changesetCounter).isEqualTo(6); // not only we got the exact number of changesets
        assertThat(changesetNodes).hasSize(6);     // but also they have to be unique
    }
    
    @Test(timeOut=10000, dataProvider="provideVariousChangesetPaginations")
    public void getChangesetsUntilChangesetNodeWithPagination_ShouldReturnCorrectChangesets(int pagination)
    {
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getChangesets(BITBUCKET_OWNER,
                                                                                      BITBUCKET_REPO,
                                                                                      THIRD_CHANGESET_NODE,
                                                                                      pagination);

        Set<String> changesetNodes = new HashSet<String>();
        int changesetCounter = 0;
        String expectedCombinedChangesetNodes =
                "_cf40601136f6_de66ffafa5ca_b597361d8735";
        String combinedChangesetNodes = "";
        
        for (BitbucketChangeset bitbucketChangeset : changesets)
        {
            changesetCounter++;
            changesetNodes.add(bitbucketChangeset.getNode());
            combinedChangesetNodes += "_" + bitbucketChangeset.getNode();
        }
        
        assertThat(combinedChangesetNodes).isEqualTo(expectedCombinedChangesetNodes);
        
        assertThat(changesetCounter).isEqualTo(3); // not only we got the exact number of changesets
        assertThat(changesetNodes).hasSize(3);     // but also they have to be unique
    }
    
    @Test(timeOut=10000)
    public void getChangesetsUntilTipNode_ShouldReturnZeroChangsets()
    {
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getChangesets(BITBUCKET_OWNER,
                                                                                      BITBUCKET_REPO,
                                                                                      TIP_CHANGESET_NODE);
        
        assertThat(changesets).hasSize(0);
    }
    
    @Test(timeOut=10000, expectedExceptions=NoSuchElementException.class)
    public void getChangesetsFromEmptyRepository_ShouldReturnEmptyIterable()
    {
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getAllChangesets(BITBUCKET_OWNER,
                                                                                         BITBUCKET_EMPTY_REPO);
        Iterator<BitbucketChangeset> changesetIterator = changesets.iterator();
        
        assertThat(changesetIterator.hasNext()).isFalse();
        
        // should throw NoSuchElementException
        changesetIterator.next();
    }
}