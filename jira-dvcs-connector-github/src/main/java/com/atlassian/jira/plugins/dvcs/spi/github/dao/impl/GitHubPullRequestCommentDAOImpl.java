package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO implementation of the {@link GitHubPullRequestCommentDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestCommentDAOImpl implements GitHubPullRequestCommentDAO
{

    /**
     * @see #GitHubPullRequestCommentDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubPullRequestCommentDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubPullRequestCommentMapping}.
     */
    private final GitHubPullRequestCommentMapping gitHubPullRequestCommentDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GitHubPullRequestCommentDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService)
    {
        this.activeObjects = activeObjects;
        this.columnNameResolverService = columnNameResolverService;

        gitHubPullRequestCommentDescription = columnNameResolverService.desc(GitHubPullRequestCommentMapping.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubPullRequestComment gitHubPullRequestComment)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubPullRequestComment.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubPullRequestComment);
                    GitHubPullRequestCommentMapping created = activeObjects.create(GitHubPullRequestCommentMapping.class, params);
                    map(gitHubPullRequestComment, created);

                } else
                {
                    GitHubPullRequestCommentMapping loaded = activeObjects.get(GitHubPullRequestCommentMapping.class,
                            gitHubPullRequestComment.getId());
                    map(loaded, gitHubPullRequestComment);
                    loaded.save();
                    map(gitHubPullRequestComment, loaded);

                }
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubPullRequestComment gitHubPullRequestComment)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubPullRequestCommentMapping toDelete = activeObjects.get(GitHubPullRequestCommentMapping.class,
                        gitHubPullRequestComment.getId());
                activeObjects.delete(toDelete);
                gitHubPullRequestComment.setId(0);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestComment getById(int id)
    {
        GitHubPullRequestCommentMapping loaded = activeObjects.get(GitHubPullRequestCommentMapping.class, id);
        if (loaded == null)
        {
            return null;
        }

        GitHubPullRequestComment result = new GitHubPullRequestComment();
        map(result, loaded);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequestComment getByGitHubId(long gitHubId)
    {
        GitHubPullRequestComment result = new GitHubPullRequestComment();

        Query query = Query.select().where(columnNameResolverService.column(gitHubPullRequestCommentDescription.getGitHubId()) + " = ?",
                gitHubId);
        GitHubPullRequestCommentMapping[] founded = activeObjects.find(GitHubPullRequestCommentMapping.class, query);
        if (founded.length == 1)
        {
            map(result, founded[0]);
            return result;

        } else if (founded.length == 0)
        {
            return null;

        } else
        {
            throw new IllegalStateException("GitHub ID conflict on the pull request comments! GitHub ID: " + gitHubId + " Founded: "
                    + founded.length + " records.");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPullRequestComment> getAll()
    {
        List<GitHubPullRequestComment> result = new LinkedList<GitHubPullRequestComment>();
        for (GitHubPullRequestCommentMapping source : activeObjects.find(GitHubPullRequestCommentMapping.class))
        {
            GitHubPullRequestComment target = new GitHubPullRequestComment();
            map(target, source);
            result.add(target);
        }
        return result;
    }

    // Mapping

    /**
     * Re-maps provided model value into the AO creation map.
     * 
     * @param target
     *            creation map
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubPullRequestComment source)
    {
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());
        GitHubPullRequestMapping pullRequest = activeObjects.get(GitHubPullRequestMapping.class, source.getPullRequest().getId());

        target.put(columnNameResolverService.column(gitHubPullRequestCommentDescription.getGitHubId()), source.getGitHubId());
        target.put(columnNameResolverService.column(gitHubPullRequestCommentDescription.getPullRequest()), pullRequest);
        target.put(columnNameResolverService.column(gitHubPullRequestCommentDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubPullRequestCommentDescription.getCreatedBy()), createdBy);
        target.put(columnNameResolverService.column(gitHubPullRequestCommentDescription.getText()), source.getText());
    }

    /**
     * Re-maps provided model value into the AO value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    private void map(GitHubPullRequestCommentMapping target, GitHubPullRequestComment source)
    {
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());
        GitHubPullRequestMapping pullRequest = activeObjects.get(GitHubPullRequestMapping.class, source.getPullRequest().getId());

        target.setGitHubId(source.getGitHubId());
        target.setPullRequest(pullRequest);
        target.setCreatedBy(createdBy);
        target.setCreatedAt(source.getCreatedAt());
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
    private void map(GitHubPullRequestComment target, GitHubPullRequestCommentMapping source)
    {
        GitHubPullRequest pullRequest = new GitHubPullRequest();
        GitHubPullRequestDAOImpl.map(pullRequest, source.getPullRequest());

        GitHubUser createdBy = new GitHubUser();
        GitHubUserDAOImpl.map(createdBy, source.getCreatedBy());

        target.setId(source.getID());
        target.setGitHubId(source.getGitHubId());
        target.setPullRequest(pullRequest);
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setText(source.getText());
    }
}
