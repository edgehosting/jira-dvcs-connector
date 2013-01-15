package com.atlassian.jira.plugins.dvcs.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * 
 * DefaultRepositoryActivityDao
 * 
 * 
 * <br />
 * <br />
 * Created on 15.1.2013, 15:17:03 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
@SuppressWarnings("all")
public class DefaultRepositoryActivityDao implements RepositoryActivityDao
{

    private final ActiveObjects activeObjects;

    private static final Class<RepositoryActivityPullRequestMapping>[] ALL_ACTIVITY_TABLES = (Class[]) new Object[] {
            RepositoryActivityPullRequestCommentMapping.class, RepositoryActivityPullRequestLikeMapping.class,
            RepositoryActivityPullRequestUpdateMapping.class };

    public DefaultRepositoryActivityDao(ActiveObjects activeObjects)
    {
        super();
        this.activeObjects = activeObjects;
    }

    @Override
    public void save(final Map<String, Object> activity)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            public Void doInTransaction()
            {
                activeObjects.create((Class) activity.remove(RepositoryActivityPullRequestMapping.ENTITY_TYPE),
                        activity);
                return null;
            }

        });
    }

    @Override
    public List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey)
    {
        List<RepositoryActivityPullRequestMapping> ret = new ArrayList<RepositoryActivityPullRequestMapping>();

        for (final Class<RepositoryActivityPullRequestMapping> activityTable : ALL_ACTIVITY_TABLES)
        {
            final Query query = Query
                    .select()
                    .from(activityTable)
                    .where(RepositoryActivityPullRequestMapping.ISSUE_KEY + " = ?",
                            new Object[] { issueKey.toUpperCase() });

            ret.addAll(Arrays.asList(activeObjects.find(activityTable, query)));

        }
        return sort(ret);
    }

    @Override
    public void removeAll(Repository forRepository)
    {
        for (final Class<RepositoryActivityPullRequestMapping> activityTable : ALL_ACTIVITY_TABLES)
        {
            final Query query = Query
                    .select()
                    .from(activityTable)
                    .where(RepositoryActivityPullRequestMapping.REPO_SLUG + " = ?",
                            new Object[] { forRepository.getSlug() });

            activeObjects.executeInTransaction(new TransactionCallback<Void>()
            {
                public Void doInTransaction()
                {
                    deleteFromTableByQuery(activityTable, query);
                    return null;
                }
            });
        }

    }

    // --------------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------
    // private helpers
    // --------------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------

    private void deleteFromTableByQuery(final Class<RepositoryActivityPullRequestMapping> activityTable, Query query)
    {
        activeObjects.stream(activityTable, query,
                new EntityStreamCallback<RepositoryActivityPullRequestMapping, Integer>()
                {
                    @Override
                    public void onRowRead(RepositoryActivityPullRequestMapping mapping)
                    {
                        activeObjects.delete(activeObjects.get(activityTable, mapping.getID()));
                    }
                });
    }

    private List<RepositoryActivityPullRequestMapping> sort(List<RepositoryActivityPullRequestMapping> sortable)
    {
        Collections.sort(sortable, new Comparator<RepositoryActivityPullRequestMapping>()
        {
            @Override
            public int compare(RepositoryActivityPullRequestMapping o1, RepositoryActivityPullRequestMapping o2)
            {
                try
                {
                    return -o1.getLastUpdatedOn().compareTo(o2.getLastUpdatedOn());
                } catch (NullPointerException e)
                {
                    return 0;
                }
            }

        });

        return sortable;
    }

}
