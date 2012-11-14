package com.atlassian.jira.plugins.dvcs.github;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryHook;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.TypedResource;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.sal.api.net.ResponseException;


/**
 * @author Martin Skurla
 * @author Miroslav Stencel mstencel@atlassian.com
 */
@RunWith(MockitoJUnitRunner.class)
public class GithubCommunicatorTest
{
	@Mock
	private Repository repositoryMock;
	@Mock
	private GithubClientProvider githubClientProvider;
	@Mock
	private CommitService commitService;
	@Mock
	private RepositoryService repositoryService;
	@Mock
	private UserService userService;
	@Mock
	private User githubUser;
	
	// tested object
	private DvcsCommunicator communicator;

	private ChangesetCacheImpl changesetCache;
	
	private class ChangesetCacheImpl implements ChangesetCache
	{
	    
	    private List<String> cache = new ArrayList<String>();
	    
        @Override
        public boolean isCached(int repositoryId, String changesetNode)
        {
            return cache.contains(changesetNode);
        }
	    
        public void add(String node)
        {
            cache.add(node);
        }

        public void clear()
        {
            cache.clear();
        }
        
	}
	
	@Before
	public void initializeGithubCommunicator()
    {
        communicator = new GithubCommunicator(changesetCache = new ChangesetCacheImpl(), mock(GithubOAuth.class), githubClientProvider);
        when(githubClientProvider.getRepositoryService(repositoryMock)).thenReturn(repositoryService);
        when(githubClientProvider.getUserService(repositoryMock)).thenReturn(userService);
        when(githubClientProvider.getCommitService(repositoryMock)).thenReturn(commitService);
	}

	@Test
	public void settingUpPostcommitHook_ShouldSendPOSTRequestToGithub() throws IOException
    {
        when(repositoryMock.getOrgName()).thenReturn("ORG");
		when(repositoryMock.getSlug())   .thenReturn("SLUG");
		
		communicator.setupPostcommitHook(repositoryMock, "POST-COMMIT-URL");
		
		verify(repositoryService).createHook(Matchers.<IRepositoryIdProvider>anyObject(),Matchers.<RepositoryHook>anyObject());
	}

    @Test
    public void gettingUser_ShouldSendGETRequestToGithub_AndParseJsonResult() throws Exception
    {
        when(userService.getUser("USER-NAME")).thenReturn(githubUser);
        when(githubUser.getLogin()).thenReturn("Test GitHub user login");
        when(githubUser.getName()).thenReturn("Test GitHub user name");
        when(githubUser.getGravatarId()).thenReturn("gravatarId");
        
        DvcsUser githubUser = communicator.getUser(repositoryMock, "USER-NAME");
        
        assertThat(githubUser.getAvatar(), is("https://secure.gravatar.com/avatar/gravatarId?s=60"));
        assertThat(githubUser.getUsername(), is("Test GitHub user login"));
        assertThat(githubUser.getFullName(), is("Test GitHub user name"));
    }

    @Test
    public void gettingDetailChangeset_ShouldSendGETRequestToGithub_AndParseJsonResult() throws ResponseException, IOException
    {
        Changeset changesetMock = mock(Changeset.class);
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryCommit repositoryCommit = mock(RepositoryCommit.class);
        when(commitService.getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString())).thenReturn(repositoryCommit);
        Commit commit = mock(Commit.class);
        when(repositoryCommit.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("ABC-123 fix");

        Changeset detailChangeset = communicator.getDetailChangeset(repositoryMock, changesetMock);
        
        verify(commitService).getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString());

