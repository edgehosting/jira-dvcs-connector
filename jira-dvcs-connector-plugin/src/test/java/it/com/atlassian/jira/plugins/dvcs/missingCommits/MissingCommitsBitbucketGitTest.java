package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import com.atlassian.jira.plugins.dvcs.util.ZipUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * @author Martin Skurla
 */
public class MissingCommitsBitbucketGitTest extends AbstractBitbucketMissingCommitsTest
{
    private static final String _1ST_GIT_REPO_ZIP_TO_PUSH = "missingCommits/git/git_1st_push.zip";
    private static final String _2ND_GIT_REPO_ZIP_TO_PUSH = "missingCommits/git/git_2nd_push_after_merge.zip";

    @Override
    void createRemoteDvcsRepository()
    {
        bitbucketRepositoriesREST.createGitRepository(getMissingCommitsRepositoryName());
    }

    @Override
    void pushToRemoteDvcsRepository(String pathToRepoZip) throws Exception
    {
        File extractedRepoDir = ZipUtils.extractRepoZipIntoTempDir(pathToRepoZip);

        String gitPushUrl = String.format("https://%1$s:%2$s@bitbucket.org/%1$s/%3$s.git", DVCS_REPO_OWNER,
                DVCS_REPO_PASSWORD,
                getMissingCommitsRepositoryName());

        String gitCommand = getGitCommand();

        executeCommand(extractedRepoDir, gitCommand, "remote", "rm", "origin");
        executeCommand(extractedRepoDir, gitCommand, "remote", "add", "origin", gitPushUrl);
        executeCommand(extractedRepoDir, gitCommand, "push", "-u", "origin", "master");

        FileUtils.deleteDirectory(extractedRepoDir);
    }

    @Override
    String getFirstDvcsZipRepoPathToPush()
    {
        // repository after the 1st push in following state:
        // +------------------+------------+--------------------------------------------+
        // | Author           | Commit     | Message                                    |
        // +------------------+------------+--------------------------------------------+
        // | Miroslav Stencel | 9d08182535 | MC-1 5th commit + 2nd push {user1} [14:26] |
        // | Miroslav Stencel | f6ffeee87f | MC-1 2nd commit + 1st push {user1} [14:18] |
        // | Miroslav Stencel | db26d59a1f | MC-1 1st commit {user1} [14:16]            |
        return _1ST_GIT_REPO_ZIP_TO_PUSH;
    }

    @Override
    String getSecondDvcsZipRepoPathToPush()
    {
        // repository after the 2nd push in following state:
        // +------------------+------------+--------------------------------------------+
        // | Author           | Commit     | Message                                    |
        // +------------------+------------+--------------------------------------------+
        // | Miroslav Stencel | f59cc8a7b7 | merge + 3rd push {user2} [14:44]           |
        // | Miroslav Stencel | 9d08182535 | MC-1 5th commit + 2nd push {user1} [14:26] |
        // | Miroslav Stencel | cc2ac8c703 | MC-1 4th commit {user2} [14:25]            |
        // | Miroslav Stencel | d5d190c12c | MC-1 3rd commit {user2} [14:24]            |
        // | Miroslav Stencel | f6ffeee87f | MC-1 2nd commit + 1st push {user1} [14:18] |
        // | Miroslav Stencel | db26d59a1f | MC-1 1st commit {user1} [14:16]            |
        return _2ND_GIT_REPO_ZIP_TO_PUSH;
    }
}
