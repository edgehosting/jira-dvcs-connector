package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubUserDAO;
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
     * @see #GitHubUserDAOImpl(ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            Injected {@link ActiveObjects} dependency.
     */
    public GitHubUserDAOImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
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
        Query query = Query.select().where(GitHubUserMapping.COLUMN_LOGIN + " = ? ", login);
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
        target.put(GitHubUserMapping.COLUMN_SYNCHRONIZED_AT, source.getSynchronizedAt());
        target.put(GitHubUserMapping.COLUMN_GIT_HUB_ID, source.getGitHubId());
        target.put(GitHubUserMapping.COLUMN_LOGIN, source.getLogin());
        target.put(GitHubUserMapping.COLUMN_NAME, source.getName());
        target.put(GitHubUserMapping.COLUMN_EMAIL, source.getEmail());
        target.put(GitHubUserMapping.COLUMN_URL, source.getUrl());
        target.put(GitHubUserMapping.COLUMN_AVATAR_URL, source.getAvatarUrl());
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
        target.setId(source.getID());
        target.setSynchronizedAt(source.getSynchronizedAt());
        target.setGitHubId(source.getGitHubId());
        target.setLogin(source.getLogin());
        target.setName(source.getName());
        target.setEmail(source.getEmail());
        target.setUrl(source.getUrl());
        target.setAvatarUrl(source.getAvatarUrl());
    }

}
