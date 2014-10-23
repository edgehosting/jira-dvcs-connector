package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryDomainMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestToCommitMapping;
import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingFunction;
import com.atlassian.jira.plugins.dvcs.dao.ao.EntityBeanGenerator;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.impl.IssueKeyExtractor;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;
import net.java.ao.Query;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

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
@Component
public class RepositoryPullRequestDaoImpl implements RepositoryPullRequestDao
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryPullRequestDaoImpl.class);

    private final ActiveObjects activeObjects;

    private final EntityBeanGenerator beanGenerator;

    @Autowired
    public RepositoryPullRequestDaoImpl(@ComponentImport ActiveObjects activeObjects, EntityBeanGenerator beanGenerator)
    {
        super();
        this.activeObjects = checkNotNull(activeObjects);
        this.beanGenerator = beanGenerator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkCommit(Repository domain, RepositoryPullRequestMapping request, RepositoryCommitMapping commit)
    {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(RepositoryPullRequestToCommitMapping.DOMAIN, domain.getId());
        params.put(RepositoryPullRequestToCommitMapping.REQUEST_ID, request.getID());
        params.put(RepositoryPullRequestToCommitMapping.COMMIT, commit.getID());
        activeObjects.create(RepositoryPullRequestToCommitMapping.class, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlinkCommits(Repository domain, RepositoryPullRequestMapping request, Iterable<? extends RepositoryCommitMapping> commits)
    {
        Iterable<Integer> commitIds = Iterables.transform(commits, new Function<RepositoryCommitMapping, Integer>()
        {
            @Override
            public Integer apply(final RepositoryCommitMapping repositoryCommitMapping)
            {
                return repositoryCommitMapping.getID();
            }
        });

        final String baseWhereClause = ActiveObjectsUtils.renderListOperator(RepositoryPullRequestToCommitMapping.COMMIT, "IN", "OR", commitIds);

        Query query = Query.select().where(RepositoryPullRequestToCommitMapping.REQUEST_ID + " = ? AND "
                + baseWhereClause, ObjectArrays.concat(request.getID(), Iterables.toArray(commitIds, Object.class)));
        ActiveObjectsUtils.delete(activeObjects, RepositoryPullRequestToCommitMapping.class, query);
    }

    public RepositoryPullRequestMapping createPullRequest()
    {
        return beanGenerator.createInstanceOf(RepositoryPullRequestMapping.class);
    }

    @Override
    public void removeCommits(Iterable<? extends RepositoryCommitMapping> commits)
    {
        activeObjects.delete(Iterables.toArray(commits, RepositoryCommitMapping.class));
    }

    @Override
    public RepositoryPullRequestMapping savePullRequest(final Repository domain, final Map<String, Object> request)
    {
        return doSavePullRequest(domain.getId(), request);
    }

    @Override
    public RepositoryPullRequestMapping savePullRequest(final RepositoryPullRequestMapping pullRequest)
    {
        return doSavePullRequest(pullRequest.getDomainId(), asMap(pullRequest));
    }

    private RepositoryPullRequestMapping doSavePullRequest(final int repositoryId, final Map<String, Object> request)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<RepositoryPullRequestMapping>()
        {
            @Override
            public RepositoryPullRequestMapping doInTransaction()
            {
                request.put(RepositoryDomainMapping.DOMAIN, repositoryId);
                return activeObjects.create(RepositoryPullRequestMapping.class, request);
            }
        });
    }

    @Override
    public RepositoryPullRequestMapping updatePullRequestInfo(int localId, RepositoryPullRequestMapping pullRequestMapping)
    {
        final RepositoryPullRequestMapping request = findRequestById(localId);
        request.setName(ActiveObjectsUtils.stripToLimit(pullRequestMapping.getName(), 255));
        request.setSourceBranch(pullRequestMapping.getSourceBranch());
        request.setDestinationBranch(pullRequestMapping.getDestinationBranch());
        request.setLastStatus(pullRequestMapping.getLastStatus());
        request.setSourceRepo(pullRequestMapping.getSourceRepo());
        request.setUpdatedOn(pullRequestMapping.getUpdatedOn());
        request.setCommentCount(pullRequestMapping.getCommentCount());

        request.setExecutedBy(pullRequestMapping.getExecutedBy());

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                request.save();
                return null;
            }
        });
        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int updatePullRequestIssueKeys(Repository domain, int pullRequestId)
    {
        RepositoryPullRequestMapping repositoryPullRequestMapping = findRequestById(pullRequestId);
        Set<String> existingIssueKeys = getIssueKeys(domain.getId(), pullRequestId);

        Set<String> currentIssueKeys = new HashSet<String>();
        currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(repositoryPullRequestMapping.getName(), repositoryPullRequestMapping.getSourceBranch()));

        // commits
        for (RepositoryCommitMapping commit : repositoryPullRequestMapping.getCommits())
        {
            if (!commit.isMerge())
            {
                currentIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(commit.getMessage()));
            }
        }

        // updates information to reflect current state
        Set<String> addedIssueKeys = new HashSet<String>();
        addedIssueKeys.addAll(currentIssueKeys);
        addedIssueKeys.removeAll(existingIssueKeys);

        Set<String> removedIssueKeys = new HashSet<String>();
        removedIssueKeys.addAll(existingIssueKeys);
        removedIssueKeys.removeAll(currentIssueKeys);

        // adds news one
        for (String issueKeyToAdd : addedIssueKeys)
        {
            Map<String, Object> issueKeyMapping = asIssueKeyMapping(issueKeyToAdd, repositoryPullRequestMapping.getID());
            issueKeyMapping.put(RepositoryDomainMapping.DOMAIN, domain.getId());
            activeObjects.create(RepositoryPullRequestIssueKeyMapping.class, issueKeyMapping);
        }

        // removes canceled
        for (String issueKeyToRemove : removedIssueKeys)
        {
            activeObjects.delete(activeObjects.find(
                    RepositoryPullRequestIssueKeyMapping.class,
                    Query.select().where(
                            RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID
                                    + " = ? AND " + RepositoryPullRequestIssueKeyMapping.ISSUE_KEY + " = ? ", domain.getId(),
                            repositoryPullRequestMapping.getID(), issueKeyToRemove)));
        }

        return currentIssueKeys.size();
    }

    @Override
    public Set<String> getIssueKeys(int repositoryId, int pullRequestId)
    {
        Query query = Query
                .select()
                .from(RepositoryPullRequestIssueKeyMapping.class)
                .where(RepositoryDomainMapping.DOMAIN + " = ? AND " + RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID + " = ? ",
                        repositoryId, pullRequestId);

        RepositoryPullRequestIssueKeyMapping[] mappings = activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, query);
        Set<String> issueKeys = Sets.newHashSet();
        for (RepositoryPullRequestIssueKeyMapping mapping : mappings)
        {
            issueKeys.add(mapping.getIssueKey());
        }
        return issueKeys;
    }

    @Override
    public Set<String> getExistingIssueKeysMapping(Repository domain, Integer pullRequestId)
    {
        checkNotNull(domain, "domain");
        return getIssueKeys(domain.getId(), pullRequestId);
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
    public RepositoryPullRequestMapping findRequestByRemoteId(Repository domain, long remoteId)
    {
        Query query = Query
                .select()
                .from(RepositoryPullRequestMapping.class)
                .where(RepositoryPullRequestMapping.REMOTE_ID + " = ? AND " + RepositoryPullRequestMapping.DOMAIN + " = ?", remoteId,
                        domain.getId());

        RepositoryPullRequestMapping[] found = activeObjects.find(RepositoryPullRequestMapping.class, query);
        return found.length == 1 ? found[0] : null;
    }

    @Override
    public List<RepositoryPullRequestMapping> getByIssueKeys(final Iterable<String> issueKeys)
    {
        Collection<Integer> prIds = findRelatedPullRequests(issueKeys);
        if (prIds.isEmpty())
        {
            return Lists.newArrayList();
        }
        final String whereClause = ActiveObjectsUtils.renderListOperator("pr.ID", "IN", "OR", prIds).toString();
        final Object [] params = ObjectArrays.concat(new Object[]{Boolean.FALSE, Boolean.TRUE}, prIds.toArray(), Object.class);
        
        Query select = Query.select()
                .alias(RepositoryMapping.class, "repo")
                .alias(RepositoryPullRequestMapping.class, "pr")
                .join(RepositoryMapping.class, "repo.ID = pr." + RepositoryPullRequestMapping.TO_REPO_ID)
                .where("repo." + RepositoryMapping.DELETED + " = ? AND repo." + RepositoryMapping.LINKED + " = ? AND " + whereClause, params);
        return Arrays.asList(activeObjects.find(RepositoryPullRequestMapping.class, select));
    }

    @Override
    public List<RepositoryPullRequestMapping> getByIssueKeys(final Iterable<String> issueKeys, final String dvcsType)
    {
        Collection<Integer> prIds = findRelatedPullRequests(issueKeys);
        
        if (prIds.isEmpty())
        {
            return Lists.newArrayList();
        }
       
        final String whereClause = ActiveObjectsUtils.renderListOperator("pr.ID", "IN", "OR", prIds).toString();
        final Object [] params = ObjectArrays.concat(new Object[] { dvcsType, Boolean.FALSE, Boolean.TRUE }, prIds.toArray(), Object.class);

        Query select = Query.select()
                .alias(RepositoryMapping.class, "repo")
                .alias(RepositoryPullRequestMapping.class, "pr")
                .alias(OrganizationMapping.class, "org")
                .join(RepositoryMapping.class, "repo.ID = pr." + RepositoryPullRequestMapping.TO_REPO_ID)
                .join(OrganizationMapping.class, "repo." + RepositoryMapping.ORGANIZATION_ID + " = org.ID")
                .where("org." + OrganizationMapping.DVCS_TYPE + " = ? AND repo." + RepositoryMapping.DELETED + " = ? AND repo." + RepositoryMapping.LINKED + " = ? AND " + whereClause, params);
        
        return Arrays.asList(activeObjects.find(RepositoryPullRequestMapping.class, select));
    }

    private Collection<Integer> findRelatedPullRequests(final Iterable<String> issueKeys)
    {
        return Collections2.transform(findRelatedPullRequestsObjects(issueKeys), new Function<RepositoryPullRequestIssueKeyMapping, Integer>()
        {
            @Override
            public Integer apply(@Nullable RepositoryPullRequestIssueKeyMapping input)
            {
                return input.getPullRequestId();
            }
        });
    }

    private List<RepositoryPullRequestIssueKeyMapping> findRelatedPullRequestsObjects(final Iterable<String> issueKeys)
    {
        String whereClause = ActiveObjectsUtils.renderListOperator(RepositoryPullRequestIssueKeyMapping.ISSUE_KEY, "IN", "OR", issueKeys).toString();

        final Query query = Query.select().from(RepositoryPullRequestIssueKeyMapping.class)
                .where(whereClause, Iterables.toArray(issueKeys, Object.class));

        RepositoryPullRequestIssueKeyMapping[] mappings = activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, query);
        return Arrays.asList(mappings);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void removeAll(Repository domain)
    {
        for (Class<? extends RepositoryDomainMapping> entityType : new Class[]
                { RepositoryPullRequestIssueKeyMapping.class, RepositoryPullRequestToCommitMapping.class, PullRequestParticipantMapping.class, RepositoryPullRequestMapping.class, 
                        RepositoryCommitMapping.class })
        {
            ActiveObjectsUtils.delete(activeObjects, entityType,
                    Query.select().where(RepositoryDomainMapping.DOMAIN + " = ? ", domain.getId()));
        }
    }

    @Override
    public RepositoryCommitMapping saveCommit(final Repository domain, final Map<String, Object> commit)
    {
        return activeObjects.executeInTransaction(new TransactionCallback<RepositoryCommitMapping>()
        {
            @Override
            public RepositoryCommitMapping doInTransaction()
            {
                commit.put(RepositoryDomainMapping.DOMAIN, domain.getId());
                return activeObjects.create(RepositoryCommitMapping.class, commit);
            }

        });
    }

    @Override
    public RepositoryCommitMapping getCommit(Repository domain, int pullRequesCommitId)
    {
        return activeObjects.get(RepositoryCommitMapping.class, pullRequesCommitId);
    }


    @Override
    public RepositoryCommitMapping getCommitByNode(Repository domain, int pullRequestId, String node)
    {
        Query query = Query
                .select()
                .alias(RepositoryCommitMapping.class, "cm")
                .alias(RepositoryPullRequestToCommitMapping.class, "pr2cm")
                .alias(RepositoryPullRequestMapping.class, "PR")
                .join(RepositoryPullRequestToCommitMapping.class,
                        "cm.ID = pr2cm." + RepositoryPullRequestToCommitMapping.COMMIT)
                .join(RepositoryPullRequestMapping.class,
                        "pr2cm." + RepositoryPullRequestToCommitMapping.REQUEST_ID + " = PR.ID")
                .where("cm." + RepositoryDomainMapping.DOMAIN + " = ? AND PR.ID"
                        + " = ? AND cm." + RepositoryCommitMapping.NODE
                        + " = ?", domain.getId(), pullRequestId, node);

        RepositoryCommitMapping[] found = activeObjects.find(RepositoryCommitMapping.class, query);
        return found.length == 1 ? found[0] : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepositoryCommitMapping getCommitByNode(Repository repository, String node)
    {
        Query query = Query.select().where(RepositoryCommitMapping.DOMAIN + " = ? AND " + RepositoryCommitMapping.NODE + " = ?",
                repository.getId(), node);

        RepositoryCommitMapping[] found = activeObjects.find(RepositoryCommitMapping.class, query);
        if (found.length == 0)
        {
            return null;

        } else if (found.length == 1)
        {
            return found[0];

        } else
        {
            throw new IllegalStateException("Multiple commits for a same Commit Node and Repository ID. Repository ID: "
                    + repository.getId() + " Commit Node: " + node);

        }
    }

    @Override
    public PullRequestParticipantMapping[] getParticipants(final int pullRequestId)
    {
        PullRequestParticipantMapping[] result = activeObjects.find(PullRequestParticipantMapping.class, Query.select().where(PullRequestParticipantMapping.PULL_REQUEST_ID + " = ?", pullRequestId));

        return result;
    }

    @Override
    public void removeParticipant(final PullRequestParticipantMapping participantMapping)
    {
        LOGGER.debug("deleting participant with id = [ {} ]", participantMapping.getID());

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                activeObjects.delete(participantMapping);
                return null;
            }
        });
    }

    @Override
    public void saveParticipant(final PullRequestParticipantMapping participantMapping)
    {
        LOGGER.debug("saving participant with id = [ {} ]", participantMapping.getID());

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                participantMapping.save();
                return null;
            }
        });
    }

    @Override
    public void createParticipant(final int pullRequestId, final int repositoryId, final Participant participant)
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(PullRequestParticipantMapping.USERNAME, participant.getUsername());
        params.put(PullRequestParticipantMapping.APPROVED, participant.isApproved());
        params.put(PullRequestParticipantMapping.ROLE, participant.getRole());
        params.put(PullRequestParticipantMapping.PULL_REQUEST_ID, pullRequestId);
        params.put(PullRequestParticipantMapping.DOMAIN, repositoryId);
        activeObjects.create(PullRequestParticipantMapping.class, params);
    }

    @Override
    public int getNumberOfIssueKeysToPullRequests()
    {
        Query query = Query.select("ISSUE_KEY")
                .from(RepositoryPullRequestIssueKeyMapping.class);

        return activeObjects.count(RepositoryPullRequestIssueKeyMapping.class, query);
    }

    public boolean forEachIssueKeyMapping(final Organization organization, final Repository repository, final int pageSize, IssueToMappingFunction closure)
    {
        int currentPage = 0;
        RepositoryPullRequestIssueKeyMapping[] mappings;
        boolean result;
        final int repositoryId = repository.getId();

        do
        {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            Query issueQuery = Query.select()
                    .from(RepositoryPullRequestIssueKeyMapping.class)
                    .alias(RepositoryPullRequestIssueKeyMapping.class, "prik")
                    .alias(RepositoryPullRequestMapping.class, "pr")
                    .join(RepositoryPullRequestMapping.class, "prik.PULL_REQUEST_ID = pr.ID")
                    .alias(RepositoryMapping.class, "rm")
                    .join(RepositoryMapping.class, "rm.ID = pr.TO_REPOSITORY_ID")
                    .where("rm.ID = ?", repositoryId)
                    .limit(pageSize)
                    .offset(currentPage * pageSize);

            mappings = activeObjects.find(RepositoryPullRequestIssueKeyMapping.class, issueQuery);
            currentPage++;

            ImmutableSet.Builder<String> setBuilder = ImmutableSet.builder();

            for (RepositoryPullRequestIssueKeyMapping mapping : mappings)
            {
                setBuilder.add(mapping.getIssueKey());
            }

            final ImmutableSet<String> issueKeys = setBuilder.build();
            result = closure.execute(organization.getDvcsType(), repositoryId, issueKeys);
            LOGGER.info("processing page {} with this many elements {} took {} and had the result {}",
                    new Object[] { currentPage, issueKeys.size(), stopWatch, result });
        }
        while (mappings.length > 0 && result);
        return result;
    }

    /**
     * Returns a RepositoryPullRequestMapping as a map of attributes.
     */
    private Map<String, Object> asMap(RepositoryPullRequestMapping mapping)
    {
        Map<String, Object> attributes = Maps.newHashMap();

        //noinspection UnnecessaryBoxing
        attributes.put(RepositoryPullRequestMapping.REMOTE_ID, mapping.getRemoteId());
        attributes.put(RepositoryPullRequestMapping.NAME, mapping.getName());
        attributes.put(RepositoryPullRequestMapping.URL, mapping.getUrl());
        attributes.put(RepositoryPullRequestMapping.TO_REPO_ID, mapping.getToRepositoryId());
        attributes.put(RepositoryPullRequestMapping.AUTHOR, mapping.getAuthor());
        attributes.put(RepositoryPullRequestMapping.EXECUTED_BY, mapping.getExecutedBy());
        attributes.put(RepositoryPullRequestMapping.CREATED_ON, mapping.getCreatedOn());
        attributes.put(RepositoryPullRequestMapping.UPDATED_ON, mapping.getUpdatedOn());
        attributes.put(RepositoryPullRequestMapping.DESTINATION_BRANCH, mapping.getDestinationBranch());
        attributes.put(RepositoryPullRequestMapping.SOURCE_BRANCH, mapping.getSourceBranch());
        attributes.put(RepositoryPullRequestMapping.LAST_STATUS, mapping.getLastStatus());
        attributes.put(RepositoryPullRequestMapping.SOURCE_REPO, mapping.getSourceRepo());
        attributes.put(RepositoryPullRequestMapping.COMMENT_COUNT, mapping.getCommentCount());

        return attributes;
    }
}
