package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.Repository;
import com.aragost.javahg.RepositoryConfiguration;
import com.aragost.javahg.commands.AddCommand;
import com.aragost.javahg.commands.BranchCommand;
import com.aragost.javahg.commands.CommitCommand;
import com.aragost.javahg.commands.PushCommand;
import com.aragost.javahg.commands.UpdateCommand;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract, common implementation for all GitHub tests.
 *
 * @author Miroslav Stencel
 */
public class MercurialDvcs implements Dvcs
{
    /**
     * Map between test repository URI and local directory of this repository.
     */
    private Map<String, Repository> uriToLocalRepository = new HashMap<String, Repository>();

    /**
     * Retrieve the local repo for that name from the local map of repositories that this instance knows about
     */
    private Repository getLocalRepository(String owner, String repositoryName)
    {
        return uriToLocalRepository.get(getUriKey(owner, repositoryName));
    }

    /**
     * Clones repository
     *
     * @param cloneUrl returns local repository
     */
    private Repository clone(String cloneUrl)
    {
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        configureHgBin(repositoryConfiguration);
        configureHgRc(repositoryConfiguration);

        return Repository.clone(repositoryConfiguration, Files.createTempDir(), cloneUrl);
    }


    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#createBranch(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void createBranch(String owner, String repositoryName, String branchName)
    {
        BranchCommand.on(getLocalRepository(owner, repositoryName)).set(branchName);
    }

    @Override
    public void switchBranch(String owner, String repositoryName, String branchName)
    {
        try
        {
            UpdateCommand.on(getLocalRepository(owner, repositoryName)).rev(branchName).execute();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#addFile(java.lang.String, java.lang.String, java.lang.String, byte[])
     */
    @Override
    public void addFile(String owner, String repositoryName, String filePath, byte[] content)
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
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        AddCommand.on(repository).execute(filePath);
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#commit(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String commit(String owner, String repositoryName, String message, String authorName, String authorEmail)
    {
        CommitCommand commitCommand = CommitCommand.on(getLocalRepository(owner, repositoryName));
        commitCommand.message(message);
        commitCommand.user(String.format("%s <%s>", authorName, authorEmail));
        Changeset changeset = commitCommand.execute();

        return changeset.getNode();
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#push(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void push(String owner, String repositoryName, String username, String password)
    {
        push(owner, repositoryName, username, password, null, false);
    }

    private String generateCloneUrl(String owner, String repositorySlug, String username, String password)
    {
        return String.format("https://%s:%s@bitbucket.org/%s/%s", username, password, owner, repositorySlug);
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#push(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void push(String owner, String repositoryName, String username, String password, String reference, boolean newBranch)
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
        }
        catch (IOException e)
        {
            new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#push(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void push(String owner, String repositoryName, String username, String password, String reference)
    {
        push(owner, repositoryName, username, password, reference, false);
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#createTestLocalRepository(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void createTestLocalRepository(String owner, String repositoryName, String username, String password)
    {
        Repository localRepository = clone(generateCloneUrl(owner, repositoryName, username, password));
        uriToLocalRepository.put(getUriKey(owner, repositoryName), localRepository);
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#deleteTestRepository(java.lang.String)
     */
    @Override
    public void deleteTestRepository(String repositoryUri)
    {
        Repository localRepository = uriToLocalRepository.remove(repositoryUri);
        if (localRepository != null)
        {
            localRepository.close();
            try
            {
                FileUtils.deleteDirectory(localRepository.getDirectory());

            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void deleteAllRepositories()
    {
        for (Repository repository : uriToLocalRepository.values())
        {
            repository.close();
            try
            {
                FileUtils.deleteDirectory(repository.getDirectory());

            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void configureHgBin(RepositoryConfiguration repositoryConfiguration)
    {
        Process process;
        try
        {
            process = new ProcessBuilder(repositoryConfiguration.getHgBin(), "--version").start();
            process.waitFor();
        }
        catch (Exception e)
        {
            repositoryConfiguration.setHgBin("/usr/local/bin/hg");
        }
    }

    /**
     * The Bitbucket SHA fingerprint appears to be a bit messed up, we create a temp file to use as the .hgrc and put
     * the Bitbucket fingerprint in it
     */
    private void configureHgRc(RepositoryConfiguration repositoryConfiguration)
    {
        File tempDir = Files.createTempDir();
        BufferedWriter writer = null;
        try
        {
            File hgRc = new File(tempDir.getCanonicalPath() + ".hgrc");
            writer = new BufferedWriter(new FileWriter(hgRc));
            writer.write("[hostfingerprints]");
            writer.newLine();
            writer.write("bitbucket.org = 45:ad:ae:1a:cf:0e:73:47:06:07:e0:88:f5:cc:10:e5:fa:1c:f7:99");
            writer.flush();

            final String hgRcPath = hgRc.getCanonicalPath();
            repositoryConfiguration.setHgrcPath(hgRcPath);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (writer != null)
            {
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    private String getUriKey(String owner, String slug)
    {
        return owner + "/" + slug;
    }

    @Override
    public String getDvcsType()
    {
        return RepositoryRemoteRestpoint.ScmType.HG;
    }

    @Override
    public String getDefaultBranchName()
    {
        return "default";
    }
}
