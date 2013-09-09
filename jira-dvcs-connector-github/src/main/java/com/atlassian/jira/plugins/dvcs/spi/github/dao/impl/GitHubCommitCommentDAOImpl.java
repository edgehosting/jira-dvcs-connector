package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Active objects implementation of {@link GitHubCommitCommentDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitCommentDAOImpl implements GitHubCommitCommentDAO
{

    /**
     * @see #GitHubCommitCommentDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubCommitCommentDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * @see ColumnNameResolverService#desc(Class)
     */
    private final GitHubCommitCommentMapping gitHubCommitCommentDescription;

    /**
     * @see ColumnNameResolverService#desc(Class)
     */
    private final GitHubCommitMapping gitHubCommitMappingDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GitHubCommitCommentDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService)
    {
        this.activeObjects = activeObjects;

        this.columnNameResolverService = columnNameResolverService;
        gitHubCommitCommentDescription = columnNameResolverService.desc(GitHubCommitCommentMapping.class);
        gitHubCommitMappingDescription = columnNameResolverService.desc(GitHubCommitMapping.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubCommitComment gitHubCommitComment)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubCommitComment.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubCommitComment);
                    GitHubCommitCommentMapping created = activeObjects.create(GitHubCommitCommentMapping.class, params);
                    map(gitHubCommitComment, created);

                } else
                {
                    GitHubCommitCommentMapping loaded = activeObjects.get(GitHubCommitCommentMapping.class, gitHubCommitComment.getId());
                    map(loaded, gitHubCommitComment);
                    loaded.save();
                    map(gitHubCommitComment, loaded);

                }

                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubCommitComment gitHubCommitComment)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubCommitCommentMapping toDelete = activeObjects.get(GitHubCommitCommentMapping.class, gitHubCommitComment.getId());
                activeObjects.delete(toDelete);
                gitHubCommitComment.setId(0);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitComment getById(int id)
    {
        GitHubCommitCommentMapping loaded = activeObjects.get(GitHubCommitCommentMapping.class, id);
        if (loaded == null)
        {
            return null;
        }

        GitHubCommitComment result = new GitHubCommitComment();
        map(result, loaded);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitComment getByGitHubId(long gitHubId)
    {
        Query query = Query.select().where(columnNameResolverService.column(gitHubCommitCommentDescription.getGitHubId()) + " = ? ",
                gitHubId);
        GitHubCommitCommentMapping[] founded = activeObjects.find(GitHubCommitCommentMapping.class, query);

        if (founded.length == 0)
        {
            return null;

        } else if (founded.length == 1)
        {
            GitHubCommitComment result = new GitHubCommitComment();
            map(result, founded[0]);
            return result;

        } else
        {
            throw new IllegalStateException("GitHub ID conflict on commit comments! GitHub ID: " + gitHubId + " Founded: " + founded.length
                    + " records.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommitComment> getAll(GitHubRepository domain, int first, int count)
    {
        Query query = createAllQuery(domain);
        query.setOffset(first);
        query.setLimit(count);

        GitHubCommitCommentMapping[] founded = activeObjects.find(GitHubCommitCommentMapping.class, query);

        // re-maps founded collection
        List<GitHubCommitComment> result = new LinkedList<GitHubCommitComment>();
        for (GitHubCommitCommentMapping foundedItem : founded)
        {
            GitHubCommitComment resultItem = new GitHubCommitComment();
            map(resultItem, foundedItem);
            result.add(resultItem);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAllCount(GitHubRepository domain)
    {
        Query query = createAllQuery(domain);
        return activeObjects.count(GitHubCommitCommentMapping.class, query);
    }

    /**
     * Creates query related to {@link #getAll(GitHubRepository, int, int)} and {@link #getAllCount(GitHubRepository)}.
     * 
     * @param domain
     * @return created query
     */
    private Query createAllQuery(GitHubRepository domain)
    {
        return Query.select() //
                .from(GitHubCommitCommentMapping.class) //
                .where(columnNameResolverService.column(gitHubCommitCommentDescription.getDomain()) + " = ? ", domain.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommit> getCommentedCommits(GitHubRepository domain, int first, int count)
    {
        Query query = Query
                .select()
                .from(GitHubCommitMapping.class)
                .alias(GitHubCommitMapping.class, "COMMIT")
                .alias(GitHubCommitCommentMapping.class, "COMMENT")
                .join(GitHubCommitCommentMapping.class,
                        "COMMIT.ID = COMMENT." + columnNameResolverService.column(gitHubCommitCommentDescription.getCommit()))
                .where("COMMIT." + columnNameResolverService.column(gitHubCommitMappingDescription.getDomain()) + " = ? ", domain.getId());
        query.setOffset(first);
        query.setLimit(count);

        GitHubCommitMapping[] founded = activeObjects.find(GitHubCommitMapping.class, query);

        List<GitHubCommit> result = new LinkedList<GitHubCommit>();
        for (GitHubCommitMapping foundedItem : founded)
        {
            GitHubCommit targetItem = new GitHubCommit();
            GitHubCommitDAOImpl.map(targetItem, foundedItem);
            result.add(targetItem);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommitComment> getByCommit(GitHubRepository domain, GitHubCommit commit)
    {
        GitHubCommitCommentMapping[] founded = activeObjects.find(GitHubCommitCommentMapping.class,
                Query.select()
                        .where(columnNameResolverService.column(gitHubCommitCommentDescription.getCommit()) + " = ? ", commit.getId()));

        List<GitHubCommitComment> result = new LinkedList<GitHubCommitComment>();

        for (GitHubCommitCommentMapping foundedItem : founded)
        {
            GitHubCommitComment targetItem = new GitHubCommitComment();
            map(targetItem, foundedItem);
            result.add(targetItem);
        }

        return result;
    }

    /**
     * Maps provided model value into AO creation map.
     * 
     * @param target
     *            AO creation map
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubCommitComment source)
    {
        target.put(columnNameResolverService.column(gitHubCommitCommentDescription.getDomain()), source.getDomain().getId());
        target.put(columnNameResolverService.column(gitHubCommitCommentDescription.getGitHubId()), source.getGitHubId());
        target.put(columnNameResolverService.column(gitHubCommitCommentDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubCommitCommentDescription.getCreatedBy()), source.getCreatedBy().getId());
        target.put(columnNameResolverService.column(gitHubCommitCommentDescription.getCommit()), source.getCommit().getId());
        target.put(columnNameResolverService.column(gitHubCommitCommentDescription.getUrl()), source.getUrl());
        target.put(columnNameResolverService.column(gitHubCommitCommentDescription.getHtmlUrl()), source.getHtmlUrl());
        target.put(columnNameResolverService.column(gitHubCommitCommentDescription.getText()), source.getText());
    }

    /**
     * Maps provided model value into AO value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(GitHubCommitCommentMapping target, GitHubCommitComment source)
    {
        target.setDomain(activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId()));
        target.setGitHubId(source.getGitHubId());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId()));
        target.setCommit(activeObjects.get(GitHubCommitMapping.class, source.getCommit().getId()));
        target.setUrl(source.getUrl());
        target.setHtmlUrl(source.getHtmlUrl());
        target.setText(source.getText());
    }

    /**
     * Maps provided AO value into model value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    private void map(GitHubCommitComment target, GitHubCommitCommentMapping source)
    {
        GitHubCommit commit = new GitHubCommit();
        GitHubCommitDAOImpl.map(commit, source.getCommit());

        GitHubUser createdBy = new GitHubUser();
        GitHubUserDAOImpl.map(createdBy, source.getCreatedBy());

        target.setId(source.getID());
        target.setGitHubId(source.getGitHubId());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setCommit(commit);
        target.setUrl(source.getUrl());
        target.setHtmlUrl(source.getHtmlUrl());
        target.setText(source.getText());
    }

}
