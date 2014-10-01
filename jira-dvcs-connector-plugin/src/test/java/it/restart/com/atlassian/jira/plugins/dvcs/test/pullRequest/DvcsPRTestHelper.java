package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Helper class that can be used to populate a local repository in a common way for {@link
 * it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest.PullRequestTestCases}
 */
public class DvcsPRTestHelper
{
    private final Dvcs dvcs;
    private String accountName;
    private final String commitAuthor;
    private final String commitAuthorEmail;
    private final String password;

    public DvcsPRTestHelper(final Dvcs dvcs, final String accountName, final String commitAuthor,
            final String commitAuthorEmail, final String password)
    {
        this.dvcs = dvcs;
        this.accountName = accountName;
        this.commitAuthor = commitAuthor;
        this.commitAuthorEmail = commitAuthorEmail;
        this.password = password;
    }

    public Collection<String> createBranchAndCommits(final String repositoryName, final String fixBranchName, final String issueKey, final int numberOfCommits)
    {
        final Collection<String> commitResults = new ArrayList<String>();

        dvcs.addFile(this.accountName, repositoryName, "README.txt", "Hello World!".getBytes());
        dvcs.commit(accountName, repositoryName, "Initial commit!", commitAuthor, commitAuthorEmail);
        dvcs.push(accountName, repositoryName, accountName, password);

        dvcs.createBranch(accountName, repositoryName, fixBranchName);

        String fileName = issueKey + "_fix.txt";
        for (int i = 0; i < numberOfCommits; i++)
        {
            String fileContents = "Fix commit iteration " + i;

            dvcs.addFile(accountName, repositoryName, fileName, fileContents.getBytes());
            commitResults.add(dvcs.commit(accountName, repositoryName, "Fix number " + i, commitAuthor, commitAuthorEmail));
        }

        dvcs.push(accountName, repositoryName, accountName, password, fixBranchName, true);

        return commitResults;
    }

    public Collection<String> addMoreCommitsAndPush(final String repositoryName, final String fixBranchName, final String issueKey, final int numberOfCommits)
    {
        final Collection<String> commitResults = new ArrayList<String>();
        String fileName = issueKey + "_fix_UPDATED.txt";
        for (int i = 0; i < numberOfCommits; i++)
        {
            String fileContents = "Fix commit iteration " + i;

            dvcs.addFile(accountName, repositoryName, fileName, fileContents.getBytes());
            commitResults.add(dvcs.commit(accountName, repositoryName, "Fix number " + i, commitAuthor, commitAuthorEmail));
        }
        dvcs.push(accountName, repositoryName, accountName, password, fixBranchName);
        return commitResults;
    }
}
