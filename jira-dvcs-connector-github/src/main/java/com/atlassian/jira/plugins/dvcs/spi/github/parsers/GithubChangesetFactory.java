package com.atlassian.jira.plugins.dvcs.spi.github.parsers;

import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.util.CustomStringUtils;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

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

        return new Changeset(
                repositoryId,
                repositoryCommit.getSha(),
                "",
                repositoryCommit.getCommit().getAuthor().getName(),
                repositoryCommit.getAuthor().getLogin(),
                repositoryCommit.getCommit().getAuthor().getDate(),
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
