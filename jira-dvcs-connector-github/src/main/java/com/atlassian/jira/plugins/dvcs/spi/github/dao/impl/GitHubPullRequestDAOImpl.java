package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestActionMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO implementation of the {@link GitHubPullRequestDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestDAOImpl implements GitHubPullRequestDAO
{

    /**
     * @see #GitHubPullRequestDAOImpl(ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     */
    public GitHubPullRequestDAOImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubPullRequest gitHubPullRequest)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubPullRequest.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubPullRequest);
                    GitHubPullRequestMapping created = activeObjects.create(GitHubPullRequestMapping.class, params);

                    for (GitHubPullRequestAction action : gitHubPullRequest.getActions())
                    {
                        addAction(created, action);
                    }

                    map(gitHubPullRequest, created);

                } else
                {
                    GitHubPullRequestMapping loaded = activeObjects.get(GitHubPullRequestMapping.class, gitHubPullRequest.getId());
                    map(loaded, gitHubPullRequest);
                    loaded.save();

                    updateActions(gitHubPullRequest, loaded);

                    map(gitHubPullRequest, loaded);

                }

                return null;
            }

        });
    }

    /**
     * Updates {@link GitHubPullRequest#getActions()}.
     * 
     * @param newPullRequest
     *            new state
     * @param loadedPullRequest
     *            previos/loaded state
     */
    private void updateActions(GitHubPullRequest newPullRequest, GitHubPullRequestMapping loadedPullRequest)
    {
        Map<Integer, GitHubPullRequestActionMapping> remainingLoadedActions = new HashMap<Integer, GitHubPullRequestActionMapping>();
        for (GitHubPullRequestActionMapping actionMapping : loadedPullRequest.getActions())
        {
            remainingLoadedActions.put(actionMapping.getID(), actionMapping);
        }

        // adds new actions
        for (GitHubPullRequestAction action : newPullRequest.getActions())
        {
            if (remainingLoadedActions.containsKey(action.getId()))
            {
                remainingLoadedActions.remove(action.getId());

            } else
            {
                addAction(loadedPullRequest, action);

            }
        }

        // removes remaining/obsolete actions
        for (GitHubPullRequestActionMapping remainingAction : remainingLoadedActions.values())
        {
            activeObjects.delete(remainingAction);
        }
    }

    /**
     * Adds provided action to the provided {@link GitHubPullRequestMapping}.
     * 
     * @param pullRequest
     *            to update
     * @param action
     *            to add
     */
    private void addAction(GitHubPullRequestMapping pullRequest, GitHubPullRequestAction action)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        map(params, pullRequest, action);
        GitHubPullRequestActionMapping created = activeObjects.create(GitHubPullRequestActionMapping.class, params);
        map(action, created);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubPullRequest gitHubPullRequest)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubPullRequestMapping toDelete = activeObjects.get(GitHubPullRequestMapping.class, gitHubPullRequest.getId());
                activeObjects.delete(toDelete);
                gitHubPullRequest.setId(0);
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest getById(int id)
    {
        GitHubPullRequestMapping loaded = activeObjects.get(GitHubPullRequestMapping.class, id);
        if (loaded == null)
        {
            return null;
        }

        GitHubPullRequest result = new GitHubPullRequest();
        map(result, loaded);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest getByGitHubId(long gitHubId)
    {
        Query query = Query.select().where(GitHubPullRequestMapping.COLUMN_GIT_HUB_ID + " = ?", gitHubId);
        GitHubPullRequestMapping[] founded = activeObjects.find(GitHubPullRequestMapping.class, query);
        if (founded.length == 1)
        {
            GitHubPullRequest result = new GitHubPullRequest();
            map(result, founded[0]);
            return result;

        } else if (founded.length == 0)
        {
            return null;

        } else
        {
            throw new IllegalStateException("GitHub ID conflict on pull requests! GitHub ID: " + gitHubId + " Founded: " + founded.length
                    + " records.");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPullRequest> getGitHubPullRequest(String issueKey)
    {
        // FIXME<Stanislav Dvorscak>

        List<GitHubPullRequest> result = new ArrayList<GitHubPullRequest>();
        GitHubPullRequestMapping[] founded = activeObjects.find(GitHubPullRequestMapping.class);
        for (GitHubPullRequestMapping found : founded)
        {
            GitHubPullRequest pullRequest = new GitHubPullRequest();
            map(pullRequest, found);
            result.add(pullRequest);
        }

        return result;
    }

    // //
    // Mapping functionality
    // //

    /**
     * Re-maps the provided model value into the AO creation map.
     * 
     * @param target
     *            AO map value
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubPullRequest source)
    {
        target.put(GitHubPullRequestMapping.COLUMN_GIT_HUB_ID, source.getGitHubId());
        target.put(GitHubPullRequestMapping.COLUMN_TITLE, source.getTitle());
    }

    private void map(Map<String, Object> target, GitHubPullRequestMapping pullRequestMapping, GitHubPullRequestAction source)
    {
        GitHubUserMapping actor = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());

        target.put(GitHubPullRequestActionMapping.COLUMN_GIT_HUB_EVENT_ID, source.getGitHubEventId());
        target.put(GitHubPullRequestActionMapping.COLUMN_PULL_REQUEST, pullRequestMapping);
        target.put(GitHubPullRequestActionMapping.COLUMN_CREATED_AT, source.getCreatedAt());
        target.put(GitHubPullRequestActionMapping.COLUMN_CREATED_BY, actor);
        target.put(GitHubPullRequestActionMapping.COLUMN_ACTION, source.getAction());
    }

    /**
     * Re-maps the provided AO value into the model value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    static void map(GitHubPullRequest target, GitHubPullRequestMapping source)
    {
        target.setId(source.getID());
        target.setGitHubId(source.getGitHubId());
        target.setTitle(source.getTitle());

        target.getActions().clear();
        for (GitHubPullRequestActionMapping sourceAction : source.getActions())
        {
            GitHubPullRequestAction targetAction = new GitHubPullRequestAction();
            map(targetAction, sourceAction);
            target.getActions().add(targetAction);
        }
    }

    /**
     * Re-maps the provided AO value into the model value of the {@link GitHubPullRequestAction}.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    private static void map(GitHubPullRequestAction target, GitHubPullRequestActionMapping source)
    {
        GitHubUser targetActor = new GitHubUser();
        GitHubUserDAOImpl.map(targetActor, source.getCreatedBy());

        target.setId(source.getID());
        target.setAction(source.getAction());
        target.setCreatedBy(targetActor);
        target.setAt(source.getCreatedAt());
        target.setGitHubEventId(source.getGitHubEventId());
    }

    /**
     * Re-maps the provided model value into the AO value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(GitHubPullRequestMapping target, GitHubPullRequest source)
    {
        // re-mapping
        target.setGitHubId(source.getGitHubId());
        target.setTitle(source.getTitle());
    }

}
