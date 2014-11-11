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

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    @Resource
    @ComponentImport
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
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {

            @Override
            public Void doInTransaction()
            {
                activeObjects.create(GitHubEventMapping.class, gitHubEvent);
                return null;
            }

        });
        return null;
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
        Query query = Query.select().where(GitHubEventMapping.REPOSITORY + " = ? AND " + GitHubEventMapping.GIT_HUB_ID + " = ? ", repository.getId(), gitHubId);
        GitHubEventMapping[] founded = activeObjects.find(GitHubEventMapping.class, query);
        if (founded.length > 1)
        {
            throw new RuntimeException("Multiple GitHubEvents exists with the same id: " + gitHubId);
        }
        return founded.length == 1 ? founded[0] : null;
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
