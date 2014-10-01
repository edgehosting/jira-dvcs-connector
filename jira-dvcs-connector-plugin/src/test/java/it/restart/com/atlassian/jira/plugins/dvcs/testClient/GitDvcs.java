package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
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
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract, common implementation for all GitHub tests.
 * 
 * @author Miroslav Stencel
 * 
 */
public class GitDvcs implements Dvcs
{
    /**
     * Map between test repository URI and local directory of this repository.
     */
    private Map<String, Git> uriToLocalRepository = new HashMap<String, Git>();

    /**
     * @param repositoryUri
     *            e.g.: owner/name
     * @return local repository for provided repository uri
     */
    protected Git getLocalRepository(String repositoryUri)
    {
        return uriToLocalRepository.get(repositoryUri);
    }

    /**
     * Clones repository, which is defined by provided repository URI. Clone URL will be obtained from {@link #uriToRemoteRepository}.
     * Useful for {@link #fork(String)} repository.
     * 
     * @param repositoryUri
     *            e.g.: owner/name
     * @param username
     *            for get access to clone
     * @param password
     *            for get access to clone
     */
    private void clone(String owner, String repositoryName, String username, String password)
    {
        try
        {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            cloneCommand.setURI(generateCloneUrl(owner, repositoryName));
            cloneCommand.setDirectory(getLocalRepository(getUriKey(owner, repositoryName)).getRepository().getDirectory().getParentFile());
            cloneCommand.call();

        } catch (InvalidRemoteException e)
        {
            throw new RuntimeException(e);

        } catch (TransportException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }


    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#createBranch(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void createBranch(String owner, String repositoryName, String branchName)
    {
        try
        {
            String repositoryUri = getUriKey(owner, repositoryName);
            CreateBranchCommand createBranchCommand = getLocalRepository(repositoryUri).branchCreate();
            createBranchCommand.setName(branchName);
            createBranchCommand.call();
            checkout(repositoryUri, branchName);
        } catch (RefAlreadyExistsException e)
        {
            throw new RuntimeException(e);

        } catch (RefNotFoundException e)
        {
            throw new RuntimeException(e);

        } catch (InvalidRefNameException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }

    public void switchBranch(String owner, String repositoryName, String branchName)
    {
        String repositoryUri = getUriKey(owner, repositoryName);
        checkout(repositoryUri, branchName);
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#addFile(java.lang.String, java.lang.String, java.lang.String, byte[])
     */
    @Override
    public void addFile(String owner, String repositoryName, String filePath, byte[] content)
    {
        Git git = getLocalRepository(getUriKey(owner, repositoryName));

        File targetFile = new File(git.getRepository().getDirectory().getParentFile(), filePath);
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

        try
        {
            AddCommand addCommand = git.add();
            addCommand.addFilepattern(filePath);
            addCommand.call();
        } catch (NoFilepatternException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#commit(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String commit(String owner, String repositoryName, String message, String authorName, String authorEmail)
    {
        try
        {
            CommitCommand commitCommand = getLocalRepository(getUriKey(owner, repositoryName)).commit();
            commitCommand.setMessage(message);
            commitCommand.setAuthor(authorName, authorEmail);
            RevCommit commit = commitCommand.call();
            return commit.getId().getName();
        } catch (NoHeadException e)
        {
            throw new RuntimeException(e);

        } catch (NoMessageException e)
        {
            throw new RuntimeException(e);

        } catch (UnmergedPathsException e)
        {
            throw new RuntimeException(e);

        } catch (ConcurrentRefUpdateException e)
        {
            throw new RuntimeException(e);

        } catch (WrongRepositoryStateException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }

    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#push(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void push(String owner, String repositoryName, String username, String password)
    {
        try
        {
            PushCommand pushCommand = getLocalRepository(getUriKey(owner, repositoryName)).push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            pushCommand.call();
        } catch (InvalidRemoteException e)
        {
            throw new RuntimeException(e);

        } catch (TransportException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#push(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void push(String owner, String repositoryName, String username, String password, String reference, boolean newBranch)
    {
        try
        {
            Git localRepository = getLocalRepository(getUriKey(owner, repositoryName));
            PushCommand pushCommand = localRepository.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
            pushCommand.setRefSpecs(new RefSpec(localRepository.getRepository().getRef(reference).getName()));
            pushCommand.call();

        } catch (InvalidRemoteException e)
        {
            throw new RuntimeException(e);

        } catch (TransportException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        } catch (IOException e)
        {
            throw new RuntimeException(e);
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
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#createTestLocalRepository(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void createTestLocalRepository(String owner, String repositoryName, String username, String password)
    {
        try
        {
            Repository localRepository = new FileRepository(Files.createTempDir() + "/.git");
            String repositoryUri = getUriKey(owner, repositoryName);
            uriToLocalRepository.put(repositoryUri, new Git(localRepository));
            clone(owner, repositoryName, username, password);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs#deleteTestRepository(java.lang.String)
     */
    @Override
    public void deleteTestRepository(String repositoryUri)
    {
        Git localRepository = uriToLocalRepository.remove(repositoryUri);
        if (localRepository != null)
        {
            localRepository.getRepository().close();
            try
            {
                FileUtils.deleteDirectory(localRepository.getRepository().getDirectory().getParentFile());

            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void deleteAllRepositories()
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    private String getUriKey(String owner, String slug)
    {
        return owner + "/" + slug;
    }
    
    /**
     * Checkout on provided repository - git checkout.
     * 
     * @param repositoryUri
     *            over which repository e.g.: owner/name
     * @param name
     *            name to checkout e.g.: branch name
     */
    private void checkout(String repositoryUri, String name)
    {
        try
        {
            CheckoutCommand checkoutCommand = getLocalRepository(repositoryUri).checkout();
            checkoutCommand.setName(name);
            checkoutCommand.call();
        } catch (RefAlreadyExistsException e)
        {
            throw new RuntimeException(e);

        } catch (RefNotFoundException e)
        {
            throw new RuntimeException(e);

        } catch (InvalidRefNameException e)
        {
            throw new RuntimeException(e);

        } catch (CheckoutConflictException e)
        {
            throw new RuntimeException(e);

        } catch (GitAPIException e)
        {
            throw new RuntimeException(e);

        }
    }
    
    private String generateCloneUrl(String owner, String repositorySlug)
    {
        return String.format("https://%s@bitbucket.org/%s/%s.git", owner, owner, repositorySlug);
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
}
