package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPushMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPushDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;

/**
 * AO implementation of the {@link GitHubPushDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPushDAOImpl implements GitHubPushDAO
{

    /**
     * @see #GitHubPushDAOImpl(ActiveObjects, GitHubCommitService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubPushDAOImpl(ActiveObjects, GitHubCommitService)
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            Injected {@link ActiveObjects} dependency.
     * @param gitHubCommitService
     *            Injected {@link GitHubCommitService} dependency.
     */
    public GitHubPushDAOImpl(ActiveObjects activeObjects, GitHubCommitService gitHubCommitService)
    {
        this.activeObjects = activeObjects;
        this.gitHubCommitService = gitHubCommitService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(GitHubPush gitHubPush)
    {
        if (gitHubPush.getId() == 0)
        {
            Map<String, Object> params = new HashMap<String, Object>();
            map(params, gitHubPush);
            GitHubPushMapping created = activeObjects.create(GitHubPushMapping.class, params);
            map(gitHubPush, created);

        } else
        {
            GitHubPushMapping loaded = activeObjects.get(GitHubPushMapping.class, gitHubPush.getId());
            map(loaded, gitHubPush);
            loaded.save();
            map(gitHubPush, loaded);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(GitHubPush gitHubPush)
    {
        GitHubPushMapping loaded = activeObjects.get(GitHubPushMapping.class, gitHubPush.getId());
        activeObjects.delete(loaded);
        gitHubPush.setId(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getById(int id)
    {
        GitHubPushMapping loaded = activeObjects.get(GitHubPushMapping.class, id);
        if (loaded == null)
        {
            return null;
        }

        GitHubPush result = new GitHubPush();
        map(result, loaded);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getByBefore(String sha)
    {
        Query query = Query.select().where(GitHubPushMapping.COLUMN_BEFORE + " = ?", sha);
        GitHubPushMapping[] founded = activeObjects.find(GitHubPushMapping.class, query);
        if (founded.length == 1)
        {
            GitHubPush result = new GitHubPush();
            map(result, founded[0]);
            return result;

        } else if (founded.length == 0)
        {
            return null;
        } else
        {
            throw new IllegalStateException("SHA conflict of the Push! SHA before: " + sha + " Number of records: " + founded.length);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubPush getByHead(String sha)
    {
        Query query = Query.select().where(GitHubPushMapping.COLUMN_HEAD + " = ?", sha);
        GitHubPushMapping[] founded = activeObjects.find(GitHubPushMapping.class, query);
        if (founded.length == 1)
        {
            GitHubPush result = new GitHubPush();
            map(result, founded[0]);
            return result;

        } else if (founded.length == 0)
        {
            return null;
        } else
        {
            throw new IllegalStateException("SHA conflict of the Push! SHA head: " + sha + " Number of records: " + founded.length);
        }
    }

    // //
    // Mapping functionality
    // //

    /**
     * Creates AO creation map for the provided model value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubPush source)
    {
        // pre-processing
        GitHubCommitMapping[] commits = new GitHubCommitMapping[source.getCommits().size()];
        for (int i = 0; i < commits.length; i++)
        {
            commits[i] = activeObjects.get(GitHubCommitMapping.class, source.getCommits().get(i).getId());
        }

        // re-mapping
        target.put(GitHubPushMapping.COLUMN_CREATED_AT, source.getCreatedAt());
        target.put(GitHubPushMapping.COLUMN_BEFORE, source.getBefore());
        target.put(GitHubPushMapping.COLUMN_HEAD, source.getHead());
        target.put(GitHubPushMapping.COLUMN_REF, source.getRef());
        target.put(GitHubPushMapping.COLUMN_COMMITS, commits);
    }

    /**
     * Re-maps the provided AO value into the provided model value.
     * 
     * @param target
     *            model value
     * @param source
     *            AO value
     */
    private void map(GitHubPush target, GitHubPushMapping source)
    {
        // pre-processing
        List<GitHubCommit> commits = new LinkedList<GitHubCommit>();
        for (GitHubCommitMapping commit : source.getCommits())
        {
            commits.add(gitHubCommitService.getById(commit.getID()));
        }

        // re-mapping
        target.setId(source.getID());
        target.setCreatedAt(source.getCreatedAt());
        target.setBefore(source.getBefore());
        target.setHead(source.getHead());
        target.setRef(source.getRef());
        target.setCommits(commits);
    }

    /**
     * Re-maps the provided model value into the provided AO value.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void map(GitHubPushMapping target, GitHubPush source)
    {
        // pre-processing
        GitHubCommitMapping[] commits = new GitHubCommitMapping[source.getCommits().size()];
        for (int i = 0; i < commits.length; i++)
        {
            commits[i] = activeObjects.get(GitHubCommitMapping.class, source.getCommits().get(i).getId());
        }

        // re-mapping
        target.setCreatedAt(source.getCreatedAt());
        target.setBefore(source.getBefore());
        target.setHead(source.getHead());
        target.setRef(source.getRef());
        target.setCommits(commits);
    }
}
