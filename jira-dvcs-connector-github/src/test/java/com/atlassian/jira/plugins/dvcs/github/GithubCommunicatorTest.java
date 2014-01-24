package com.atlassian.jira.plugins.dvcs.github;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.google.common.collect.ImmutableList;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.sal.api.net.ResponseException;

/**
 * @author Martin Skurla
 * @author Miroslav Stencel mstencel@atlassian.com
 */
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

        private final List<String> cache = new ArrayList<String>();

        @Override
        public boolean isCached(int repositoryId, String changesetNode)
        {
            return cache.contains(changesetNode);
        }

        public void add(String node)
        {
            cache.add(node);
        }

        @Override
        public boolean isEmpty(int repositoryId)
        {
            return cache.isEmpty();
        }
    }

	@BeforeMethod
	public void initializeMocksAndGithubCommunicator()
    {
        MockitoAnnotations.initMocks(this);

        communicator = new GithubCommunicator(changesetCache = new ChangesetCacheImpl(), mock(OAuthStore.class), githubClientProvider);
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
        when(githubUser.getAvatarUrl()).thenReturn("https://secure.gravatar.com/avatar/gravatarId?s=60");

        DvcsUser githubUser = communicator.getUser(repositoryMock, "USER-NAME");

        assertThat(githubUser.getAvatar())  .isEqualTo("https://secure.gravatar.com/avatar/gravatarId?s=60");
        assertThat(githubUser.getUsername()).isEqualTo("Test GitHub user login");
        assertThat(githubUser.getFullName()).isEqualTo("Test GitHub user name");
    }

    @Test
    public void gettingDetailChangeset_ShouldSendGETRequestToGithub_AndParseJsonResult() throws ResponseException, IOException
    {
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryCommit repositoryCommit = mock(RepositoryCommit.class);
        when(commitService.getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString())).thenReturn(repositoryCommit);
        Commit commit = mock(Commit.class);
        when(repositoryCommit.getCommit()).thenReturn(commit);
        when(commit.getMessage()).thenReturn("ABC-123 fix");

        Changeset detailChangeset = communicator.getChangeset(repositoryMock, "abcde");

        verify(commitService).getCommit(Matchers.<IRepositoryIdProvider>anyObject(),anyString());

        assertThat(detailChangeset.getMessage()).isEqualTo("ABC-123 fix");
    }

    @Test
    public void getChangesets_noBranches()
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");

        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock).iterator();
        assertThat(changesetIterator.hasNext()).isFalse();
        assertThat(changesetIterator.hasNext()).isFalse();

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
    public void getChangesets_onlyNexts() throws IOException
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());

        createSampleBranches(repositoryId);

        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock).iterator();

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

        assertThat(changesetCounter).isEqualTo(5);
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

        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock).iterator();

        changesetIterator.next();

        // we are on the last node
        assertThat(changesetIterator.hasNext()).isTrue();
        assertThat(changesetIterator.hasNext()).isTrue();
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

        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock).iterator();

        changesetCache.add("NODE-1");

        changesetIterator.next();
        assertThat(changesetIterator.hasNext()).isFalse();
        assertThat(changesetIterator.hasNext()).isFalse();
    }

    @Test
    public void getChangesets_twoHasNextWhenStopped() throws IOException
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());

        createBranchWithTwoNodes(repositoryId);

        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock).iterator();

        changesetCache.add("MASTER-SHA");

        assertThat(changesetIterator.hasNext()).isFalse();
        assertThat(changesetIterator.hasNext()).isFalse();

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

        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock).iterator();

        // we stop master branch
        changesetCache.add("MASTER-SHA");

        // we stopped the master branch, it should iterate branch1
        assertThat(changesetIterator.hasNext()).isTrue();
        assertThat(changesetIterator.hasNext()).isTrue();

        Changeset detailChangeset = changesetIterator.next();
        assertThat(detailChangeset.getBranch()).isEqualTo("branch1");
        assertThat(detailChangeset.getNode())  .isEqualTo("BRANCH-SHA");
    }

    @Test
    public void getChangesets_hasNext() throws IOException
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());

        createSampleBranches(repositoryId);

        Iterator<Changeset> changesetIterator = communicator.getChangesets(repositoryMock).iterator();

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

        assertThat(changesetCounter).isEqualTo(5);
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

        for ( Changeset changeset : communicator.getChangesets(repositoryMock) )
        {
            changesetCache.add(changeset.getNode());
            changesetCounter++;
        }
        assertThat(changesetCounter).isEqualTo(3);
    }

    @Test
    public void getChangsets_fullsync() throws IOException
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());

        createMoreComplexSample(repositoryId);

        int changesetCounter = 0;

        for ( Changeset changeset : communicator.getChangesets(repositoryMock) )
        {
            changesetCache.add(changeset.getNode());
            changesetCounter++;
        }
        assertThat(changesetCounter).isEqualTo(15);
    }

    @Test
    public void getFileDetailsShouldFetchCommitsFromGitHub() throws Exception
    {
        // Repository
        when(repositoryMock.getSlug())   .thenReturn("SLUG");
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        RepositoryId repositoryId = RepositoryId.create(repositoryMock.getOrgName(), repositoryMock.getSlug());

        final Changeset cs = mock(Changeset.class);
        when(cs.getNode()).thenReturn("some-hash");

        final CommitFile file = new CommitFile()
                .setStatus("modified")
                .setFilename("my_file")
                .setAdditions(1)
                .setDeletions(2);

        final RepositoryCommit ghCommit = mock(RepositoryCommit.class);
        when(ghCommit.getFiles()).thenReturn(ImmutableList.of(file));

        when(commitService.getCommit(repositoryId, cs.getNode())).thenReturn(ghCommit);

        List<ChangesetFileDetail> fileDetails = communicator.getFileDetails(repositoryMock, cs);
        assertEquals(fileDetails, ImmutableList.of(
                new ChangesetFileDetail(CustomStringUtils.getChangesetFileAction(file.getStatus()), file.getFilename(), file.getAdditions(), file.getDeletions())
        ));
    }

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
        RepositoryCommit node2 = mockRepositoryCommit(repositoryId, "NODE-2", "ABC-123 node 2 fix",
                mockRepositoryCommit(repositoryId, "NODE-1", "ABC-123 node 1 fix"));

        mockRepositoryCommit(repositoryId, "MASTER-SHA", "ABC-123 node 4 fix", node2);


        mockRepositoryCommit(repositoryId, "BRANCH-SHA", "ABC-123 node 5 fix",
                mockRepositoryCommit(repositoryId, "NODE-3", "ABC-123 node 3 fix",
                    node2));

        when(repositoryService.getBranches(repositoryId)).thenReturn(Arrays.asList(master, branch1));
    }

    private void createMoreComplexSample(RepositoryId repositoryId) throws IOException
    {
     // Branches
        RepositoryBranch master = createMockRepositoryBranch("MASTER", "MASTER-SHA");
        RepositoryBranch branch1 = createMockRepositoryBranch("branch1", "BRANCH-SHA");
        RepositoryBranch branch2 = createMockRepositoryBranch("branch2", "BRANCH2-SHA");
        RepositoryBranch branch3 = createMockRepositoryBranch("branch3", "BRANCH3-SHA");

//  B3   M  B1   B2
//               14
//          13   |
//          |    12
//       10 11  /
//      / |/| >9
//     /  8 7
//    / / |/
//  15 4  6
//   \ |  |
//     3  5
//      \ |
//        2
//        |
//        1

     // Changeset
        RepositoryCommit node8;
        RepositoryCommit node2;
        RepositoryCommit node3;
        RepositoryCommit node6;
        RepositoryCommit node7;
        RepositoryCommit node9;

        mockRepositoryCommit(repositoryId, "MASTER-SHA", "ABC-123 node 10 fix",
        node8 = mockRepositoryCommit(repositoryId, "NODE-8", "ABC-123 node 8 fix",
                        mockRepositoryCommit(repositoryId, "NODE-4", "ABC-123 node 4 fix",
                        node3 = mockRepositoryCommit(repositoryId, "NODE-3", "ABC-123 node 3 fix",
                                node2 = mockRepositoryCommit(repositoryId, "NODE-2", "ABC-123 node 2 fix",
                                                mockRepositoryCommit(repositoryId, "NODE-1", "ABC-123 node 1 fix")))),
                node6 = mockRepositoryCommit(repositoryId, "NODE-6", "ABC-123 node 6 fix",
                                mockRepositoryCommit(repositoryId, "NODE-5", "ABC-123 node 5 fix",
                                        node2))),
                mockRepositoryCommit(repositoryId, "BRANCH3-SHA", "ABC-123 node 15 fix",
                        node3));


        mockRepositoryCommit(repositoryId, "BRANCH-SHA", "ABC-123 node 13 fix",
                mockRepositoryCommit(repositoryId, "NODE-11", "ABC-123 node 11 fix",
                        node8,
                node7 = mockRepositoryCommit(repositoryId, "NODE-7", "ABC-123 node 7 fix",
                                node6),
                node9 = mockRepositoryCommit(repositoryId, "NODE-9", "ABC-123 node 9 fix",
                            node7)));

        mockRepositoryCommit(repositoryId, "BRANCH2-SHA", "ABC-123 node 14 fix",
                mockRepositoryCommit(repositoryId, "NODE-12", "ABC-123 node 12 fix",
                        node9));

        when(repositoryService.getBranches(repositoryId)).thenReturn(Arrays.asList(master, branch1, branch2, branch3));
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
}

