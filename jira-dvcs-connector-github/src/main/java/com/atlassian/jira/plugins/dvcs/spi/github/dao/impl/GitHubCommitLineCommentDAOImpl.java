package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitLineCommentMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitLineCommentDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommitLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Active objects implementation of {@link GitHubCommitLineCommentDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitLineCommentDAOImpl implements GitHubCommitLineCommentDAO
{

    /**
     * @see #GitHubCommitLineCommentDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubCommitLineCommentDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * @see @see ColumnNameResolverService#desc(Class)
     */
    private final GitHubCommitLineCommentMapping gitHubCommitLineCommentMappingDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GitHubCommitLineCommentDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService)
    {
        this.activeObjects = activeObjects;
        this.columnNameResolverService = columnNameResolverService;
        this.gitHubCommitLineCommentMappingDescription = columnNameResolverService.desc(GitHubCommitLineCommentMapping.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubCommitLineComment gitHubCommitLineComment)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubCommitLineComment.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubCommitLineComment);
                    GitHubCommitLineCommentMapping created = activeObjects.create(GitHubCommitLineCommentMapping.class, params);
                    map(gitHubCommitLineComment, created);

                } else
                {
                    GitHubCommitLineCommentMapping loaded = activeObjects.get(GitHubCommitLineCommentMapping.class,
                            gitHubCommitLineComment.getId());
                    map(loaded, gitHubCommitLineComment);
                    loaded.save();
                    map(gitHubCommitLineComment, loaded);
                }
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubCommitLineComment gitHubCommitLineComment)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubCommitLineCommentMapping loaded = activeObjects.get(GitHubCommitLineCommentMapping.class,
                        gitHubCommitLineComment.getId());
                activeObjects.delete(loaded);
                gitHubCommitLineComment.setId(0);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitLineComment getById(int id)
    {
        GitHubCommitLineCommentMapping loaded = activeObjects.get(GitHubCommitLineCommentMapping.class, id);
        if (loaded != null)
        {
            GitHubCommitLineComment result = new GitHubCommitLineComment();
            map(result, loaded);
            return result;

        } else
        {
            return null;

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommitLineComment getByGitHubId(long gitHubId)
    {
        Query query = Query.select().where(
                columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getGitHubId()) + " = ? ", gitHubId);
        GitHubCommitLineCommentMapping[] founded = activeObjects.find(GitHubCommitLineCommentMapping.class, query);

        if (founded.length == 0)
        {
            return null;

        } else if (founded.length == 1)
        {
            GitHubCommitLineComment result = new GitHubCommitLineComment();
            map(result, founded[0]);
            return result;

        } else
        {
            throw new IllegalStateException("GitHub ID conflict on commit line comments! GitHub ID: " + gitHubId + " Founded: "
                    + founded.length + " records.");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommitLineComment> getAll(GitHubRepository domain, int first, int count)
    {
        Query query = createAllQuery(domain);
        GitHubCommitLineCommentMapping[] founded = activeObjects.find(GitHubCommitLineCommentMapping.class, query);

        //
        List<GitHubCommitLineComment> result = new LinkedList<GitHubCommitLineComment>();
        for (GitHubCommitLineCommentMapping foundedItem : founded)
        {
            GitHubCommitLineComment resultItem = new GitHubCommitLineComment();
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
        return activeObjects.count(GitHubCommitLineCommentMapping.class, query);
    }

    /**
     * Creates query for {@link #getAll(GitHubRepository, int, int)} resp. {@link #getAllCount(GitHubRepository)}.
     * 
     * @param domain
     *            over which domain
     * @return created query
     */
    private Query createAllQuery(GitHubRepository domain)
    {
        return Query.select() //
                .from(GitHubCommitLineCommentMapping.class) //
                .where(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getDomain()) + " = ? ", domain.getId());
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
                .alias(GitHubCommitLineCommentMapping.class, "COMMENT")
                .join(GitHubCommitLineCommentMapping.class,
                        "COMMIT.ID = COMMENT." + columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getCommit()))
                .where("COMMIT." + columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getDomain()) + " = ? ", domain.getId());
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
    public List<GitHubCommitLineComment> getByCommit(GitHubRepository domain, GitHubCommit commit)
    {
        GitHubCommitLineCommentMapping[] founded = activeObjects.find(
                GitHubCommitLineCommentMapping.class,
                Query.select().where(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getCommit()) + " = ? ",
                        commit.getId()));

        List<GitHubCommitLineComment> result = new LinkedList<GitHubCommitLineComment>();

        for (GitHubCommitLineCommentMapping foundedItem : founded)
        {
            GitHubCommitLineComment targetItem = new GitHubCommitLineComment();
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
    private void map(Map<String, Object> target, GitHubCommitLineComment source)
    {
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubCommitMapping commit = activeObjects.get(GitHubCommitMapping.class, source.getCommit().getId());

        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getDomain()), domain);
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getGitHubId()), source.getGitHubId());
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getCommit()), commit);
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getUrl()), source.getUrl());
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getHtmlUrl()), source.getHtmlUrl());
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getCreatedBy()), source.getCreatedBy()
                .getId());
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getPath()), source.getPath());
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getLine()), source.getLine());
        target.put(columnNameResolverService.column(gitHubCommitLineCommentMappingDescription.getText()), source.getText());
    }

    /**
     * Maps provided model vale into AO value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(GitHubCommitLineCommentMapping target, GitHubCommitLineComment source)
    {
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubCommitMapping commit = activeObjects.get(GitHubCommitMapping.class, source.getCommit().getId());
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());

        target.setDomain(domain);
        target.setGitHubId(source.getGitHubId());
        target.setCommit(commit);
        target.setUrl(source.getUrl());
        target.setHtmlUrl(source.getHtmlUrl());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setPath(source.getPath());
        target.setLine(source.getLine());
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
    private void map(GitHubCommitLineComment target, GitHubCommitLineCommentMapping source)
    {
        GitHubRepository domain = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(domain, source.getDomain());

        GitHubCommit commit = new GitHubCommit();
        GitHubCommitDAOImpl.map(commit, source.getCommit());

        GitHubUser createdBy = new GitHubUser();
        GitHubUserDAOImpl.map(createdBy, source.getCreatedBy());

        target.setId(source.getID());
        target.setDomain(domain);
        target.setGitHubId(source.getGitHubId());
        target.setCommit(commit);
        target.setUrl(source.getUrl());
        target.setHtmlUrl(source.getHtmlUrl());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setPath(source.getPath());
        target.setLine(source.getLine());
        target.setText(source.getText());
    }

}
