package com.atlassian.jira.plugins.dvcs.spi.github.parsers;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.User;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;

/**
 * Factory for {@link Changeset} implementations
 */
public class GithubChangesetFactory
{

    private GithubChangesetFactory()
    {
    }
    
    public static Changeset transform(RepositoryCommit repositoryCommit, int repositoryId, String branch)
    {
        final List<ChangesetFile> changesetFiles = transformFiles(repositoryCommit.getFiles());

        String name = "";
        String authorEmail = null;

        Date date = Calendar.getInstance().getTime();
        
        if (repositoryCommit.getCommit() != null
                && repositoryCommit.getCommit().getAuthor() != null)
        {
        	
            if (StringUtils.isNotBlank(repositoryCommit.getCommit().getAuthor().getName()))
            {
                name = repositoryCommit.getCommit().getAuthor().getName();
            }
            
            date = repositoryCommit.getCommit().getAuthor().getDate();
            authorEmail = repositoryCommit.getCommit().getAuthor().getEmail();
        }

        // try to get login from Author, if there is no Author try from Commiter
        String login = getUserLogin(repositoryCommit.getAuthor());
        if (StringUtils.isBlank(login))
        {
            login = getUserLogin(repositoryCommit.getCommitter());
        }

        Changeset changeset = new Changeset(
                repositoryId,
                repositoryCommit.getSha(),
                "",
                name,
                login,
                date,
                "", // todo: raw-node. what is it in github?
                branch,
                repositoryCommit.getCommit().getMessage(),
                transformParents(repositoryCommit.getParents()),
                changesetFiles,
                changesetFiles.size()
        );

        changeset.setAuthorEmail(authorEmail);
		return changeset;
        
    }

    private static String getUserLogin(User user)
    {
        if (user!=null && user.getLogin()!=null)
        {
            return user.getLogin();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static List<ChangesetFile> transformFiles(List<CommitFile> files)
    {
        if (files == null)
        {
            return Collections.<ChangesetFile>emptyList();
        }

        return (List<ChangesetFile>) CollectionUtils.collect(files, new Transformer()
        {
            @Override
            public Object transform(Object input)
            {
                CommitFile commitFile = (CommitFile) input;
                
                String filename = commitFile.getFilename();
                String status = commitFile.getStatus();
                int additions = commitFile.getAdditions();
                int deletions = commitFile.getDeletions();

                return new ChangesetFile(CustomStringUtils.getChangesetFileAction(status),
                        filename, additions, deletions);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static List<String> transformParents(List<Commit> parents)
    {
        if (parents == null)
        {
            return Collections.<String>emptyList();
        }


        return (List<String>) CollectionUtils.collect(parents, new Transformer()
        {
            @Override
            public Object transform(Object input)
            {
                Commit commit = (Commit) input;
                
                return commit.getSha();
            }
        });
    }
    
}
