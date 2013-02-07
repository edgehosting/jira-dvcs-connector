package com.atlassian.jira.plugins.dvcs.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Entity;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Joiner;

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
public class RepositoryActivityDaoImpl implements RepositoryActivityDao
{

    private final ActiveObjects activeObjects;

    private static final Class<RepositoryActivityPullRequestMapping>[] ALL_ACTIVITY_TABLES = new Class[] {
            RepositoryActivityPullRequestCommentMapping.class, RepositoryActivityPullRequestLikeMapping.class,
            RepositoryActivityPullRequestUpdateMapping.class };

    public RepositoryActivityDaoImpl(ActiveObjects activeObjects)
    {
        super();
        this.activeObjects = activeObjects;
    }

    @Override
    public void saveActivity(final Map<String, Object> activity)
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
    public RepositoryPullRequestMapping savePullRequest(final Map<String, Object> request, final Set<String> issueKeys)
    {
        return
        activeObjects.executeInTransaction(new TransactionCallback<RepositoryPullRequestMapping>()
                {
                    public RepositoryPullRequestMapping doInTransaction()
                    {
                        RepositoryPullRequestMapping pullRequest = activeObjects.create(RepositoryPullRequestMapping.class,
                                request);
                        // persist mappings
                        for (String issueKey : issueKeys)
                        {
                            activeObjects.create(RepositoryPullRequestIssueKeyMapping.class, asIssueKeyMapping(issueKey, pullRequest.getID()));
                        }
                        return  pullRequest;
                    }

                });
    }
    
    @Override
    public void saveIssueKeysMappings(final Collection<String> issueKeys, final int pullRequestId)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                for (String issueKey : issueKeys)
                {
                    activeObjects.create(RepositoryPullRequestIssueKeyMapping.class, asIssueKeyMapping(issueKey, pullRequestId));
                }
                return null;
            }
        });
    }
    
    @Override
    public Set<String> getExistingIssueKeysMapping(Integer pullRequestId)
    {
        Query query = Query.select()
                .from(RepositoryPullRequestIssueKeyMapping.class)
                .where(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID +  " = ? ", 
                      pullRequestId);
        RepositoryPullRequestIssueKeyMapping[] mappings = activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, query);
        Set<String> issueKeys = new java.util.HashSet<String>();
        for (RepositoryPullRequestIssueKeyMapping repositoryPullRequestIssueKeyMapping : mappings)
        {
            issueKeys.add(repositoryPullRequestIssueKeyMapping.getIssueKey());
        }
        return issueKeys;
    }

    
    protected Map<String, Object> asIssueKeyMapping(String issueKey, int pullRequestId)
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(RepositoryPullRequestIssueKeyMapping.ISSUE_KEY, issueKey);
        map.put(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID, pullRequestId);
        return map;
    }

    @Override
    public RepositoryPullRequestMapping findRequestById(Integer localId, String repoSlug)
    {
        Query query = Query.select()
                           .from(RepositoryPullRequestMapping.class)
                           .where(RepositoryPullRequestMapping.LOCAL_ID +  " = ? AND " 
                                + RepositoryPullRequestMapping.TO_REPO_SLUG + " = ?", localId, repoSlug);
        
        RepositoryPullRequestMapping[] found = activeObjects.find(RepositoryPullRequestMapping.class, query);
        return found.length == 1 ? found[0] : null;
    }

    @Override
    public List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey)
    {
        List<RepositoryActivityPullRequestMapping> ret = new ArrayList<RepositoryActivityPullRequestMapping>();
        List<Integer> pullRequestIds = findRelatedPullRequests(issueKey);

        if ( !pullRequestIds.isEmpty() )
        {
		    for (final Class<RepositoryActivityPullRequestMapping> activityTable : ALL_ACTIVITY_TABLES)
		    {
		        final Query query = Query
		                .select()
		                .from(activityTable)
		                .where(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID + " IN (" + Joiner.on(",").join(pullRequestIds) + ")");
		
		        ret.addAll(Arrays.asList(activeObjects.find(activityTable, query)));
		
		    }
        }
		return sort(ret);
    }

    private List<Integer> findRelatedPullRequests(String issueKey)
    {
        List<Integer> prIds = new ArrayList<Integer>();
        final Query query = Query
                .select()
                .from(RepositoryPullRequestIssueKeyMapping.class)
                .where(RepositoryPullRequestIssueKeyMapping.ISSUE_KEY + " = ?",
                        new Object[] { issueKey.toUpperCase() });
        
        RepositoryPullRequestIssueKeyMapping[] mappings = activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, query);
        for (RepositoryPullRequestIssueKeyMapping issueKeyMapping : mappings)
        {
            prIds.add(issueKeyMapping.getPullRequestId());
        }
        return prIds;
    }

    public void removeAll(final Repository forRepository)
    {
        final List<Query> activityDeleteQueries = new ArrayList<Query>();
        
        for (final Class<RepositoryActivityPullRequestMapping> activityTable : ALL_ACTIVITY_TABLES)
        {
            activityDeleteQueries.add( 
                     Query
                    .select()
                    .from(activityTable)
                    .where(RepositoryActivityPullRequestMapping.REPO_SLUG + " = ?",
                            new Object[] { forRepository.getSlug() })
                            );
        }


        activeObjects.executeInTransaction(new TransactionCallback<Void>()
                {
                    public Void doInTransaction()
                    {
                        // drop activities
                        int i = 0;
                        HashSet<Integer> deletedIds = new java.util.HashSet<Integer>();
                        for (final Class<RepositoryActivityPullRequestMapping> activityTable : ALL_ACTIVITY_TABLES)
                        {
                            deleteFromTableByQuery(activityTable, activityDeleteQueries.get(i), deletedIds);
                            i++;
                        }
                        // drop pull requests
<<<<<<< local
                        HashSet<Integer> deletedIds = new java.util.HashSet<Integer>();
                        deleteFromTableByQuery(RepositoryPullRequestMapping.class,
                                Query
                                .select()
                                .from(RepositoryPullRequestMapping.class)
                                .where(RepositoryPullRequestMapping.TO_REPO_SLUG + " = ?",
                                        new Object[] { forRepository.getSlug() })
                                        , deletedIds);
=======
                        
//                        deleteFromTableByQuery(RepositoryActivityPullRequestMapping.class,
//                                Query
//                                .select()
//                                .from(RepositoryActivityPullRequestMapping.class)
//                                .where(RepositoryActivityPullRequestMapping.REPO_SLUG + " = ?",
//                                        new Object[] { forRepository.getSlug() })
//                                        , deletedIds);
>>>>>>> other
                        
                        // drop issue keys to PR mappings
                        if (!deletedIds.isEmpty())
                        {
	                        deleteFromTableByQuery(RepositoryPullRequestIssueKeyMapping.class,
	                                Query
	                                .select()
	                                .from(RepositoryPullRequestIssueKeyMapping.class)
	                                .where(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID + " IN (" + Joiner.on(",").join(deletedIds) +")",
	                                        new Object[] {  })
	                                        , null);
                        }
	                    return null;
                    }
                });

    }
    
    // --------------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------
    // private helpers
    // --------------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------

    private <T extends Entity> void deleteFromTableByQuery(final Class<T> activityTable, Query query, final Set<Integer> deletedIds)
    {
        activeObjects.stream(activityTable, query,
                new EntityStreamCallback<T, Integer>()
                {
                    @Override
                    public void onRowRead(T mapping)
                    {
                        if (deletedIds != null) {
                            deletedIds.add(mapping.getID());
                        }
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
