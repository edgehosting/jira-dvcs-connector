package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO implementation of the {@link GitHubCommitDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubCommitDAOImpl implements GitHubCommitDAO
{

    /**
     * @see #GitHubCommitDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubCommitDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubCommitMapping}.
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
    public GitHubCommitDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService)
    {
        this.activeObjects = activeObjects;

        this.columnNameResolverService = columnNameResolverService;
        this.gitHubCommitMappingDescription = columnNameResolverService.desc(GitHubCommitMapping.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubCommit gitHubCommit)
    {

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubCommit.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubCommit);
                    GitHubCommitMapping created = activeObjects.create(GitHubCommitMapping.class, params);
                    map(gitHubCommit, created);

                } else
                {
                    GitHubCommitMapping loaded = activeObjects.get(GitHubCommitMapping.class, gitHubCommit.getId());
                    map(loaded, gitHubCommit);
                    loaded.save();
                    map(gitHubCommit, loaded);

                }

                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubCommit gitHubCommit)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubCommitMapping toDelete = activeObjects.get(GitHubCommitMapping.class, gitHubCommit.getId());
                activeObjects.delete(toDelete);
                gitHubCommit.setId(0);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommit getById(int id)
    {
        GitHubCommitMapping loaded = activeObjects.get(GitHubCommitMapping.class, id);
        if (loaded == null)
        {
            return null;
        }

        GitHubCommit result = new GitHubCommit();
        map(result, loaded);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommit getBySha(GitHubRepository domain, GitHubRepository repository, String sha)
    {
        // prepares query
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new LinkedList<Object>();

        whereClause.append(columnNameResolverService.column(gitHubCommitMappingDescription.getDomain())).append(" = ? AND ");
        params.add(domain.getId());

        whereClause.append(columnNameResolverService.column(gitHubCommitMappingDescription.getRepository())).append(" = ? AND ");
        params.add(repository.getId());

        whereClause.append(columnNameResolverService.column(gitHubCommitMappingDescription.getSha())).append(" = ? ");
        params.add(sha);

        Query query = Query.select().where(whereClause.toString(), params.toArray());

        //
        GitHubCommitMapping[] founded = activeObjects.find(GitHubCommitMapping.class, query);
        if (founded.length == 1)
        {
            GitHubCommit result = new GitHubCommit();
            map(result, founded[0]);
            return result;

        } else if (founded.length == 0)
        {
            return null;

        } else
        {
            throw new IllegalStateException("SHA conflict of commits! SHA: " + sha + " Founded: " + founded.length + " records.");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubCommit> getAll(GitHubRepository domain, int first, int count)
    {
        Query query = createAllQuery(domain);
        query.setOffset(first);
        query.setLimit(count);
        GitHubCommitMapping[] founded = activeObjects.find(GitHubCommitMapping.class, query);

        List<GitHubCommit> result = new LinkedList<GitHubCommit>();
        GitHubCommit resultItem;
        for (GitHubCommitMapping foundedItem : founded)
        {
            resultItem = new GitHubCommit();
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
        return activeObjects.count(GitHubCommitMapping.class, query);
    }

    /**
     * Builds ALL query.
     * 
     * @param domain
     *            for repository
     * @return query
     */
    private Query createAllQuery(GitHubRepository domain)
    {
        Query result = Query.select();
        result.where(columnNameResolverService.column(gitHubCommitMappingDescription.getDomain()) + " = ? ", domain.getId());
        return result;
    }

    // //
    // Mapping functionality
    // //

    /**
     * Re-maps the provided model value into the creation AO map.
     * 
     * @param target
     *            to which is mapped
     * @param source
     *            from which is mapped
     */
    private void map(Map<String, Object> target, GitHubCommit source)
    {
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getDomain()), source.getDomain().getId());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getRepository()), source.getRepository().getId());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getSha()), source.getSha());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getCreatedBy()), source.getCreatedBy());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getCreatedByName()), source.getCreatedByName());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getCreatedByAvatarUrl()), source.getCreatedByAvatarUrl());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getHtmlUrl()), source.getHtmlUrl());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getMessage()), source.getMessage());
    }

    /**
     * Re-maps the provided model value into the AO value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(GitHubCommitMapping target, GitHubCommit source)
    {
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubRepositoryMapping repository = activeObjects.get(GitHubRepositoryMapping.class, source.getRepository().getId());

        target.setDomain(domain);
        target.setRepository(repository);
        target.setSha(source.getSha());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreatedByName(source.getCreatedByName());
        target.setCreatedByAvatarUrl(source.getCreatedByAvatarUrl());
        target.setHtmlUrl(source.getHtmlUrl());
        target.setMessage(source.getMessage());
    }

    /**
     * Re-maps the provided AO value into the model value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    static void map(GitHubCommit target, GitHubCommitMapping source)
    {
        GitHubRepository domain = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(domain, source.getDomain());

        GitHubRepository repository = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(repository, source.getRepository());

        target.setId(source.getID());
        target.setDomain(domain);
        target.setRepository(repository);
        target.setSha(source.getSha());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreatedByName(source.getCreatedByName());
        target.setCreatedByAvatarUrl(source.getCreatedByAvatarUrl());
        target.setHtmlUrl(source.getHtmlUrl());
        target.setMessage(source.getMessage());
    }

}
