package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

/**
 * Interface that represents a Dvcs that we are testing, basically enables common operations on Dvcs repositories such
 * as commit, clone, branch and merge.
 *
 * This Interface attempts to cover up some of the differences between our Dvcs repositories and their remote
 * origins. Some of the semantics of the underlying Dvcs and host will make this difficult but it is mostly ok
 * for testing.
 *
 * Generally the way this class should be used is to checkout a repository then switch to a branch. Subsequent
 * operations should be performed against this branch although this is a bit implementation dependent. When used
 * for Pull Request tests try to use {@link #push(String, String, String, String, String)} as it will try to
 * use the specified branch.
 *
 * Usually Git implementations will 'remember' what branch they are on and mercurial should be explicitly told the
 * branch to use.
 */
public interface Dvcs
{
    /**
     * Returns Dvcs type. 'hg' for Mercurial and 'git' for Git
     * 
     * @return Dvcs type
     */
    public String getDvcsType();
    
    /**
     * Default branch name. 'default' for Mercurial and 'master' for Git.
     * 
     * @return default branch name
     */
    public String getDefaultBranchName();
    
    /**
     * Creates branch on provided repository - git branch name
     * 
     * @param owner
     * @param repositoryName
     * @param branchName
     */
    public void createBranch(String owner, String repositoryName, String branchName);

    /**
     * Switches branch on provided repository - checkout for git, update for Mercurial
     *
     * @param owner
     * @param repositoryName
     * @param branchName
     */
    public void switchBranch(String owner, String repositoryName, String branchName);

    public void addFile(String owner, String repositoryName, String filePath, byte[] content);

    public String commit(String owner, String repositoryName, String message, String authorName, String authorEmail);

    public void push(String owner, String repositoryName, String username, String password);

    public void push(String owner, String repositoryName, String username, String password, String reference, boolean newBranch);

    public void push(String owner, String repositoryName, String username, String password, String reference);

    /**
     * Creates provided test repository - local side.
     * 
     * @param owner
     * @param repositoryName
     */
    public void createTestLocalRepository(String owner, String repositoryName, String username, String password);

    public void deleteTestRepository(String repositoryUri);

    /**
     * Delete all local copies of repositories that this DVCS client knows about
     */
    void deleteAllRepositories();
}