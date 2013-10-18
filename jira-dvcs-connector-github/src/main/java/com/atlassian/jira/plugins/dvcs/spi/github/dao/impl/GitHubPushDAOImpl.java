package com.atlassian.jira.plugins.dvcs.spi.github.dao.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.service.ColumnNameResolverService;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPushCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubPushMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.activeobjects.GitHubUserMapping;
import com.atlassian.jira.plugins.dvcs.spi.github.dao.GitHubPushDAO;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPush;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * AO implementation of the {@link GitHubPushDAO}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPushDAOImpl implements GitHubPushDAO
{

    /**
     * @see #GitHubPushDAOImpl(ActiveObjects, ColumnNameResolverService, GitHubCommitService)
     */
    private final ActiveObjects activeObjects;

    /**
     * @see #GitHubPushDAOImpl(ActiveObjects, ColumnNameResolverService, GitHubCommitService)
     */
    private final ColumnNameResolverService columnNameResolverService;

    /**
     * {@link ColumnNameResolverService#desc(Class)} of the {@link GitHubPushMapping}
     */
    private final GitHubPushMapping gitHubPushMappingDescription;

    /**
     * {@link ColumnNameResolverService} of the {@link GitHubPushCommitMapping}
     */
    private final GitHubPushCommitMapping gitHubPushCommitMappingDescription;

    /**
     * @see #GitHubPushDAOImpl(ActiveObjects, ColumnNameResolverService, GitHubCommitService)
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * Constructor.
     * 
     * @param activeObjects
     *            injected {@link ActiveObjects} dependency
     * @param columnNameResolverService
     *            injected {@link ColumnNameResolverService} dependency
     * @param gitHubCommitService
     *            injected {@link GitHubCommitService} dependency
     */
    public GitHubPushDAOImpl(ActiveObjects activeObjects, ColumnNameResolverService columnNameResolverService,
            GitHubCommitService gitHubCommitService)
    {
        this.activeObjects = activeObjects;

        this.columnNameResolverService = columnNameResolverService;
        this.gitHubPushMappingDescription = columnNameResolverService.desc(GitHubPushMapping.class);
        this.gitHubPushCommitMappingDescription = columnNameResolverService.desc(GitHubPushCommitMapping.class);

        this.gitHubCommitService = gitHubCommitService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final GitHubPush gitHubPush)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                if (gitHubPush.getId() == 0)
                {
                    Map<String, Object> params = new HashMap<String, Object>();
                    map(params, gitHubPush);
                    GitHubPushMapping created = activeObjects.create(GitHubPushMapping.class, params);
                    updateCommits(created, gitHubPush);

                    map(gitHubPush, created);
                } else
                {
                    GitHubPushMapping loaded = activeObjects.get(GitHubPushMapping.class, gitHubPush.getId());
                    map(loaded, gitHubPush);
                    loaded.save();
                    updateCommits(loaded, gitHubPush);

                    map(gitHubPush, loaded);
                }

                return null;
            }

        });
    }

    /**
     * Updates {@link GitHubPush#getCommits()} relations.
     * 
     * @param target
     *            AO value
     * @param source
     *            model value
     */
    private void updateCommits(GitHubPushMapping target, GitHubPush source)
    {
        Map<Integer, GitHubPushCommitMapping> remainingCommitIdToPushCommitMapping = new HashMap<Integer, GitHubPushCommitMapping>();
        for (GitHubPushCommitMapping pushCommitMapping : target.getCommits())
        {
            remainingCommitIdToPushCommitMapping.put(pushCommitMapping.getCommit().getID(), pushCommitMapping);
        }

        for (GitHubCommit commit : source.getCommits())
        {
            GitHubPushCommitMapping loaded = remainingCommitIdToPushCommitMapping.get(commit.getId());
            if (loaded != null)
            {
                map(loaded, target, commit);

            } else
            {
                addCommit(target, commit);
            }
        }

        for (GitHubPushCommitMapping toDelete : remainingCommitIdToPushCommitMapping.values())
        {
            activeObjects.delete(toDelete);
        }

    }

    /**
     * Adds provided commit to the provided push.
     * 
     * @param gitHubPushMapping
     *            owner of the commit
     * @param gitHubCommit
     *            related commit
     */
    private void addCommit(GitHubPushMapping gitHubPushMapping, GitHubCommit gitHubCommit)
    {
        GitHubCommitMapping gitHubCommitMapping = activeObjects.get(GitHubCommitMapping.class, gitHubCommit.getId());

        Map<String, Object> params = new HashMap<String, Object>();
        map(params, gitHubPushMapping, gitHubCommitMapping);
        activeObjects.create(GitHubPushCommitMapping.class, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final GitHubPush gitHubPush)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                GitHubPushMapping loaded = activeObjects.get(GitHubPushMapping.class, gitHubPush.getId());
                activeObjects.delete(loaded);
                gitHubPush.setId(0);
                return null;
            }

        });
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
    public GitHubPush getByBefore(GitHubRepository repository, String sha)
    {
        Query query = Query.select().where(
                columnNameResolverService.column(gitHubPushMappingDescription.getRepository()) + " = ? AND "
                        + columnNameResolverService.column(gitHubPushMappingDescription.getBefore()) + " = ?", repository.getId(), sha);
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
    public GitHubPush getByHead(GitHubRepository repository, String sha)
    {
        Query query = Query.select().where(
                columnNameResolverService.column(gitHubPushMappingDescription.getRepository()) + " = ? AND "
                        + columnNameResolverService.column(gitHubPushMappingDescription.getHead()) + " = ? ", repository.getId(), sha);
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

        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());

        // re-mapping
        target.put(columnNameResolverService.column(gitHubPushMappingDescription.getDomain()), domain);
        target.put(columnNameResolverService.column(gitHubPushMappingDescription.getCreatedAt()), source.getCreatedAt());
        target.put(columnNameResolverService.column(gitHubPushMappingDescription.getCreatedBy()), source.getCreatedBy().getId());
        target.put(columnNameResolverService.column(gitHubPushMappingDescription.getRepository()), source.getRepository().getId());
        target.put(columnNameResolverService.column(gitHubPushMappingDescription.getRef()), source.getRef());
        target.put(columnNameResolverService.column(gitHubPushMappingDescription.getBefore()), source.getBefore());
        target.put(columnNameResolverService.column(gitHubPushMappingDescription.getHead()), source.getHead());
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
        for (GitHubPushCommitMapping pushToCommit : source.getCommits())
        {
            commits.add(gitHubCommitService.getById(pushToCommit.getCommit().getID()));
        }

        GitHubRepository domain = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(domain, source.getDomain());

        GitHubRepository repository = new GitHubRepository();
        GitHubRepositoryDAOImpl.map(repository, source.getRepository());

        GitHubUser createdBy = new GitHubUser();
        GitHubUserDAOImpl.map(createdBy, source.getCreatedBy());

        // re-mapping
        target.setId(source.getID());
        target.setDomain(domain);
        target.setRepository(repository);
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
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
        GitHubRepositoryMapping domain = activeObjects.get(GitHubRepositoryMapping.class, source.getDomain().getId());
        GitHubRepositoryMapping repository = activeObjects.get(GitHubRepositoryMapping.class, source.getRepository().getId());
        GitHubUserMapping createdBy = activeObjects.get(GitHubUserMapping.class, source.getCreatedBy().getId());

        // re-mapping
        target.setDomain(domain);
        target.setRepository(repository);
        target.setCreatedAt(source.getCreatedAt());
        target.setCreatedBy(createdBy);
        target.setBefore(source.getBefore());
        target.setHead(source.getHead());
        target.setRef(source.getRef());
    }

    /**
     * Re-maps provided {@link GitHubPush#getCommits()} information into the AO creation map of the {@link GitHubPushCommitMapping}.
     * 
     * @param target
     *            creation AO map
     * @param pushOwner
     *            push owner of the commit
     * @param commit
     *            commit reference
     */
    private void map(Map<String, Object> target, GitHubPushMapping pushOwner, GitHubCommitMapping commit)
    {
        target.put(columnNameResolverService.column(gitHubPushCommitMappingDescription.getDomain()), commit.getDomain());
        target.put(columnNameResolverService.column(gitHubPushCommitMappingDescription.getPush()), pushOwner);
        target.put(columnNameResolverService.column(gitHubPushCommitMappingDescription.getCommit()), commit);
    }

    /**
     * Re-maps provided {@link GitHubPush#getCommits()} information into the AO value of the {@link GitHubPushCommitMapping}.
     * 
     * @param target
     *            AO value
     * @param pushOwner
     *            push owner of the commit
     * @param commit
     *            commit reference
     */
    private void map(GitHubPushCommitMapping target, GitHubPushMapping pushOwner, GitHubCommit commit)
    {
        GitHubCommitMapping commitMapping = activeObjects.get(GitHubCommitMapping.class, commit.getId());

        target.setDomain(commitMapping.getDomain());
        target.setPush(pushOwner);
        target.setCommit(commitMapping);
    }

}
