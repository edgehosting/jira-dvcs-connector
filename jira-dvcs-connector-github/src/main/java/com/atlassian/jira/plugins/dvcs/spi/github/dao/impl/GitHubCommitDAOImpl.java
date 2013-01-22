package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubCommitDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;

/**
 * AO implementation of the {@link GitHubCommitDAO}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class GitHubCommitDAOImpl implements GitHubCommitDAO
{

    /**
     * @see #GitHubCommitDAOImpl(ActiveObjects)
     */
    private final ActiveObjects activeObjects;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            Injected {@link ActiveObjects} dependency.
     */
    public GitHubCommitDAOImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubCommit gitHubCommit)
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubCommit gitHubCommit)
    {
        GitHubCommitMapping loaded = activeObjects.get(GitHubCommitMapping.class, gitHubCommit.getId());
        activeObjects.delete(loaded);
        gitHubCommit.setId(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommit getById(int id)
    {
        GitHubCommit result = new GitHubCommit();
        GitHubCommitMapping loaded = activeObjects.get(GitHubCommitMapping.class, id);
        map(result, loaded);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubCommit getBySha(String sha)
    {
        Query query = Query.select().where(GitHubCommitMapping.KEY_SHA + " = ?", sha);
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
        // FIXME<stanislav-dvorscak@solumiss.eu>
        throw new UnsupportedOperationException();
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
        target.put(GitHubCommitMapping.KEY_SHA, source.getSha());
        target.put(GitHubCommitMapping.KEY_DATE, source.getDate());
        target.put(GitHubCommitMapping.KEY_AUTHOR, source.getAuthor());
        target.put(GitHubCommitMapping.KEY_MESSAGE, source.getMessage());
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
        target.setSha(source.getSha());
        target.setDate(source.getDate());
        target.setAuthor(source.getAuthor());
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
    private void map(GitHubCommit target, GitHubCommitMapping source)
    {
        target.setId(source.getID());
        target.setSha(source.getSha());
        target.setDate(source.getDate());
        target.setAuthor(source.getAuthor());
        target.setMessage(source.getMessage());
    }

}
