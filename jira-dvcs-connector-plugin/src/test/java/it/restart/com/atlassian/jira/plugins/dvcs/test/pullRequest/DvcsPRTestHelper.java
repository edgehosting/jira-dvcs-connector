package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;

/**
 * Helper class that can be used to populate a local repository in a common way for {@link
 * it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest.PullRequestTestCases}
 */
public class DvcsPRTestHelper
{
    private final Dvcs dvcs;

    public DvcsPRTestHelper(final Dvcs dvcs)
    {
        this.dvcs = dvcs;
    }

    public String[] createBranchAndCommits(final String accountName, final String repositoryName, final String commitAuthor,
            final String commitAuthorEmail, final String password, final String fixBranchName, final String issueKey, final int numberOfCommits)
    {

        final String[] commitResults = new String[numberOfCommits];

        dvcs.addFile(accountName, repositoryName, "README.txt", "Hello World!".getBytes());
        dvcs.commit(accountName, repositoryName, "Initial commit!", commitAuthor, commitAuthorEmail);
        dvcs.push(accountName, repositoryName, accountName, password);

        dvcs.createBranch(accountName, repositoryName, fixBranchName);

        String fileName = issueKey + "_fix.txt";
        for (int i = 0; i < numberOfCommits; i++)
        {
            String fileContents = "Fix commit iteration " + i;

            dvcs.addFile(accountName, repositoryName, fileName, fileContents.getBytes());
            commitResults[i] = dvcs.commit(accountName, repositoryName, "Fix number " + i, commitAuthor, commitAuthorEmail);
        }

        dvcs.push(accountName, repositoryName, accountName, password, fixBranchName, true);

        return commitResults;
    }
}
