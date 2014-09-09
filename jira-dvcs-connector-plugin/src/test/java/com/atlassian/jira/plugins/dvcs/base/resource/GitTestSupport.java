package com.atlassian.jira.plugins.dvcs.base.resource;

import com.atlassian.jira.plugins.dvcs.base.AbstractTestListener;
import com.atlassian.jira.plugins.dvcs.base.TestListenerDelegate;
import com.google.common.io.Files;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.api.errors.UnmergedPathsException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GitTestSupport
{

    private final Map<String, RepositoryContext> repositoryByName = new HashMap<String, RepositoryContext>();

    private final class RepositoryContext
    {
        private final Repository repository;
        private final Git git;

        public RepositoryContext(Repository repository)
        {
            this.repository = repository;
            this.git = new Git(repository);
        }

    }

    public GitTestSupport()
    {
    }

    public GitTestSupport(TestListenerDelegate listenerDelegate)
    {
        listenerDelegate.register(new AbstractTestListener()
        {

        });
    }

    public void addRepository(String name)
    {
        try
        {
            Repository repository = new FileRepository(Files.createTempDir() + "/.git");
            repositoryByName.put(name, new RepositoryContext(repository));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes repository - git init.
     *
     * @param repositoryName for which repository
     * @param cloneUrl of original repository
     */
    public void init(String repositoryName, String cloneUrl)
    {
        RepositoryContext repositoryContext = repositoryByName.get(repositoryName);

        try
        {
            InitCommand initCommand = Git.init();
            initCommand.setDirectory(repositoryContext.repository.getDirectory().getParentFile());
            initCommand.call();
        }
        catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }

        try
        {
            StoredConfig config = repositoryContext.repository.getConfig();
            config.setString("remote", "origin", "url", cloneUrl);
            config.save();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Clones repository, which is defined by provided repository URI. Clone URL will be obtained from {@link
     * #uriToRemoteRepository}. Useful for {@link #fork(String)} repository.
     *
     * @param repositoryName into which repository
     * @param cloneUrl url of cloned repository
     * @param username for get access to clone
     * @param password for get access to clone
     */
    public void clone(String repositoryName, String cloneUrl, String username, String password)
    {
        RepositoryContext repositoryContext = repositoryByName.get(repositoryName);

        try
        {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            cloneCommand.setURI(cloneUrl);
            cloneCommand.setDirectory(repositoryContext.repository.getDirectory().getParentFile());
            cloneCommand.call();

        }
        catch (InvalidRemoteException e)
        {
            throw new RuntimeException(e);
        }
        catch (TransportException e)
        {
            throw new RuntimeException(e);
        }
        catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates and adds file to repository - git add.
     *
     * @param repositoryName for which repository
     * @param filePath repository relative path of file, parents directories will be automatically created
     * @param content of new file
     */
    public void addFile(String repositoryName, String filePath, byte[] content)
    {
        RepositoryContext repositoryContext = repositoryByName.get(repositoryName);

        File targetFile = new File(repositoryContext.git.getRepository().getDirectory().getParentFile(), filePath);
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

        try
        {
            AddCommand addCommand = repositoryContext.git.add();
            addCommand.addFilepattern(filePath);
            addCommand.call();
        }
        catch (NoFilepatternException e)
        {
            throw new RuntimeException(e);
        }
        catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Commits current changes.
     *
     * @param repositoryName for which repository
     * @param message commit message
     * @param authorName name of author
     * @param authorEmail email of author
     * @return SHA-1 commit id
     */
    public String commit(String repositoryName, String message, String authorName, String authorEmail)
    {
        RepositoryContext repositoryContext = repositoryByName.get(repositoryName);

        try
        {
            CommitCommand commitCommand = repositoryContext.git.commit();
            commitCommand.setMessage(message);
            commitCommand.setAuthor(authorName, authorEmail);
            RevCommit commit = commitCommand.call();
            return commit.getId().getName();
        }
        catch (NoHeadException e)
        {
            throw new RuntimeException(e);
        }
        catch (NoMessageException e)
        {
            throw new RuntimeException(e);
        }
        catch (UnmergedPathsException e)
        {
            throw new RuntimeException(e);
        }
        catch (ConcurrentRefUpdateException e)
        {
            throw new RuntimeException(e);
        }
        catch (WrongRepositoryStateException e)
        {
            throw new RuntimeException(e);
        }
        catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Push current state to remote repository.
     *
     * @param repositoryName for which repository
     * @param username committer username
     * @param password committer password
     */
    public void push(String repositoryName, String username, String password)
    {
        RepositoryContext repositoryContext = repositoryByName.get(repositoryName);

        try
        {
            PushCommand pushCommand = repositoryContext.git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            pushCommand.call();
        }
        catch (InvalidRemoteException e)
        {
            throw new RuntimeException(e);
        }
        catch (TransportException e)
        {
            throw new RuntimeException(e);
        }
        catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Push current state to remote repository.
     *
     * @param repositoryName for which repository
     * @param username committer username
     * @param password committer password
     * @param reference e.g.: name of branch
     */
    public void push(String repositoryName, String username, String password, String reference)
    {
        RepositoryContext repositoryContext = repositoryByName.get(repositoryName);

        try
        {
            PushCommand pushCommand = repositoryContext.git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            pushCommand.setRefSpecs(new RefSpec(repositoryContext.git.getRepository().getRef(reference).getName()));
            pushCommand.call();

        }
        catch (InvalidRemoteException e)
        {
            throw new RuntimeException(e);
        }
        catch (TransportException e)
        {
            throw new RuntimeException(e);
        }
        catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates branch on provided repository - git branch name
     *
     * @param repositoryName for which repository
     * @param name of branch
     */
    public void createBranch(String repositoryName, String name)
    {
        RepositoryContext repositoryContext = repositoryByName.get(repositoryName);

        try
        {
            CreateBranchCommand createBranchCommand = repositoryContext.git.branchCreate();
            createBranchCommand.setName(name);
            createBranchCommand.call();
        }
        catch (RefAlreadyExistsException e)
        {
            throw new RuntimeException(e);
        }
        catch (RefNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvalidRefNameException e)
        {
            throw new RuntimeException(e);
        }
        catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checkout on provided repository - git checkout.
     *
     * @param repositoryUri over which repository
     * @param name name to checkout e.g.: branch name
     */
    public void checkout(String repositoryName, String name)
    {
        RepositoryContext repositoryContext = repositoryByName.get(repositoryName);

        try
        {
            CheckoutCommand checkoutCommand = repositoryContext.git.checkout();
            checkoutCommand.setName(name);
            checkoutCommand.call();
        }
        catch (RefAlreadyExistsException e)
        {
            throw new RuntimeException(e);
        }
        catch (RefNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvalidRefNameException e)
        {
            throw new RuntimeException(e);
        }
        catch (CheckoutConflictException e)
        {
            throw new RuntimeException(e);
        }
        catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void deleteAllRepositories()
    {
        for (RepositoryContext repositoryContext : repositoryByName.values())
        {
            repositoryContext.repository.close();
            repositoryContext.repository.getDirectory().delete();
        }
        repositoryByName.clear();
    }
}
