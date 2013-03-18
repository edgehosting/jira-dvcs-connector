package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.Repository;
import com.aragost.javahg.RepositoryConfiguration;
import com.aragost.javahg.commands.AddCommand;
import com.aragost.javahg.commands.BranchCommand;
import com.aragost.javahg.commands.CommitCommand;
import com.aragost.javahg.commands.PushCommand;
import com.google.common.io.Files;

/**
 * Abstract, common implementation for Bitbucket tests.
 * 
 * @author Miroslav Stencel <mstencel@atlassian.com>
 * 
 */
public abstract class AbstractMercurialDVCSTest extends AbstractDVCSTest
{
    /**
     * Map between test repository URI and local directory of this repository.
     */
    private Map<String, Repository> uriToLocalRepository = new HashMap<String, Repository>();

    /**
     * Cleans common test environment.
     */
    @AfterClass(alwaysRun = true)
    public void onTestsEnvironmentCleanUp()
    {
        // log out from bitbucket
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketLoginPage.class).doLogout();
    }

    /**
     * @param repositoryUri
     *            e.g.: owner/name
     * @return local repository for provided repository uri
     */
    protected Repository getLocalRepository(String owner, String repositoryName)
    {
        return uriToLocalRepository.get(getUriKey(owner, repositoryName));
    }

    /**
     * Clones repository
     * 
     * @param cloneUrl
     * 
     * returns local repository
     */
    protected Repository clone(String cloneUrl)
    {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        configureHgBin(repositoryConfiguration);
        
        return Repository.clone(repositoryConfiguration, Files.createTempDir(), cloneUrl);
    }

    
    /**
     * Creates branch on provided repository - hg branch name
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     * @param name
     *            of branch
     */
    protected void createBranch(String owner, String repositoryName, String branchName)
    {
        BranchCommand.on(getLocalRepository(owner, repositoryName)).set(branchName);
    }

    /**
     * Creates and adds file to repository - hg add.
     * 
     * @param repositoryUri
     *            for which repository
     * @param filePath
     *            repository relative path of file, parents directories will be automatically created
     * @param content
     *            of new file
     */
    protected void addFile(String owner, String repositoryName, String filePath, byte[] content)
    {
        Repository repository = getLocalRepository(owner, repositoryName);

        File targetFile = new File(repository.getDirectory(), filePath);
        targetFile.getParentFile().mkdirs();
        try
        {
            targetFile.createNewFile();
            FileOutputStream output = new FileOutputStream(targetFile);
            output.write(content);
            output.close();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        AddCommand.on(repository).execute(filePath);
    }

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
    protected String commit(String owner, String repositoryName, String message, String authorName, String authorEmail)
    {
        CommitCommand commitCommand = CommitCommand.on(getLocalRepository(owner, repositoryName));
        commitCommand.message(message);
        commitCommand.user(String.format("%s <%s>", authorName, authorEmail));
        Changeset changeset = commitCommand.execute();
        
        return changeset.getNode();
    }

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
    protected void push(String owner, String repositoryName, String username, String password)
    {
        push(owner, repositoryName, username, password, null, false);
    }
    
    protected String generateCloneUrl(String owner, String repositorySlug, String username, String password)
    {
        return String.format("https://%s:%s@bitbucket.org/%s/%s", username, password, owner, repositorySlug);
    }

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
    protected void push(String owner, String repositoryName, String username, String password, String reference, boolean newBranch)
    {
        PushCommand pushCommand = PushCommand.on(getLocalRepository(owner, repositoryName));
        if (newBranch)
        {
            pushCommand.newBranch();
        }
        
        if (reference != null)
        {
            pushCommand.branch(reference);
        }
        
        try
        {
            pushCommand.execute(generateCloneUrl(owner, repositoryName, username, password));
        } catch (IOException e)
        {
            new RuntimeException(e);
        }
    }

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
    protected void push(String owner, String repositoryName, String username, String password, String reference)
    {
        push(owner, repositoryName, username, password, reference, false);
    }
    
    /**
     * Creates provided test repository - local side.
     * 
     * @param owner
     * @param repositoryName
     */
    protected void createTestLocalRepository(String owner, String repositoryName, String cloneUrl)
    {
        Repository localRepository = clone(cloneUrl);
        uriToLocalRepository.put(owner + "/" + repositoryName, localRepository);
    }
    
    protected void deleteTestRepository(String repositoryUri)
    {
        Repository localRepository = uriToLocalRepository.remove(repositoryUri);
        if (localRepository != null)
        {
            localRepository.close();
            try
            {
                FileUtils.deleteDirectory(localRepository.getDirectory());

            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
    
    protected String getUriKey(String owner, String slug)
    {
        return owner + "/" + slug;
    }
    
    private void configureHgBin(RepositoryConfiguration repositoryConfiguration)
    {
        Process process;
        try
        {
            process = new ProcessBuilder(repositoryConfiguration.getHgBin(), "--version").start();
            process.waitFor();
        } catch (Exception e)
        {
            repositoryConfiguration.setHgBin("/usr/local/bin/hg");
        }
    }
}
