package com.atlassian.jira.plugins.bitbucket.spi.github;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultBitbucketChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultChangeset;
import com.atlassian.jira.plugins.bitbucket.api.util.CustomStringUtils;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Factory for {@link com.atlassian.jira.plugins.bitbucket.api.Changeset} implementations
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
        Date date = Calendar.getInstance().getTime();
        if (repositoryCommit.getCommit() != null
                && repositoryCommit.getCommit().getAuthor() != null)
        {
            if (StringUtils.isBlank(repositoryCommit.getCommit().getAuthor().getName()))
            {
                name = repositoryCommit.getCommit().getAuthor().getName();
            }
            date = repositoryCommit.getCommit().getAuthor().getDate();
        }

        String login = "";
        if (repositoryCommit.getAuthor() != null
                && StringUtils.isNotBlank(repositoryCommit.getAuthor().getLogin()))
        {
            login = repositoryCommit.getAuthor().getLogin();
        }

        return new DefaultChangeset(
                repositoryId,
                repositoryCommit.getSha(),
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

                return new DefaultBitbucketChangesetFile(CustomStringUtils.getChangesetFileAction(status),
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
