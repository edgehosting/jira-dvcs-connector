package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchHeadMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToBranchMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class BranchDaoImpl implements BranchDao
{
    private static final Logger log = LoggerFactory.getLogger(BranchDaoImpl.class);

    private final ActiveObjects activeObjects;

    @Autowired
    public BranchDaoImpl(@ComponentImport ActiveObjects activeObjects)
    {
        this.activeObjects = checkNotNull(activeObjects);
    }

    @Override
    public List<BranchHead> getBranchHeads(int repositoryId)
    {
        BranchHeadMapping[] result = activeObjects.find(BranchHeadMapping.class, Query.select().where(BranchHeadMapping.REPOSITORY_ID + " = ?", repositoryId));

        return Lists.transform(Arrays.asList(result), new Function<BranchHeadMapping, BranchHead>()
        {
            @Override
            public BranchHead apply(BranchHeadMapping input)
            {
                return new BranchHead(input.getBranchName(), input.getHead());
            }
        });
    }

    @Override
    public List<Branch> getBranches(final int repositoryId)
    {
        BranchMapping[] result = activeObjects.find(BranchMapping.class, Query.select().where(BranchMapping.REPOSITORY_ID + " = ?", repositoryId));

        return Lists.transform(Arrays.asList(result), new Function<BranchMapping, Branch>()
        {
            @Override
            public Branch apply(BranchMapping input)
            {
                return new Branch(input.getID(), input.getName(), repositoryId);
            }
        });
    }

    @Override
    public void createBranch(final int repositoryId, final Branch branch, final Set<String> issueKeys)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                log.debug("adding branch {} for repository with id = [ {} ]", new Object[] { branch, repositoryId });
                final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
                map.put(BranchMapping.REPOSITORY_ID, repositoryId);
                map.put(BranchMapping.NAME, ActiveObjectsUtils.stripToLimit(branch.getName(), 255));

                BranchMapping branchMapping = activeObjects.create(BranchMapping.class, map);
                associateBranchToIssue(branchMapping, issueKeys);
                return null;
            }
        });
    }

    @Override
    public void createBranchHead(final int repositoryId, final BranchHead branchHead)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                log.debug("adding branch head {} for repository with id = [ {} ]", new Object[] { branchHead, repositoryId });
                final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
                map.put(BranchHeadMapping.REPOSITORY_ID, repositoryId);
                map.put(BranchHeadMapping.BRANCH_NAME, ActiveObjectsUtils.stripToLimit(branchHead.getName(), 255));
                map.put(BranchHeadMapping.HEAD, branchHead.getHead());

                activeObjects.create(BranchHeadMapping.class, map);
                return null;
            }
        });
    }

    @Override
    public void removeBranchHead(final int repositoryId, final BranchHead branch)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                log.debug("deleting branch head {} for repository with id = [ {} ]", new Object[] { branch, repositoryId });
                Query query = Query.select().where(BranchHeadMapping.REPOSITORY_ID + " = ? AND "
                        + BranchHeadMapping.BRANCH_NAME + " = ? AND "
                        + BranchHeadMapping.HEAD + " = ?", repositoryId, branch.getName(), branch.getHead());
                ActiveObjectsUtils.delete(activeObjects, BranchHeadMapping.class, query);
                return null;
            }
        });
    }

    @Override
    public void removeBranch(final int repositoryId, final Branch branch)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                log.debug("deleting branch mapping for branch with name {} and repository with id = [ {} ]", new Object[] { branch.getName(), repositoryId });

                // delete association issues - branch
                Query query = Query.select()
                        .from(IssueToBranchMapping.class)
                        .alias(IssueToBranchMapping.class, "mapping")
                        .alias(BranchMapping.class, "branch")
                        .join(BranchMapping.class, "mapping." + IssueToBranchMapping.BRANCH_ID + " = branch.ID")
                        .where("branch." + BranchMapping.NAME + " = ? and branch." + BranchMapping.REPOSITORY_ID + " = ?", branch.getName(), repositoryId);

                ActiveObjectsUtils.delete(activeObjects, IssueToBranchMapping.class, query);

                log.debug("deleting branch {} for repository with id = [ {} ]", new Object[] { branch, repositoryId });
                query = Query.select().where(BranchMapping.REPOSITORY_ID + " = ? AND "
                        + BranchMapping.NAME + " = ?", repositoryId, branch.getName());
                ActiveObjectsUtils.delete(activeObjects, BranchMapping.class, query);

                return null;
            }
        });
    }

    @Override
    public void removeAllBranchesInRepository(final int repositoryId)
    {
        log.debug("deleting branches for repository with id = [ {} ]", repositoryId);

        // delete association issues - branch
        Query query = Query.select()
                .from(IssueToBranchMapping.class)
                .alias(IssueToBranchMapping.class, "mapping")
                .alias(BranchMapping.class, "branch")
                .join(BranchMapping.class, "mapping." + IssueToBranchMapping.BRANCH_ID + " = branch.ID")
                .where("branch." + BranchMapping.REPOSITORY_ID + " = ?", repositoryId);

        ActiveObjectsUtils.delete(activeObjects, IssueToBranchMapping.class, query);

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                Query query = Query.select().where(BranchMapping.REPOSITORY_ID + " = ?", repositoryId);

                ActiveObjectsUtils.delete(activeObjects, BranchMapping.class, query);
                return null;
            }
        });

    }

    @Override
    public void removeAllBranchHeadsInRepository(final int repositoryId)
    {
        log.debug("deleting branch heads for repository with id = [ {} ]", repositoryId);

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                Query query = Query.select().where(BranchHeadMapping.REPOSITORY_ID + " = ?", repositoryId);
                ActiveObjectsUtils.delete(activeObjects, BranchHeadMapping.class, query);
                return null;
            }
        });
    }

    @Override
    public List<Branch> getBranchesForIssue(final Iterable<String> issueKeys)
    {
        final String baseWhereClause = ActiveObjectsUtils.renderListOperator("mapping." + IssueToBranchMapping.ISSUE_KEY, "IN", "OR", issueKeys);
        final Object[] params = ObjectArrays.concat(new Object[] { Boolean.FALSE, Boolean.TRUE }, Iterables.toArray(issueKeys, Object.class), Object.class);

        final List<BranchMapping> branches = activeObjects.executeInTransaction(new TransactionCallback<List<BranchMapping>>()
        {
            @Override
            public List<BranchMapping> doInTransaction()
            {
                BranchMapping[] mappings = activeObjects.find(BranchMapping.class,
                        Query.select()
                                .alias(IssueToBranchMapping.class, "mapping")
                                .alias(BranchMapping.class, "branch")
                                .alias(RepositoryMapping.class, "repo")
                                .join(IssueToBranchMapping.class, "mapping." + IssueToBranchMapping.BRANCH_ID + " = branch.ID")
                                .join(RepositoryMapping.class, "branch." + BranchMapping.REPOSITORY_ID + " = repo.ID")
                                .where("repo." + RepositoryMapping.DELETED + " = ? AND repo." + RepositoryMapping.LINKED + " = ? AND " + baseWhereClause, params));

                return Arrays.asList(mappings);
            }
        });

        return Lists.transform(branches, new Function<BranchMapping, Branch>()
        {
            @Override
            public Branch apply(BranchMapping input)
            {
                return new Branch(input.getID(), input.getName(), input.getRepository().getID());
            }
        });
    }

    @Override
    public List<Branch> getBranchesForIssue(final Iterable<String> issueKeys, final String dvcsType)
    {
        final String baseWhereClause = ActiveObjectsUtils.renderListOperator("mapping." + IssueToBranchMapping.ISSUE_KEY, "IN", "OR", issueKeys);
        final Object[] params = ObjectArrays.concat(new Object[] { dvcsType, Boolean.FALSE, Boolean.TRUE }, Iterables.toArray(issueKeys, Object.class), Object.class);

        final List<BranchMapping> branches = activeObjects.executeInTransaction(new TransactionCallback<List<BranchMapping>>()
        {
            @Override
            public List<BranchMapping> doInTransaction()
            {
                BranchMapping[] mappings = activeObjects.find(BranchMapping.class,
                        Query.select()
                                .alias(IssueToBranchMapping.class, "mapping")
                                .alias(BranchMapping.class, "branch")
                                .alias(RepositoryMapping.class, "repo")
                                .alias(OrganizationMapping.class, "org")
                                .join(IssueToBranchMapping.class, "mapping." + IssueToBranchMapping.BRANCH_ID + " = branch.ID")
                                .join(RepositoryMapping.class, "branch." + BranchMapping.REPOSITORY_ID + " = repo.ID")
                                .join(OrganizationMapping.class, "repo." + RepositoryMapping.ORGANIZATION_ID + " = org.ID")
                                .where("org." + OrganizationMapping.DVCS_TYPE + " = ? AND repo." + RepositoryMapping.DELETED + " = ? AND repo." + RepositoryMapping.LINKED + " = ? AND " + baseWhereClause,
                                        params));

                return Arrays.asList(mappings);
            }
        });

        return Lists.transform(branches, new Function<BranchMapping, Branch>()
        {
            @Override
            public Branch apply(BranchMapping input)
            {
                return new Branch(input.getID(), input.getName(), input.getRepository().getID());
            }
        });
    }

    @Override
    public List<Branch> getBranchesForRepository(final int repositoryId)
    {
        final List<BranchMapping> branches = activeObjects.executeInTransaction(new TransactionCallback<List<BranchMapping>>()
        {
            @Override
            public List<BranchMapping> doInTransaction()
            {
                BranchMapping[] mappings = activeObjects.find(BranchMapping.class,
                        Query.select()
                                .from(BranchMapping.class)
                                .alias(BranchMapping.class, "branch")
                                .alias(RepositoryMapping.class, "repo")
                                .join(RepositoryMapping.class, "branch." + BranchMapping.REPOSITORY_ID + " = repo.ID")
                                .where("repo." + RepositoryMapping.DELETED + " = ? AND repo." + RepositoryMapping.LINKED + " = ? AND branch." + BranchMapping.REPOSITORY_ID + " = ?", Boolean.FALSE, Boolean.TRUE, repositoryId));

                return Arrays.asList(mappings);
            }
        });

        return Lists.transform(branches, new Function<BranchMapping, Branch>()
        {
            @Override
            public Branch apply(BranchMapping input)
            {
                return new Branch(input.getID(), input.getName(), input.getRepository().getID());
            }
        });
    }

    private void associateBranchToIssue(BranchMapping branchMapping, Set<String> extractedIssues)
    {
        // remove all assoc issues-branch
        Query query = Query.select().where(IssueToBranchMapping.BRANCH_ID + " = ? ", branchMapping);
        ActiveObjectsUtils.delete(activeObjects, IssueToBranchMapping.class, query);

        // insert all
        for (String extractedIssue : extractedIssues)
        {
            final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
            map.put(IssueToBranchMapping.ISSUE_KEY, extractedIssue);
            map.put(IssueToBranchMapping.BRANCH_ID, branchMapping.getID());
            activeObjects.create(IssueToBranchMapping.class, map);
        }
    }
}
