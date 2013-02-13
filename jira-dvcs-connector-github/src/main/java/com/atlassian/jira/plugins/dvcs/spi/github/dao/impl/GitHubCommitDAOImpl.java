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
    public GitHubCommit getBySha(String sha)
    {
        Query query = Query.select().where(columnNameResolverService.column(gitHubCommitMappingDescription.getSha()) + " = ?", sha);
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
    public List<GitHubCommit> getByIssueKey(String issueKey)
    {
        List<GitHubCommit> result = new LinkedList<GitHubCommit>();
        for (GitHubCommitMapping source : activeObjects.find(GitHubCommitMapping.class))
        {
            GitHubCommit target = new GitHubCommit();
            map(target, source);
            result.add(target);
        }
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
        GitHubRepositoryMapping repository = activeObjects.get(GitHubRepositoryMapping.class, source.getRepository().getId());

        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getRepository()), repository);
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getSha()), source.getSha());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubCommitMappingDescription.getCreatedBy()), source.getCreatedBy());
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
        GitHubRepositoryMapping repository = activeObjects.get(GitHubRepositoryMapping.class, source.getRepository().getId());

        target.setRepository(repository);
        target.setSha(source.getSha());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(source.getCreatedBy());
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
        GitHubRepository repository = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(repository, source.getRepository());

        target.setId(source.getID());
        target.setRepository(repository);
        target.setSha(source.getSha());
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(source.getCreatedBy());
        target.setMessage(source.getMessage());
    }

}
