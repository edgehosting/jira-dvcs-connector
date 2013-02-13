package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubUserDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO based {@link GitHubUserDAO} implementation.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubUserDAOImpl implements GitHubUserDAO
{

    /**
     * @see #GitHubUserDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubUserDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of te {@link GitHubUserMapping}
     */
    private final GitHubUserMapping gitHubUserDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GitHubUserDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService)
    {
        this.activeObjects = activeObjects;

        this.columnNameResolverService = columnNameResolverService;
        this.gitHubUserDescription = columnNameResolverService.desc(GitHubUserMapping.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubUser gitHubUser)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {

                if (gitHubUser.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubUser);
                    GitHubUserMapping created = activeObjects.create(GitHubUserMapping.class, params);
                    map(gitHubUser, created);

                } else
                {
                    GitHubUserMapping toSave = activeObjects.get(GitHubUserMapping.class, gitHubUser.getId());
                    map(toSave, gitHubUser);
                    toSave.save();
                    map(gitHubUser, toSave);

                }
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubUser gitHubUser)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubUserMapping toDelete = activeObjects.get(GitHubUserMapping.class, gitHubUser.getId());
                activeObjects.delete(toDelete);
                gitHubUser.setId(0);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubUser getById(int id)
    {
        GitHubUserMapping loaded = activeObjects.get(GitHubUserMapping.class, id);
        if (loaded == null)
        {
            return null;

        } else
        {
            GitHubUser result = new GitHubUser();
            map(result, loaded);
            return result;

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubUser getByLogin(String login)
    {
        Query query = Query.select().where(columnNameResolverService.column(gitHubUserDescription.getLogin()) + " = ? ", login);
        GitHubUserMapping[] founded = activeObjects.find(GitHubUserMapping.class, query);

        if (founded.length == 0)
        {
            return null;

        } else if (founded.length == 1)
        {
            GitHubUser result = new GitHubUser();
            map(result, founded[0]);
            return result;

        } else
        {
            throw new IllegalStateException("Multiple users with the same login! User login: " + login + " Founded: " + founded.length
                    + " records.");

        }
    }

    /**
     * Re-maps model value into the AO creation parameters.
     * 
     * @param target
     *            AO value creation parameters
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubUser source)
    {
        GitHubRepositoryMapping repository = activeObjects.get(GitHubRepositoryMapping.class, source.getRepository().getId());

        target.put(columnNameResolverService.column(gitHubUserDescription.getRepository()), repository);
        target.put(columnNameResolverService.column(gitHubUserDescription.getGitHubId()), source.getGitHubId());
        target.put(columnNameResolverService.column(gitHubUserDescription.getSynchronizedAt()), source.getSynchronizedAt());
        target.put(columnNameResolverService.column(gitHubUserDescription.getLogin()), source.getLogin());
        target.put(columnNameResolverService.column(gitHubUserDescription.getName()), source.getName());
        target.put(columnNameResolverService.column(gitHubUserDescription.getEmail()), source.getEmail());
        target.put(columnNameResolverService.column(gitHubUserDescription.getUrl()), source.getUrl());
        target.put(columnNameResolverService.column(gitHubUserDescription.getAvatarUrl()), source.getAvatarUrl());
    }

    /**
     * Re-maps model value into the AO value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(GitHubUserMapping target, GitHubUser source)
    {
        GitHubRepositoryMapping repository = activeObjects.get(GitHubRepositoryMapping.class, source.getRepository().getId());

        target.setRepository(repository);
        target.setGitHubId(source.getGitHubId());
        target.setSynchronizedAt(source.getSynchronizedAt());
        target.setLogin(source.getLogin());
        target.setName(source.getName());
        target.setEmail(source.getEmail());
        target.setUrl(source.getUrl());
        target.setAvatarUrl(source.getAvatarUrl());
    }

    /**
     * Re-maps AO into the model.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    static void map(GitHubUser target, GitHubUserMapping source)
    {
        GitHubRepository repository = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(repository, source.getRepository());

        target.setId(source.getID());
        target.setRepository(repository);
        target.setGitHubId(source.getGitHubId());
        target.setSynchronizedAt(source.getSynchronizedAt());
        target.setLogin(source.getLogin());
        target.setName(source.getName());
        target.setEmail(source.getEmail());
        target.setUrl(source.getUrl());
        target.setAvatarUrl(source.getAvatarUrl());
    }

}