        assertThat(detailChangeset.getMessage(), is("ABC-123 fix"));
    }
    
    @Test
    public void getChangesets_noBranches()
    {
     // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        
        int changesetCounter = 0;
        
        for ( Changeset changeset : communicator.getChangesets(repositoryMock, new Date()) )
        {
            changesetCounter++;
        }
        assertThat(changesetCounter, is(0));
    }
    
    @Test
    public void getChangesets_onlyNexts() throws IOException
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createSampleBranches(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();
        
        int changesetCounter = 0;
        
        while (true)
        {
            try {
                Changeset detailChangeset = changesetIterator.next();
                
                // we need to simulate saving of the processed changeset
                changesetCache.add(detailChangeset.getNode());
                
                changesetCounter++;
            } catch (NoSuchElementException e)
            {
                break;
            }
        }
        
        assertThat(changesetCounter, is(5));
    }
    
    @Test
    public void getChangesets_hasNextAndNext() throws IOException
    {
        // Testing whether hasNext and consquent next if cache has been changed
        
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createBranchWithTwoNodes(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();
        
        assertTrue(changesetIterator.hasNext());
        changesetCache.add("MASTER-SHA");
        try
        {
            Changeset detailedChangeset = changesetIterator.next();
        } catch (NoSuchElementException e)
        {
            return;
        }
        
        fail("Exception should be thrown");
    }

    @Test
    public void getChangesets_twoHasNextOnLast() throws IOException
    {
        // Testing hasNext at the end of the iteration
        
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createBranchWithTwoNodes(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();

        changesetIterator.next();
        
        // we are on the last node
        assertTrue(changesetIterator.hasNext());
        assertTrue(changesetIterator.hasNext());
    }
    
    @Test
    public void getChangesets_twoHasNextOnLast2() throws IOException
    {
        // Testing hasNext at the end of the iteration, the last node is in cache
        
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createBranchWithTwoNodes(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();
        
        changesetCache.add("NODE-1");
        
        changesetIterator.next();
        assertFalse(changesetIterator.hasNext());
        assertFalse(changesetIterator.hasNext());
    }
    
    @Test
    public void getChangesets_twoHasNextWithChangedCacheEnd() throws IOException
    {
        // Testing hasNext when cache changes meanwhile at the end
        
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createBranchWithTwoNodes(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();
        
        changesetIterator.next();
        assertTrue(changesetIterator.hasNext());
        changesetCache.add("NODE-1");
        assertFalse(changesetIterator.hasNext());
    }
    
    @Test
    public void getChangesets_twoHasNextWithChangedCacheMoreBranches() throws IOException
    {
        // Testing hasNext when cache changes meanwhile at the end, with more branches
        
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createSampleBranches(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();
        
        changesetIterator.next();
        assertTrue(changesetIterator.hasNext());
        changesetCache.add("NODE-2");
        assertTrue(changesetIterator.hasNext());
        // we should move to the next branch
        assertThat(changesetIterator.next().getNode(), is("BRANCH-SHA"));
    }
    
    @Test
    public void getChangesets_twoHasNextWhenStopped() throws IOException 
    {       
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createBranchWithTwoNodes(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();
        
        changesetCache.add("MASTER-SHA");
        
        assertFalse(changesetIterator.hasNext());
        assertFalse(changesetIterator.hasNext());
       
        // this should throw an exception
        try
        {
            changesetIterator.next();
        } catch (NoSuchElementException e)
        {
            return;
        }
        
        fail("Exception should be thrown.");
    }
    
    @Test
    public void getChangesets_MasterBranchStopped() throws IOException 
    {       
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createSampleBranches(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();
        
        // we stop master branch
        changesetCache.add("MASTER-SHA");
        
        // we stopped the master branch, it should iterate branch1
        assertTrue(changesetIterator.hasNext());
        assertTrue(changesetIterator.hasNext());
       
        Changeset detailChangeset = changesetIterator.next();
        assertThat(detailChangeset.getBranch(), is("branch1"));
        assertThat(detailChangeset.getNode(), is("BRANCH-SHA"));
    }
    
    @Test
    public void getChangesets_hasNext() throws IOException
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createSampleBranches(repositoryId);
        
        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock, new Date()).iterator();
        
        int changesetCounter = 0;
        
        while (changesetIterator.hasNext())
        {
            changesetIterator.hasNext();
            changesetIterator.hasNext();

            Changeset detailChangeset = changesetIterator.next();
            changesetCounter++;
            
            // we need to simulate saving of the processed changeset
            changesetCache.add(detailChangeset.getNode());
            
            changesetIterator.hasNext();
            changesetIterator.hasNext();
        }
        
        assertThat(changesetCounter, is(5));
    }
    
    @Test
    public void getChangesets_earlyStopTest()
    {
        // iteration should continue on the sibling branch immediately after calling stop
    }
    
    @Test
    public void getChangsets_softsync() throws IOException
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());
        
        createSampleBranches(repositoryId);
        
        changesetCache.add("NODE-1");
        changesetCache.add("NODE-2");
        
        int changesetCounter = 0;
        
        for ( Changeset changeset : communicator.getChangesets(repositoryMock, new Date()) )
        {
            changesetCounter++;
        }
        assertThat(changesetCounter, is(3));
    }
    // builder.newBranch("MASTER").newCommit("MASTER-SHA","ABC-123 fix");
    
    private void createBranchWithTwoNodes(RepositoryId repositoryId) throws IOException
    {
     // Branches
        RepositoryBranch master = createMockRepositoryBranch("MASTER", "MASTER-SHA");
        
     // Changeset
        mockRepositoryCommit(repositoryId, "MASTER-SHA", "ABC-123 fix",
                mockRepositoryCommit(repositoryId, "NODE-1", "ABC-123 node 1 fix"));
        
        when(repositoryService.getBranches(repositoryId)).thenReturn(Arrays.asList(master));
    }
    
    private void createSampleBranches(RepositoryId repositoryId) throws IOException
    {
     // Branches
        RepositoryBranch master = createMockRepositoryBranch("MASTER", "MASTER-SHA");
        RepositoryBranch branch1 = createMockRepositoryBranch("branch1", "BRANCH-SHA");    
        
     // Changeset
        mockRepositoryCommit(repositoryId, "MASTER-SHA", "ABC-123 node 4 fix",
                mockRepositoryCommit(repositoryId, "NODE-2", "ABC-123 node 2 fix",
                        mockRepositoryCommit(repositoryId, "NODE-1", "ABC-123 node 1 fix")));
        
        
        mockRepositoryCommit(repositoryId, "BRANCH-SHA", "ABC-123 node 5 fix",
                mockRepositoryCommit(repositoryId, "NODE-3", "ABC-123 node 3 fix",
                    mockRepositoryCommit(repositoryId, "NODE-2", "ABC-123 node 2 fix",
                            mockRepositoryCommit(repositoryId, "NODE-1", "ABC-123 node 1 fix"))));
        
        when(repositoryService.getBranches(repositoryId)).thenReturn(Arrays.asList(master, branch1));
    }
    
    private RepositoryBranch createMockRepositoryBranch(final String name, final String topNode)
    {
        RepositoryBranch repositoryBranchMock = mock(RepositoryBranch.class);
        when(repositoryBranchMock.getName()).thenReturn(name);
        
        TypedResource branchCommit = mock(TypedResource.class);
        when(branchCommit.getSha()).thenReturn(topNode);
        when(repositoryBranchMock.getCommit()).thenReturn(branchCommit);
        
        return repositoryBranchMock;
    }
    
    private RepositoryCommit mockRepositoryCommit(final RepositoryId repositoryId, final String node, final String message, RepositoryCommit... parents) throws IOException
    {
        RepositoryCommit repositoryCommit = mock(RepositoryCommit.class);
        when(commitService.getCommit(repositoryId,node)).thenReturn(repositoryCommit);
        Commit commit = mock(Commit.class);
        when(repositoryCommit.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn(message);
        when(commit.getSha()).thenReturn(node);
        when(repositoryCommit.getSha()).thenReturn(node);
        List<Commit> parentCommits = new ArrayList<Commit>();
        for ( RepositoryCommit parentRepositoryCommit : parents )
        {
            parentCommits.add(parentRepositoryCommit.getCommit());
        }
        when(repositoryCommit.getParents()).thenReturn(parentCommits);
        return repositoryCommit;
    }
    
    private RepositoryId createRepositoryId(Repository repository)
    {
        return RepositoryId.create(
                repository.getOrgName(), repository.getSlug());
    }
    
    private Commit mockCommit(RepositoryCommit repositoryCommit)
    {
        Commit commit = mock(Commit.class);
        when(repositoryCommit.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("ABC-123 fix");
        return commit;
    }
}

