package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO implementation of the {@link GitHubEventDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubEventDAOImpl implements GitHubEventDAO
{

    /**
     * @see #GitHubEventDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubEventDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubEventMapping}
     */
    private final GitHubEventMapping gitHubEventMappingDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GitHubEventDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService)
    {
        this.activeObjects = activeObjects;

        this.columnNameResolverService = columnNameResolverService;
        gitHubEventMappingDescription = columnNameResolverService.desc(GitHubEventMapping.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubEvent gitHubEvent)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubEvent.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubEvent);
                    GitHubEventMapping created = activeObjects.create(GitHubEventMapping.class, params);
                    map(gitHubEvent, created);

                } else
                {
                    GitHubEventMapping toSave = activeObjects.get(GitHubEventMapping.class, gitHubEvent.getId());
                    map(toSave, gitHubEvent);
                    toSave.save();
                    map(gitHubEvent, toSave);

                }

                return null;
            }

        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEvent getByGitHubId(String gitHubId)
    {
        Query query = Query.select().where(columnNameResolverService.column(gitHubEventMappingDescription.getGitHubId()) + " = ? ",
                gitHubId);
        GitHubEventMapping[] founded = activeObjects.find(GitHubEventMapping.class, query);

        if (founded.length == 0)
        {
            return null;

        } else if (founded.length == 1)
        {
            GitHubEvent result = new GitHubEvent();
            map(result, founded[0]);
            return result;

        } else
        {
            throw new IllegalStateException("GitHub ID conflict on event! GitHub ID: " + gitHubId + " Founded: " + founded.length
                    + " records.");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEvent getLast(GitHubRepository gitHubRepository)
    {
        Query query = Query.select();
        query.where(columnNameResolverService.column(gitHubEventMappingDescription.getDomain()) + " = ? ", gitHubRepository.getId());
        query.setOrderClause(columnNameResolverService.column(gitHubEventMappingDescription.getCreatedAt()) + " desc ");
        query.setLimit(1);
        GitHubEventMapping[] founded = activeObjects.find(GitHubEventMapping.class, query);

        if (founded.length == 1)
        {
            GitHubEvent result = new GitHubEvent();
            map(result, founded[0]);
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
    public GitHubEvent getLastSavePoint(GitHubRepository gitHubRepository)
    {
        Query query = Query.select().from(GitHubEventMapping.class);
        query.where(
                columnNameResolverService.column(gitHubEventMappingDescription.isSavePoint()) + " = ? AND "
                        + columnNameResolverService.column(gitHubEventMappingDescription.getDomain()) + " = ? ", true,
                gitHubRepository.getId());
        query.setOrderClause(columnNameResolverService.column(gitHubEventMappingDescription.getCreatedAt()) + " desc");
        query.setLimit(1);

        GitHubEventMapping[] founded = activeObjects.find(GitHubEventMapping.class, query);
        if (founded.length == 1)
        {
            GitHubEvent result = new GitHubEvent();
            map(result, founded[0]);
            return result;

        } else
        {
            return null;

        }
    }

    /**
     * Re-maps provided model value into the AO creation map.
     * 
     * @param target
     *            AO creation map
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubEvent source)
    {
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        
        target.put(columnNameResolverService.column(gitHubEventMappingDescription.getGitHubId()), source.getGitHubId());
        target.put(columnNameResolverService.column(gitHubEventMappingDescription.getDomain()), domain);
        target.put(columnNameResolverService.column(gitHubEventMappingDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubEventMappingDescription.isSavePoint()), source.isSavePoint());
    }

    /**
     * Re-maps provided model value into the AO value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(GitHubEventMapping target, GitHubEvent source)
    {
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        
        target.setGitHubId(source.getGitHubId());
        target.setDomain(domain);
        target.setCreatedAt(source.getCreatedAt());
        target.setSavePoint(source.isSavePoint());
    }

    /**
     * Re-maps provided AO value into the model value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    private void map(GitHubEvent target, GitHubEventMapping source)
    {
        GitHubRepository domain = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(domain, source.getDomain());

        target.setId(source.getID());
        target.setGitHubId(source.getGitHubId());
        target.setDomain(domain);
        target.setCreatedAt(source.getCreatedAt());
        target.setSavePoint(source.isSavePoint());
    }

}
