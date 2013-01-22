package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO implementation of the {@link GitHubPullRequestDAO}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
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
                    map(gitHubPullRequest, created);

                } else
                {
                    GitHubPullRequestMapping loaded = activeObjects.get(GitHubPullRequestMapping.class, gitHubPullRequest.getId());
                    map(loaded, gitHubPullRequest);
                    loaded.save();
                    map(gitHubPullRequest, loaded);

                }

                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPullRequest gitHubPullRequest)
    {
        GitHubPullRequestMapping loaded = activeObjects.get(GitHubPullRequestMapping.class, gitHubPullRequest.getId());
        activeObjects.delete(loaded);
        gitHubPullRequest.setId(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest getById(int id)
    {
        GitHubPullRequest result = new GitHubPullRequest();
        GitHubPullRequestMapping loaded = activeObjects.get(GitHubPullRequestMapping.class, id);
        map(result, loaded);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPullRequest getByGitHubId(long gitHubId)
    {
        Query query = Query.select().where(GitHubPullRequestMapping.KEY_GIT_HUB_ID + " = ?", gitHubId);
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
            throw new IllegalStateException("GitHub ID conflict of pull requests! GitHub ID: " + gitHubId + " Founded: " + founded.length
                    + " records.");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GitHubPullRequest> getGitHubPullRequest(String issueKey)
    {
        // FIXME<stanislav-dvorscak@solumiss.eu>
        throw new UnsupportedOperationException();
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
        target.put(GitHubPullRequestMapping.KEY_TITLE, source.getTitle());
    }

    /**
     * Re-maps the provided AO value into the model value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    private void map(GitHubPullRequest target, GitHubPullRequestMapping source)
    {
        target.setId(source.getID());
        target.setTitle(source.getTitle());
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
        target.setTitle(source.getTitle());
    }

}
