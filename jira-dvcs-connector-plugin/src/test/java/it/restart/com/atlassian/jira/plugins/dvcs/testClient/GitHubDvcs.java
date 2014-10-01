package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestSupport;
import com.atlassian.jira.plugins.dvcs.base.resource.GitTestSupport;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;

public class GitHubDvcs implements Dvcs
{
    private GitTestSupport gitTestSupport;
    private final GitHubTestSupport gitHubTestSupport;

    public GitHubDvcs(GitHubTestSupport gitHubTestSupport)
    {
        this.gitHubTestSupport = gitHubTestSupport;
        gitTestSupport = new GitTestSupport();
    }

    @Override
    public String getDvcsType()
    {
        return RepositoryRemoteRestpoint.ScmType.GIT;
    }

    @Override
    public String getDefaultBranchName()
    {
        return "master";
    }

    @Override
    public void createBranch(final String owner, final String repositoryName, final String branchName)
    {
        gitTestSupport.createBranch(repositoryName, branchName);
        gitTestSupport.checkout(repositoryName, branchName);
    }

    @Override
    public void switchBranch(final String owner, final String repositoryName, final String branchName)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void addFile(final String owner, final String repositoryName, final String filePath, final byte[] content)
    {
        gitTestSupport.addFile(repositoryName, filePath, content);
    }

    @Override
    public String commit(final String owner, final String repositoryName, final String message, final String authorName, final String authorEmail)
    {
        return gitTestSupport.commit(repositoryName, message, authorName, authorEmail);
    }

    @Override
    public void push(final String owner, final String repositoryName, final String username, final String password)
    {
        gitTestSupport.push(repositoryName, username, password);
    }

    @Override
    public void push(final String owner, final String repositoryName, final String username, final String password, final String reference, final boolean newBranch)
    {
        gitTestSupport.push(repositoryName, username, password);
    }

    @Override
    public void push(final String owner, final String repositoryName, final String username, final String password, final String reference)
    {
        gitTestSupport.push(repositoryName, username, password);
    }

    @Override
    public void createTestLocalRepository(final String owner, final String repositoryName, final String username, final String password)
    {
        gitTestSupport.addRepository(repositoryName);

        gitTestSupport.clone(repositoryName, gitHubTestSupport.getRepository(owner, repositoryName).getCloneUrl(), username, password);
    }

    @Override
    public void deleteTestRepository(final String repositoryUri)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void deleteAllRepositories()
    {
        gitTestSupport.deleteAllRepositories();
    }
}
