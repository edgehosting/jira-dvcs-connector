package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubEvent;
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
     * @see #GitHubEventDAOImpl(ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     */
    public GitHubEventDAOImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
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
        Query query = Query.select().where(GitHubEventMapping.COLUMN_GIT_HUB_ID + " = ? ", gitHubId);
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
    public GitHubEvent getLast()
    {
        Query query = Query.select();
        query.setOrderClause(GitHubEventMapping.COLUMN_CREATED_AT + " desc ");
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
    public GitHubEvent getLastSavePoint()
    {
        Query query = Query.select().from(GitHubEventMapping.class);
        query.where(GitHubEventMapping.COLUMN_SAVE_POINT + " = ? ", true);
        query.setOrderClause(GitHubEventMapping.COLUMN_CREATED_AT + " desc");
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
        target.put(GitHubEventMapping.COLUMN_GIT_HUB_ID, source.getGitHubId());
        target.put(GitHubEventMapping.COLUMN_CREATED_AT, source.getCreatedAt());
        target.put(GitHubEventMapping.COLUMN_SAVE_POINT, source.isSavePoint());
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
        target.setGitHubId(source.getGitHubId());
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
        target.setId(source.getID());
        target.setGitHubId(source.getGitHubId());
        target.setCreatedAt(source.getCreatedAt());
        target.setSavePoint(source.isSavePoint());
    }

}
