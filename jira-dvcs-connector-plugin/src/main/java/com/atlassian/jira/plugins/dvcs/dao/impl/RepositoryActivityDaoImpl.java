package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestCommentMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityPullRequestUpdateMapping.Status;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.jira.plugins.dvcs.util.IssueKeyExtractor;
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

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryActivityDaoImpl.class);

    private final ActiveObjects activeObjects;

    @SuppressWarnings("unchecked")
    private static final Class<RepositoryActivityPullRequestMapping>[] ALL_ACTIVITY_TABLES = new Class[] {
            RepositoryActivityPullRequestCommentMapping.class, RepositoryActivityPullRequestUpdateMapping.class };

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
                return activeObjects.create((Class<? extends RepositoryActivityPullRequestMapping>) activity
                        .remove(RepositoryActivityPullRequestMapping.ENTITY_TYPE), activity);
            }

        });
    }

    @Override
    public RepositoryPullRequestMapping savePullRequest(final Map<String, Object> request)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<RepositoryPullRequestMapping>()
        {
            @Override
            public RepositoryPullRequestMapping doInTransaction()
            {
                RepositoryPullRequestMapping pullRequest = activeObjects.create(RepositoryPullRequestMapping.class, request);
                return pullRequest;
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePullRequestIssueKyes(int pullRequestId)
    {
        RepositoryPullRequestMapping repositoryPullRequestMapping = findRequestById(pullRequestId);
        Set<String> existingIssueKeys = getExistingIssueKeysMapping(pullRequestId);

        Set<String> currentIssueKeys = new HashSet<String>();
        currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(repositoryPullRequestMapping.getName(), repositoryPullRequestMapping.getDescription()));
        
        // commits
        {
            Query query = Query.select();
            query.where(RepositoryActivityPullRequestUpdateMapping.PULL_REQUEST_ID + " = ? ", pullRequestId);
            for (RepositoryActivityPullRequestUpdateMapping updateActivity : activeObjects.find(RepositoryActivityPullRequestUpdateMapping.class, query)) {
                for (RepositoryActivityCommitMapping commit : updateActivity.getCommits()) {
                    currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(commit.getMessage()));
                }
            }
        }
        
        // comments
        for (RepositoryActivityPullRequestCommentMapping comment : getPullRequestComments(repositoryPullRequestMapping)) {
            currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(comment.getMessage()));
        }
        
        // updates information to reflect current state
        Set<String> addedIssueKeys = new HashSet<String>();
        addedIssueKeys.addAll(currentIssueKeys);
        addedIssueKeys.removeAll(existingIssueKeys);
        
        Set<String> removedIssueKeys = new HashSet<String>();
        removedIssueKeys.addAll(existingIssueKeys);
        removedIssueKeys.removeAll(currentIssueKeys);
        
        // adds news one
        for (String issueKeyToAdd : addedIssueKeys) {
            activeObjects.create(RepositoryPullRequestIssueKeyMapping.class, asIssueKeyMapping(issueKeyToAdd, repositoryPullRequestMapping.getID()));
        }
        
        // removes canceled
        Query query = Query.select();
        query.setWhereClause(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID + " = ? AND " + RepositoryPullRequestIssueKeyMapping.ISSUE_KEY + " = ? ");
        for (String issueKeyToRemove : removedIssueKeys) {
            query.setWhereParams(new Object[] {repositoryPullRequestMapping.getID(), issueKeyToRemove});
            activeObjects.delete(activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, query));
        }
    }

    @Override
    public Set<String> getExistingIssueKeysMapping(Integer pullRequestId)
    {
        Query query = Query.select().from(RepositoryPullRequestIssueKeyMapping.class)
                .where(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID + " = ? ", pullRequestId);
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
    public RepositoryPullRequestMapping findRequestByRemoteId(int repositoryId, long remoteId)
    {
        Query query = Query
                .select()
                .from(RepositoryPullRequestMapping.class)
                .where(RepositoryPullRequestMapping.REMOTE_ID + " = ? AND " + RepositoryPullRequestMapping.TO_REPO_ID + " = ?", remoteId,
                        repositoryId);

        RepositoryPullRequestMapping[] found = activeObjects.find(RepositoryPullRequestMapping.class, query);
        return found.length == 1 ? found[0] : null;
    }

    @Override
    public List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey)
    {
        List<RepositoryActivityPullRequestMapping> ret = new ArrayList<RepositoryActivityPullRequestMapping>();
        List<Integer> pullRequestIds = findRelatedPullRequests(issueKey);

        if (!pullRequestIds.isEmpty())
        {
            for (final Class<RepositoryActivityPullRequestMapping> activityTable : ALL_ACTIVITY_TABLES)
            {
                final Query query = Query.select().from(activityTable)
                        .where(RepositoryActivityPullRequestMapping.PULL_REQUEST_ID + " IN (" + Joiner.on(",").join(pullRequestIds) + ")");

                ret.addAll(Arrays.asList(activeObjects.find(activityTable, query)));

            }
        }
        return sort(ret);
    }

    private List<Integer> findRelatedPullRequests(String issueKey)
    {
        List<Integer> prIds = new ArrayList<Integer>();
        final Query query = Query.select().from(RepositoryPullRequestIssueKeyMapping.class)
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
                ActiveObjectsUtils.delete(
                        activeObjects,
                        RepositoryActivityCommitMapping.class,
                        Query.select()

                                .alias(RepositoryActivityCommitMapping.class, "COMMIT")

                                .alias(RepositoryActivityPullRequestUpdateMapping.class, "PR_UPDATE")
                                .join(RepositoryActivityPullRequestUpdateMapping.class,
                                        "COMMIT." + RepositoryActivityCommitMapping.ACTIVITY_ID + " = PR_UPDATE.ID")

                                .where("PR_UPDATE." + RepositoryActivityPullRequestMapping.REPOSITORY_ID + " = ?", forRepository.getId()));

                // drop activities
                for (final Class<RepositoryActivityPullRequestMapping> activityTable : ALL_ACTIVITY_TABLES)
                {
                    ActiveObjectsUtils.delete(activeObjects, activityTable,
                            Query.select().where(RepositoryActivityPullRequestMapping.REPOSITORY_ID + " = ?", forRepository.getId()));
                }

                // drop pull requests
                Set<Integer> deletedIds = ActiveObjectsUtils.deleteAndReturnIds(
                        activeObjects,
                        RepositoryPullRequestMapping.class,
                        Query.select().from(RepositoryPullRequestMapping.class)
                                .where(RepositoryPullRequestMapping.TO_REPO_ID + " = ?", forRepository.getId()));

                // drop issue keys to PR mappings
                for (Integer deletedId : deletedIds)
                {
                    ActiveObjectsUtils.delete(
                            activeObjects,
                            RepositoryPullRequestIssueKeyMapping.class,
                            Query.select().from(RepositoryPullRequestIssueKeyMapping.class)
                                    .where(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID + " = " + deletedId));
                }

                return null;
            }
        });
    }

    @Override
    public RepositoryActivityCommitMapping saveCommit(final Map<String, Object> commit)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<RepositoryActivityCommitMapping>()
        {
            @Override
            public RepositoryActivityCommitMapping doInTransaction()
            {
                return activeObjects.create(RepositoryActivityCommitMapping.class, commit);
            }

        });
    }

    @Override
    public RepositoryActivityCommitMapping getCommit(int pullRequesCommitId)
    {
        return activeObjects.get(RepositoryActivityCommitMapping.class, pullRequesCommitId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepositoryActivityPullRequestUpdateMapping getPullRequestActivityByRemoteId(RepositoryPullRequestMapping pullRequest,
            String remoteId)
    {
        Query query = Query.select().from(RepositoryActivityPullRequestUpdateMapping.class);
        query.where(RepositoryActivityPullRequestUpdateMapping.PULL_REQUEST_ID + " = ? AND "
                + RepositoryActivityPullRequestUpdateMapping.REMOTE_ID + " = ? ", pullRequest.getID(), remoteId);

        RepositoryActivityPullRequestUpdateMapping[] founded = activeObjects.find(RepositoryActivityPullRequestUpdateMapping.class, query);
        if (founded.length == 1)
        {
            return founded[0];

        } else if (founded.length == 0)
        {
            return null;

        } else
        {
            LOGGER.error("There are multiple records with same pull request ID and remote ID! Pull request ID: " + pullRequest.getID()
                    + " Remote ID: " + remoteId + "First one will be used!");
            return founded[0];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RepositoryActivityPullRequestUpdateMapping> getPullRequestActivityByStatus(RepositoryPullRequestMapping pullRequest,
            Status status)
    {
        Query query = Query.select().from(RepositoryActivityPullRequestUpdateMapping.class);
        query.where(RepositoryActivityPullRequestUpdateMapping.PULL_REQUEST_ID + " = ? AND "
                + RepositoryActivityPullRequestUpdateMapping.STATUS + " = ? ", pullRequest.getID(), status);
        return Arrays.asList(activeObjects.find(RepositoryActivityPullRequestUpdateMapping.class, query));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RepositoryActivityPullRequestCommentMapping> getPullRequestComments(RepositoryPullRequestMapping pullRequest)
    {
        Query query = Query.select().from(RepositoryActivityPullRequestCommentMapping.class);
        query.where(RepositoryActivityPullRequestCommentMapping.PULL_REQUEST_ID + " = ? ", pullRequest.getID());

        RepositoryActivityPullRequestCommentMapping[] founded = activeObjects
                .find(RepositoryActivityPullRequestCommentMapping.class, query);
        return Arrays.asList(founded);
    }

    @Override
    public void updateActivityStatus(int activityId, Status status)
    {
        RepositoryActivityPullRequestUpdateMapping activity = activeObjects.get(RepositoryActivityPullRequestUpdateMapping.class,
                activityId);
        activity.setStatus(status);
        activity.save();
    }

    @Override
    public RepositoryActivityCommitMapping getCommitByNode(int pullRequestId, String node)
    {
        Query query = Query
                .select()
                .alias(RepositoryActivityCommitMapping.class, "COMMIT")
                .alias(RepositoryActivityPullRequestUpdateMapping.class, "PR_UPDATE")
                .join(RepositoryActivityPullRequestUpdateMapping.class,
                        "COMMIT." + RepositoryActivityCommitMapping.ACTIVITY_ID + " = PR_UPDATE.ID")
                .where("PR_UPDATE." + RepositoryActivityPullRequestMapping.PULL_REQUEST_ID + " = ? AND COMMIT."
                        + RepositoryActivityCommitMapping.NODE + " = ?", pullRequestId, node);

        RepositoryActivityCommitMapping[] found = activeObjects.find(RepositoryActivityCommitMapping.class, query);
        return found.length == 1 ? found[0] : null;
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
