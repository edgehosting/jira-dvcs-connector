package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestLineCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO implementation of the {@link GitHubPullRequestLineCommentDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestLineCommentDAOImpl implements GitHubPullRequestLineCommentDAO
{

    /**
     * @see #GitHubPullRequestLineCommentDAOImpl(ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     */
    public GitHubPullRequestLineCommentDAOImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubPullRequestLineComment gitHubPullRequestLineComment)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubPullRequestLineComment.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubPullRequestLineComment);
                    GitHubPullRequestLineCommentMapping created = activeObjects.create(GitHubPullRequestLineCommentMapping.class, params);
                    map(gitHubPullRequestLineComment, created);

                } else
                {
                    GitHubPullRequestLineCommentMapping loaded = activeObjects.get(GitHubPullRequestLineCommentMapping.class,
                            gitHubPullRequestLineComment.getId());
                    map(loaded, gitHubPullRequestLineComment);
                    loaded.save();
                    map(gitHubPullRequestLineComment, loaded);

                }
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubPullRequestLineComment gitHubPullRequestLineComment)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubPullRequestLineCommentMapping toDelete = activeObjects.get(GitHubPullRequestLineCommentMapping.class,
                        gitHubPullRequestLineComment.getId());
                activeObjects.delete(toDelete);
                gitHubPullRequestLineComment.setId(0);

                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestLineComment getById(int id)
    {
        GitHubPullRequestLineCommentMapping loaded = activeObjects.get(GitHubPullRequestLineCommentMapping.class, id);
        if (loaded == null)
        {
            return null;
        }

        GitHubPullRequestLineComment result = new GitHubPullRequestLineComment();
        map(result, loaded);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestLineComment getByGitHubId(long gitHubId)
    {
        Query query = Query.select().where(GitHubPullRequestLineCommentMapping.COLUMN_GIT_HUB_ID + " = ? ", gitHubId);
        GitHubPullRequestLineCommentMapping[] founded = activeObjects.find(GitHubPullRequestLineCommentMapping.class, query);

        if (founded.length == 1)
        {
            GitHubPullRequestLineComment result = new GitHubPullRequestLineComment();
            map(result, founded[0]);
            return result;

        } else if (founded.length == 0)
        {
            return null;

        } else
        {
            throw new IllegalStateException("GitHub ID conflict on comments! GitHub ID: " + gitHubId + " Founded: " + founded.length
                    + " records.");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPullRequestLineComment> getAll()
    {
        List<GitHubPullRequestLineComment> result = new LinkedList<GitHubPullRequestLineComment>();
        for (GitHubPullRequestLineCommentMapping source : activeObjects.get(GitHubPullRequestLineCommentMapping.class))
        {
            GitHubPullRequestLineComment target = new GitHubPullRequestLineComment();
            map(target, source);
            result.add(target);
        }
        return result;
    }

    /**
     * Re-maps provided model value into the AO creation map.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubPullRequestLineComment source)
    {
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());
        GitHubPullRequestMapping pullRequest = activeObjects.get(GitHubPullRequestMapping.class, source.getPullRequest().getId());
        GitHubCommitMapping commit = activeObjects.get(GitHubCommitMapping.class, source.getCommit().getId());

        target.put(GitHubPullRequestLineCommentMapping.COLUMN_GIT_HUB_ID, source.getGitHubId());
        target.put(GitHubPullRequestLineCommentMapping.COLUMN_CREATED_AT, source.getCreatedAt());
        target.put(GitHubPullRequestLineCommentMapping.COLUMN_CREATED_BY, createdBy);
        target.put(GitHubPullRequestLineCommentMapping.COLUMN_PULL_REQUEST, pullRequest);
        target.put(GitHubPullRequestLineCommentMapping.COLUMN_COMMIT, commit);
        target.put(GitHubPullRequestLineCommentMapping.COLUMN_PATH, source.getPath());
        target.put(GitHubPullRequestLineCommentMapping.COLUMN_LINE, source.getLine());
        target.put(GitHubPullRequestLineCommentMapping.COLUMN_TEXT, source.getText());
    }

    /**
     * Re-maps provided model value into the AO value.
     * 
     * @param target
     *            AO value
     * @param sourced
     *            model value
     */
    private void map(GitHubPullRequestLineCommentMapping target, GitHubPullRequestLineComment source)
    {
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());
        GitHubPullRequestMapping pullRequest = activeObjects.get(GitHubPullRequestMapping.class, source.getPullRequest().getId());
        GitHubCommitMapping commit = activeObjects.get(GitHubCommitMapping.class, source.getCommit().getId());

        target.setGitHubId(source.getGitHubId());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setPullRequest(pullRequest);
        target.setCommit(commit);
        target.setPath(source.getPath());
        target.setLine(source.getLine());
        target.setText(source.getText());
    }

    /**
     * Re-maps provided AO value into the model value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    private void map(GitHubPullRequestLineComment target, GitHubPullRequestLineCommentMapping source)
    {
        GitHubUser createdBy = new GitHubUser();
        GitHubUserDAOImpl.map(createdBy, source.getCreatedBy());

        GitHubPullRequest pullRequest = new GitHubPullRequest();
        GitHubPullRequestDAOImpl.map(pullRequest, source.getPullRequest());

        GitHubCommit commit = new GitHubCommit();
        GitHubCommitDAOImpl.map(commit, source.getCommit());

        target.setId(source.getID());
        target.setGitHubId(source.getGitHubId());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setPullRequest(pullRequest);
        target.setCommit(commit);
        target.setPath(source.getPath());
        target.setLine(source.getLine());
        target.setText(source.getPath());
    }

}
