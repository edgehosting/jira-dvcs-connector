package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestLineCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
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
     * @see #GitHubPullRequestLineCommentDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubPullRequestLineCommentDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubPullRequestLineCommentMapping}
     */
    private final GitHubPullRequestLineCommentMapping gitHubPullRequestLineCommentMappingDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GitHubPullRequestLineCommentDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService)
    {
        this.activeObjects = activeObjects;

        this.columnNameResolverService = columnNameResolverService;
        this.gitHubPullRequestLineCommentMappingDescription = columnNameResolverService.desc(GitHubPullRequestLineCommentMapping.class);
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
        Query query = Query.select().where(
                columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getGitHubId()) + " = ? ", gitHubId);
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
    public List<GitHubPullRequestLineComment> getByPullRequest(GitHubPullRequest pullRequest)
    {
        Query query = Query.select().where(
                columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getDomain()) + " = ? AND "
                        + columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getPullRequest()) + " = ? ",
                pullRequest.getDomain().getId(), pullRequest.getId());

        List<GitHubPullRequestLineComment> result = new LinkedList<GitHubPullRequestLineComment>();
        for (GitHubPullRequestLineCommentMapping source : activeObjects.find(GitHubPullRequestLineCommentMapping.class, query))
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
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());
        GitHubPullRequestMapping pullRequest = activeObjects.get(GitHubPullRequestMapping.class, source.getPullRequest().getId());
        GitHubCommitMapping commit = activeObjects.get(GitHubCommitMapping.class, source.getCommit().getId());

        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getDomain()), domain);
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getGitHubId()), source.getGitHubId());
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getCreatedBy()), createdBy);
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getPullRequest()), pullRequest);
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getUrl()), source.getUrl());
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getHtmlUrl()), source.getHtmlUrl());
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getCommit()), commit);
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getPath()), source.getPath());
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getLine()), source.getLine());
        target.put(columnNameResolverService.column(gitHubPullRequestLineCommentMappingDescription.getText()), source.getText());
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
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());
        GitHubPullRequestMapping pullRequest = activeObjects.get(GitHubPullRequestMapping.class, source.getPullRequest().getId());
        GitHubCommitMapping commit = activeObjects.get(GitHubCommitMapping.class, source.getCommit().getId());

        target.setDomain(domain);
        target.setGitHubId(source.getGitHubId());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setPullRequest(pullRequest);
        target.setUrl(source.getUrl());
        target.setHtmlUrl(source.getHtmlUrl());
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
        GitHubRepository domain = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(domain, source.getDomain());

        GitHubUser createdBy = new GitHubUser();
        GitHubUserDAOImpl.map(createdBy, source.getCreatedBy());

        GitHubPullRequest pullRequest = new GitHubPullRequest();
        GitHubPullRequestDAOImpl.map(pullRequest, source.getPullRequest());

        GitHubCommit commit = new GitHubCommit();
        GitHubCommitDAOImpl.map(commit, source.getCommit());

        target.setId(source.getID());
        target.setDomain(domain);
        target.setGitHubId(source.getGitHubId());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setPullRequest(pullRequest);
        target.setUrl(source.getUrl());
        target.setHtmlUrl(source.getHtmlUrl());
        target.setCommit(commit);
        target.setPath(source.getPath());
        target.setLine(source.getLine());
        target.setText(source.getText());
    }

}
