package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import com.atlassian.jira.plugins.dvcs.util.ZipUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * @author Martin Skurla
 */
public class MissingCommitsBitbucketMercurialTest extends AbstractBitbucketMissingCommitsTest
{
    private static final String _1ST_HG_REPO_ZIP_TO_PUSH = "missingCommits/mercurial/hg_1st_push.zip";
    private static final String _2ND_HG_REPO_ZIP_TO_PUSH = "missingCommits/mercurial/hg_2nd_push_after_merge.zip";

    @Override
    void createRemoteDvcsRepository()
    {
        bitbucketRepositoriesREST.createHgRepository(getMissingCommitsRepositoryName());
    }

    @Override
    void pushToRemoteDvcsRepository(String pathToRepoZip) throws Exception
    {
        File extractedRepoDir = ZipUtils.extractRepoZipIntoTempDir(pathToRepoZip);

        String hgPushUrl = String.format("https://%1$s:%2$s@bitbucket.org/%1$s/%3$s", DVCS_REPO_OWNER,
                DVCS_REPO_PASSWORD,
                getMissingCommitsRepositoryName());
        executeCommand(extractedRepoDir, getHgCommand(), "push", hgPushUrl);
        FileUtils.deleteDirectory(extractedRepoDir);
    }

    @Override
    String getFirstDvcsZipRepoPathToPush()
    {
        // repository after the 1st push in following state:
        // +---------------+---------+--------------------------------------------+
        // | Author        | Commit  | Message                                    |
        // +---------------+---------+--------------------------------------------+
        // | Martin Skurla | 8b32e32 | MC-1 5th commit + 2nd push {user1} [10:47] |
        // | Martin Skurla | ccdd16b | MC-1 2nd commit + 1st push {user1} [10:38] |
        // | Martin Skurla | 792d8d6 | MC-1 1st commit {user1} [10:37]            |
        return _1ST_HG_REPO_ZIP_TO_PUSH;
    }

    @Override
    String getSecondDvcsZipRepoPathToPush()
    {
        // repository afther the 2nd push in following state:
        // +---------------+---------+--------------------------------------------+
        // | Author        | Commit  | Message                                    |
        // +---------------+---------+--------------------------------------------+
        // | Martin Skurla | 9caa788 | merge + 3rd push {user2} [11:04]           |
        // | Martin Skurla | 066e3b1 | MC-1 4th commit {user2} [10:45]            |
        // | Martin Skurla | 1b05d76 | MC-1 3rd commit {user2} [10:44]            |
        // | Martin Skurla | 8b32e32 | MC-1 5th commit + 2nd push {user1} [10:47] |
        // | Martin Skurla | ccdd16b | MC-1 2nd commit + 1st push {user1} [10:38] |
        // | Martin Skurla | 792d8d6 | MC-1 1st commit {user1} [10:37]            |
        return _2ND_HG_REPO_ZIP_TO_PUSH;
    }
}
