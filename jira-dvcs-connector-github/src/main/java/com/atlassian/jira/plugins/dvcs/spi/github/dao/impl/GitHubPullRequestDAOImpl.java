package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestActionMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPullRequestDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestAction;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
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
     * @see #GitHubPullRequestDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubPullRequestDAOImpl(ActiveObjects, ColumnNameResolverService)
     */
    private ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubPullRequestMapping}
     */
    private final GitHubPullRequestMapping gitHubRepositoryMappingDescription;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubPullRequestActionMapping}
     */
    private final GitHubPullRequestActionMapping gitHubPullRequestActionMappingDescription;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     */
    public GitHubPullRequestDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService)
    {
        this.activeObjects = activeObjects;

        this.columnNameResolverService = columnNameResolverService;
        this.gitHubRepositoryMappingDescription = columnNameResolverService.desc(GitHubPullRequestMapping.class);
        this.gitHubPullRequestActionMappingDescription = columnNameResolverService.desc(GitHubPullRequestActionMapping.class);
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
     *            previous/loaded state
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
     * @param repository
     *            over which repository
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
        Query query = Query.select().where(columnNameResolverService.column(gitHubRepositoryMappingDescription.getGitHubId()) + " = ?",
                gitHubId);
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
    public List<GitHubPullRequest> getByRepository(GitHubRepository repository)
    {
        Query query = Query.select().where(columnNameResolverService.column(gitHubRepositoryMappingDescription.getDomain()) + " = ? ",
                repository.getId());

        List<GitHubPullRequest> result = new ArrayList<GitHubPullRequest>();
        GitHubPullRequestMapping[] founded = activeObjects.find(GitHubPullRequestMapping.class, query);
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
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubRepositoryMapping baseRepository = activeObjects.get(GitHubRepositoryMapping.class, source.getBaseRepository().getId());
        GitHubRepositoryMapping headRepository = activeObjects.get(GitHubRepositoryMapping.class, source.getHeadRepository().getId());

        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getGitHubId()), source.getGitHubId());
        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getPullRequestNumber()), source.getNumber());
        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getDomain()), domain);

        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getBaseRepository()), baseRepository);
        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getBaseSha()), source.getBaseSha());

        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getHeadRepository()), headRepository);
        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getHeadSha()), source.getHeadSha());

        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getTitle()), source.getTitle());
        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getText()), source.getText());
        target.put(columnNameResolverService.column(gitHubRepositoryMappingDescription.getUrl()), source.getUrl());
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
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubRepositoryMapping baseRepository = activeObjects.get(GitHubRepositoryMapping.class, source.getBaseRepository().getId());
        GitHubRepositoryMapping headRepository = activeObjects.get(GitHubRepositoryMapping.class, source.getHeadRepository().getId());

        // re-mapping
        target.setGitHubId(source.getGitHubId());
        target.setPullRequestNumber(source.getNumber());
        target.setDomain(domain);

        target.setBaseRepository(baseRepository);
        target.setBaseSha(source.getBaseSha());

        target.setHeadRepository(headRepository);
        target.setHeadSha(source.getHeadSha());

        target.setTitle(source.getTitle());
        target.setText(source.getText());
        target.setUrl(source.getUrl());
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
        GitHubRepository domain = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(domain, source.getDomain());

        GitHubRepository baseRepository = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(baseRepository, source.getBaseRepository());

        GitHubRepository headRepository = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(headRepository, source.getHeadRepository());

        target.setId(source.getID());
        target.setGitHubId(source.getGitHubId());
        target.setNumber(source.getPullRequestNumber());
        target.setDomain(domain);

        target.setBaseRepository(baseRepository);
        target.setBaseSha(source.getBaseSha());

        target.setHeadRepository(headRepository);
        target.setHeadSha(source.getHeadSha());

        target.setTitle(source.getTitle());
        target.setText(source.getText());
        target.setUrl(source.getUrl());

        target.getActions().clear();
        for (GitHubPullRequestActionMapping sourceAction : source.getActions())
        {
            GitHubPullRequestAction targetAction = new GitHubPullRequestAction();
            map(targetAction, sourceAction);
            target.getActions().add(targetAction);
        }
    }

    /**
     * Re-maps provided model value into the AO creation map.
     * 
     * @param target
     *            creation AO map
     * @param pullRequestMapping
     * @param source
     *            model value
     */
    private void map(Map<String, Object> target, GitHubPullRequestMapping pullRequestMapping, GitHubPullRequestAction source)
    {
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());

        target.put(columnNameResolverService.column(gitHubPullRequestActionMappingDescription.getDomain()), domain);
        target.put(columnNameResolverService.column(gitHubPullRequestActionMappingDescription.getGitHubEventId()),
                source.getGitHubEventId());
        target.put(columnNameResolverService.column(gitHubPullRequestActionMappingDescription.getPullRequest()), pullRequestMapping);
        target.put(columnNameResolverService.column(gitHubPullRequestActionMappingDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubPullRequestActionMappingDescription.getCreatedBy()), createdBy);
        target.put(columnNameResolverService.column(gitHubPullRequestActionMappingDescription.getBaseSha()), source.getBaseSha());
        target.put(columnNameResolverService.column(gitHubPullRequestActionMappingDescription.getHeadSha()), source.getHeadSha());
        target.put(columnNameResolverService.column(gitHubPullRequestActionMappingDescription.getAction()), source.getAction());
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
        GitHubRepository domain = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(domain, source.getDomain());

        GitHubUser targetActor = new GitHubUser();
        GitHubUserDAOImpl.map(targetActor, source.getCreatedBy());

        target.setId(source.getID());
        target.setDomain(domain);
        target.setAction(source.getAction());
        target.setCreatedBy(targetActor);
        target.setCreatedAt(source.getCreatedAt());
        target.setBaseSha(source.getBaseSha());
        target.setHeadSha(source.getHeadSha());
        target.setGitHubEventId(source.getGitHubEventId());
    }

}
