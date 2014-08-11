package it.restart.com.atlassian.jira.plugins.dvcs.testClient;


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

    /**
     * Creates and adds file to repository - git add.
     * 
     * @param repositoryUri
     *            for which repository
     * @param filePath
     *            repository relative path of file, parents directories will be automatically created
     * @param content
     *            of new file
     */
    public void addFile(String owner, String repositoryName, String filePath, byte[] content);

    /**
     * Commits current changes.
     * 
     * @param repositoryUri
     *            for which repository
     * @param message
     *            commit message
     * 
     * @param authorName
     *            name of author
     * @param authorEmail
     *            email of author
     * @return SHA-1 commit id
     */
    public String commit(String owner, String repositoryName, String message, String authorName, String authorEmail);

    /**
     * Push current state to remote repository.
     * 
     * @param repositoryUri
     *            e.g. owner/name
     * @param username
     *            committer username
     * @param password
     *            committer password
     */
    public void push(String owner, String repositoryName, String username, String password);

    /**
     * Push current state to remote repository.
     * 
     * @param repositoryUri
     *            e.g. owner/name
     * @param username
     *            committer username
     * @param password
     *            committer password
     * @param reference
     *            e.g.: name of branch
     * @param newBranch
     *               whether new branch is being pushed
     */
    public void push(String owner, String repositoryName, String username, String password, String reference, boolean newBranch);

    /**
     * Push current state to remote repository.
     * 
     * @param repositoryUri
     *            e.g. owner/name
     * @param username
     *            committer username
     * @param password
     *            committer password
     * @param reference
     *            e.g.: name of branch
     */
    public void push(String owner, String repositoryName, String username, String password, String reference);

    /**
     * Creates provided test repository - local side.
     * 
     * @param owner
     * @param repositoryName
     */
    public void createTestLocalRepository(String owner, String repositoryName, String username, String password);

    public void deleteTestRepository(String repositoryUri);
}