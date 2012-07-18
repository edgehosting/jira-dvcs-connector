package com.atlassian.jira.plugins.dvcs.spi.github.parsers;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.User;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

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

    private static List<ChangesetFile> transformFiles(List<CommitFile> files)
    {
        if (files == null)
        {
            return Collections.<ChangesetFile>emptyList();
        }

        return Lists.transform(files, new Function<CommitFile, ChangesetFile>()
        {
            @Override
            public ChangesetFile apply(@Nullable CommitFile commitFile)
            {
                String filename = commitFile.getFilename();
                String status = commitFile.getStatus();
                int additions = commitFile.getAdditions();
                int deletions = commitFile.getDeletions();

                return new ChangesetFile(CustomStringUtils.getChangesetFileAction(status),
                        filename, additions, deletions);
            }
        });
    }

    private static List<String> transformParents(List<Commit> parents)
    {
        if (parents == null)
        {
            return Collections.<String>emptyList();
        }


        return Lists.transform(parents, new Function<Commit, String>()
        {
            @Override
            public String apply(@Nullable Commit commit)
            {
                return commit.getSha();
            }
        });
    }
    
}
