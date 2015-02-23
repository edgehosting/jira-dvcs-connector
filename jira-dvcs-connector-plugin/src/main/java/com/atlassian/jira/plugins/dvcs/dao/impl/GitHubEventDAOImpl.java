package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper;
import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper.OrderClause;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import javax.annotation.Resource;

import static com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils.ID;

/**
 * AO implementation of the {@link GitHubEventDAO}.
 *
 * @author Stanislav Dvorscak
 */
@Component
public class GitHubEventDAOImpl implements GitHubEventDAO
{
    private static final Logger log = LoggerFactory.getLogger(GitHubEventDAOImpl.class);

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    @Resource
    @ComponentImport
    @SuppressWarnings ("SpringJavaAutowiringInspection")
    private ActiveObjects activeObjects;

    /**
     * Injected {@link QueryHelper} dependency.
     */
    @Resource
    private QueryHelper queryHelper;

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEventMapping create(final Map<String, Object> gitHubEvent)
    {
        GitHubEventMapping createdMapping = activeObjects.executeInTransaction(new TransactionCallback<GitHubEventMapping>()
        {
            @Override
            public GitHubEventMapping doInTransaction()
            {
                return activeObjects.create(GitHubEventMapping.class, gitHubEvent);
            }
        });

        final int repositoryId = createdMapping.getRepository().getID();
        final String gitHubId = createdMapping.getGitHubId();
        GitHubEventMapping[] retrievedMappings = findAllById(repositoryId, gitHubId);

        if (retrievedMappings.length > 1)
        {
            String stack = ExceptionUtils.getStackTrace(new Throwable());
            final String warningMessage = "Just created a GitHubEventMapping for repository {} and gitHubId {} and there now more than one in the database. This is the calling stack:\n";
            log.warn(warningMessage, new Object[] { repositoryId, gitHubId, stack });
        }

        return createdMapping;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markAsSavePoint(final GitHubEventMapping gitHubEvent)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                gitHubEvent.setSavePoint(true);
                gitHubEvent.save();
                return null;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAll(Repository repository)
    {
        Query allForRepositoryQuery = Query.select().where(GitHubEventMapping.REPOSITORY + " = ? ", repository.getId());
        ActiveObjectsUtils.delete(activeObjects, GitHubEventMapping.class, allForRepositoryQuery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEventMapping getByGitHubId(Repository repository, String gitHubId)
    {
        GitHubEventMapping[] githubEvents = findAllById(repository.getId(), gitHubId);
        if (githubEvents.length > 1)
        {
            final Object[] warnParams = { gitHubId, repository.getId(), githubEvents.length };
            log.warn("Search for event {} in repository {} found this many {}, "
                    + "returning the one marked as a save point, if none are save points then the first", warnParams);

            GitHubEventMapping eventToUse = githubEvents[0];
            for (GitHubEventMapping retrievedMapping : githubEvents)
            {
                if (retrievedMapping.isSavePoint())
                {
                    eventToUse = retrievedMapping;
                }
            }

            final Object[] infoParams = { gitHubId, repository.getId(), eventToUse.getID() };
            log.info("When multiple mappings were found for event {} in repository {} we chose the one with this id {}", infoParams);

            return eventToUse;
        }
        return githubEvents.length == 1 ? githubEvents[0] : null;
    }

    private GitHubEventMapping[] findAllById(int repositoryId, String gitHubId)
    {
        Query query = Query.select().
                where(GitHubEventMapping.REPOSITORY + " = ? AND " + GitHubEventMapping.GIT_HUB_ID + " = ? ", repositoryId,
                        gitHubId).order("ID");
        return activeObjects.find(GitHubEventMapping.class, query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitHubEventMapping getLastSavePoint(Repository repository)
    {
        Query query = Query.select();
        query.where(GitHubEventMapping.REPOSITORY + " = ? AND " + GitHubEventMapping.SAVE_POINT + " = ? ", repository.getId(), true);
        query.setOrderClause(queryHelper.getOrder(new OrderClause[] {
                new OrderClause(GitHubEventMapping.CREATED_AT, OrderClause.Order.DESC), new OrderClause(ID, OrderClause.Order.DESC) }));
        query.setLimit(1);

        GitHubEventMapping[] founded = activeObjects.find(GitHubEventMapping.class, query);
        return founded.length == 1 ? founded[0] : null;
    }

}
