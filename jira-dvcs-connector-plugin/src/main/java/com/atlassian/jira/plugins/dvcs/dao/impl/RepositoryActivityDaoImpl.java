package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
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
public class RepositoryActivityDaoImpl implements RepositoryActivityDao
{

    private final ActiveObjects activeObjects;

    @SuppressWarnings("unchecked")
    private static final Class<RepositoryActivityPullRequestMapping>[] ALL_ACTIVITY_TABLES = new Class[] {
            RepositoryActivityPullRequestCommentMapping.class, 
            RepositoryActivityPullRequestUpdateMapping.class };

    public RepositoryActivityDaoImpl(ActiveObjects activeObjects)
    {
        super();
        this.activeObjects = activeObjects;
    }

    @Override
    public RepositoryActivityPullRequestMapping saveActivity(final Map<String, Object> activity)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<RepositoryActivityPullRequestMapping>()
        {
            @Override
            @SuppressWarnings("unchecked")
            public RepositoryActivityPullRequestMapping doInTransaction()
            {
                return activeObjects.create((Class<? extends RepositoryActivityPullRequestMapping>) activity.remove(RepositoryActivityPullRequestMapping.ENTITY_TYPE),
                        activity);
            }

        });
    }
    
    @Override
    public RepositoryPullRequestMapping savePullRequest(final Map<String, Object> request, final Set<String> issueKeys)
    {
        return
        activeObjects.executeInTransaction(new TransactionCallback<RepositoryPullRequestMapping>()
                {
                    @Override
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
    public RepositoryPullRequestMapping findRequestById(int localId)
    {
        return activeObjects.get(RepositoryPullRequestMapping.class, localId);
    }
    
    @Override
    public RepositoryPullRequestMapping findRequestByRemoteId(int repositoryId, int remoteId)
    {
        Query query = Query.select()
                           .from(RepositoryPullRequestMapping.class)
                           .where(RepositoryPullRequestMapping.REMOTE_ID +  " = ? AND " 
                                + RepositoryPullRequestMapping.TO_REPO_ID + " = ?", remoteId, repositoryId);
        
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
                .where(RepositoryPullRequestIssueKeyMapping.ISSUE_KEY + " = ?", issueKey.toUpperCase());
        
        RepositoryPullRequestIssueKeyMapping[] mappings = activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, query);
        for (RepositoryPullRequestIssueKeyMapping issueKeyMapping : mappings)
        {
            prIds.add(issueKeyMapping.getPullRequestId());
        }
        return prIds;
    }

    @Override
    public void removeAll(final Repository forRepository)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
                {
                    @Override
                    public Void doInTransaction()
                    {
                        // drop commits
                        ActiveObjectsUtils.delete(activeObjects, RepositoryActivityCommitMapping.class,
                                Query.select()
                                .join(RepositoryActivityPullRequestUpdateMapping.class,"ACTIVITY_ID=PR_UPDATE.ID")
                                .alias(RepositoryActivityPullRequestUpdateMapping.class, "PR_UPDATE")
                                .where("PR_UPDATE." + RepositoryActivityPullRequestMapping.REPOSITORY_ID + " = ?", forRepository.getId()));
                        
                        // drop activities
                        for (final Class<RepositoryActivityPullRequestMapping> activityTable : ALL_ACTIVITY_TABLES)
                        {
                            ActiveObjectsUtils.delete(activeObjects, activityTable, Query
                                    .select()
                                    .where(RepositoryActivityPullRequestMapping.REPOSITORY_ID + " = ?", forRepository.getId()));
                        }
                        
                        // drop pull requests
                        Set<Integer> deletedIds = ActiveObjectsUtils.deleteAndReturnIds(activeObjects,RepositoryPullRequestMapping.class,
                                Query
                                .select()
                                .from(RepositoryPullRequestMapping.class)
                                .where(RepositoryPullRequestMapping.TO_REPO_ID + " = ?", forRepository.getId()));
                        
                        // drop issue keys to PR mappings
                        if (!deletedIds.isEmpty())
                        {
                            ActiveObjectsUtils.delete(activeObjects, RepositoryPullRequestIssueKeyMapping.class,
                                    Query
                                    .select()
                                    .from(RepositoryPullRequestIssueKeyMapping.class)
                                    .where(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID + " IN (" + Joiner.on(",").join(deletedIds) +")"));
                        }
                        return null;
                    }
                });
    }
    
    @Override
    public void saveCommit(final Map<String, Object> commit)
    {
        activeObjects.executeInTransaction(new TransactionCallback<RepositoryActivityCommitMapping>()
                {
                    @Override
                    public RepositoryActivityCommitMapping doInTransaction()
                    {
                        return activeObjects.create(RepositoryActivityCommitMapping.class, commit);
                    }

                });
    }

    // --------------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------
    // private helpers
    // --------------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------------------------------------

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
