package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;


import static org.fest.assertions.api.Assertions.assertThat;

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
public class ChangesetRemoteRestpointTest
{
    private static final String BITBUCKET_OWNER      = "dvcsconnectortest";
    private static final String BITBUCKET_REPO       = "testrepo";
    private static final String BITBUCKET_EMPTY_REPO = "testemptyrepo";

    // all 6 changesets are numbered from bottom of the same page
    private static final String TIP_CHANGESET_NODE  = "cf40601136f6";
    private static final String _5TH_CHANGESET_NODE = "de66ffafa5ca";
    private static final String _4TH_CHANGESET_NODE = "b597361d8735";
    private static final String _3RD_CHANGESET_NODE = "d2088255ee40";
    private static final String _2ND_CHANGESET_NODE = "7c029943eb97";
    private static final String _1ST_CHANGESET_NODE = "c02f3167afcc";

    private static final int NUMBER_OF_ALL_CHANGESETS = 6;
    
    private static BitbucketRemoteClient bitbucketRemoteClient;
    
    @BeforeClass
    public static void initializeBitbucketRemoteClient()
    {
        AuthProvider noAuthProvider = new NoAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL);
        bitbucketRemoteClient = new BitbucketRemoteClient(noAuthProvider);        
    }
    
    @SuppressWarnings("unused")
    @DataProvider
    private Object[][] provideVariousChangesetPaginations()
    {
        return new Object[][] {
            { 1 }, { 2 }, { 3 }, { 4 }, { 5 }, { 6 }, { 7 } // full range for 6 changesets
        };
    }
    
   
    @Test(timeOut=10000, dataProvider="provideVariousChangesetPaginations")
    public void gettingChangesetsWithPagination_ShouldReturnAllChangesets(int pagination)
    {       
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getChangesets(BITBUCKET_OWNER,
                                                                                      BITBUCKET_REPO,
                                                                                      null,
                                                                                      pagination);
        Set<String> changesetNodes = new HashSet<String>();
        int changesetCounter = 0;
        String combinedChangesetNodes = "";
        
        for (BitbucketChangeset bitbucketChangeset : changesets)
        {
            changesetCounter++;
            changesetNodes.add(bitbucketChangeset.getNode());
            combinedChangesetNodes += bitbucketChangeset.getNode();
        }
       
        assertThat(changesetCounter).isEqualTo(NUMBER_OF_ALL_CHANGESETS); // not only we got the exact number of changesets
        assertThat(changesetNodes).hasSize(NUMBER_OF_ALL_CHANGESETS);     // but also their nodes have to be unique

        assertThat(combinedChangesetNodes).isEqualTo(TIP_CHANGESET_NODE  + _5TH_CHANGESET_NODE + _4TH_CHANGESET_NODE +
                                                     _3RD_CHANGESET_NODE + _2ND_CHANGESET_NODE + _1ST_CHANGESET_NODE);
    }
    
    @Test(timeOut=10000, dataProvider="provideVariousChangesetPaginations")
    public void gettingChangesetsUntilChangesetNodeWithPagination_ShouldReturnCorrectChangesets(int pagination)
    {
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getChangesets(BITBUCKET_OWNER,
                                                                                      BITBUCKET_REPO,
                                                                                      _3RD_CHANGESET_NODE,
                                                                                      pagination);
        Set<String> changesetNodes = new HashSet<String>();
        int changesetCounter = 0;
        String combinedChangesetNodes = "";      
        
        for (BitbucketChangeset bitbucketChangeset : changesets)
        {
            changesetCounter++;
            changesetNodes.add(bitbucketChangeset.getNode());
            combinedChangesetNodes += bitbucketChangeset.getNode();
        }
       
        assertThat(changesetCounter).isEqualTo(3); // not only we got the exact number of changesets
        assertThat(changesetNodes).hasSize(3);     // but also their nodes have to be unique

        assertThat(combinedChangesetNodes).isEqualTo(TIP_CHANGESET_NODE + _5TH_CHANGESET_NODE + _4TH_CHANGESET_NODE);
    }
    
    @Test(timeOut=10000)
    public void gettingChangesetsUntilTipNode_ShouldReturnZeroChangsets()
    {
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getChangesets(BITBUCKET_OWNER,
                                                                                      BITBUCKET_REPO,
                                                                                      TIP_CHANGESET_NODE);
        assertThat(changesets).hasSize(0);
    }
    
    @Test(timeOut=10000, expectedExceptions=NoSuchElementException.class)
    public void gettingChangesetsFromEmptyRepository_ShouldReturnEmptyIterable()
    {       
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getChangesets(BITBUCKET_OWNER,
                                                                                      BITBUCKET_EMPTY_REPO);
        Iterator<BitbucketChangeset> changesetIterator = changesets.iterator();
        
        assertThat(changesetIterator.hasNext()).isFalse();
        
        // should throw NoSuchElementException
        changesetIterator.next();
    }

    @Test(timeOut=10000)
    public void gettingChangesetsFromIterable_ShouldWorkWithoutHasNextCall()
    {
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getChangesets(BITBUCKET_OWNER,
                                                                                      BITBUCKET_REPO);
        Iterator<BitbucketChangeset> changesetIterator = changesets.iterator();

        int changesetCounter = 0;
        try
        {
            while (true)
            {
                changesetIterator.next();
                changesetCounter++;
            }
        }
        catch (NoSuchElementException e)
        {
            assertThat(changesetCounter).isEqualTo(NUMBER_OF_ALL_CHANGESETS);
        }
    }

    @Test(timeOut=10000)
    public void gettingChangesetsFromIterable_ShouldWorkWithMultipleHasNextCallsOnAfterTheOther()
    {
        Iterable<BitbucketChangeset> changesets = bitbucketRemoteClient.getChangesetsRest()
                                                                       .getChangesets(BITBUCKET_OWNER,
                                                                                      BITBUCKET_REPO);

        Iterator<BitbucketChangeset> changesetIterator = changesets.iterator();

        int changesetCounter = 0;
        while (changesetIterator.hasNext())
        {
            changesetIterator.hasNext();
            changesetIterator.hasNext();

            changesetIterator.next();
            changesetCounter++;

            changesetIterator.hasNext();
            changesetIterator.hasNext();
        }

        assertThat(changesetCounter).isEqualTo(NUMBER_OF_ALL_CHANGESETS);
    }
}