package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestSupport;
import com.atlassian.jira.plugins.dvcs.base.resource.GitTestResource;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;

public class GitHubDvcs implements Dvcs
{
    private GitTestResource gitTestResource;
    private final GitHubTestSupport gitHubTestSupport;

    public GitHubDvcs(GitHubTestSupport gitHubTestSupport)
    {
        this.gitHubTestSupport = gitHubTestSupport;
        gitTestResource = new GitTestResource();
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
        gitTestResource.createBranch(repositoryName, branchName);
        gitTestResource.checkout(repositoryName, branchName);
    }

    @Override
    public void switchBranch(final String owner, final String repositoryName, final String branchName)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void addFile(final String owner, final String repositoryName, final String filePath, final byte[] content)
    {
        gitTestResource.addFile(repositoryName, filePath, content);
    }

    @Override
    public String commit(final String owner, final String repositoryName, final String message, final String authorName, final String authorEmail)
    {
        return gitTestResource.commit(repositoryName, message, authorName, authorEmail);
    }

    @Override
    public void push(final String owner, final String repositoryName, final String username, final String password)
    {
        gitTestResource.push(repositoryName, username, password);
    }

    @Override
    public void push(final String owner, final String repositoryName, final String username, final String password, final String reference, final boolean newBranch)
    {
        gitTestResource.push(repositoryName, username, password);
    }

    @Override
    public void push(final String owner, final String repositoryName, final String username, final String password, final String reference)
    {
        gitTestResource.push(repositoryName, username, password);
    }

    @Override
    public void createTestLocalRepository(final String owner, final String repositoryName, final String username, final String password)
    {
        gitTestResource.addRepository(repositoryName);

        gitTestResource.clone(repositoryName, gitHubTestSupport.getRepository(owner, repositoryName).getCloneUrl(), username, password);
    }

    @Override
    public void deleteTestRepository(final String repositoryUri)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void deleteAllRepositories()
    {
        gitTestResource.deleteAllRepositories();
    }
}
