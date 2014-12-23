package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.dao.impl.QueryDSLFeatureHelper;
import com.atlassian.jira.plugins.dvcs.dao.impl.querydsl.BranchQueryDSL;
import com.atlassian.jira.plugins.dvcs.event.BranchCreatedEvent;
import com.atlassian.jira.plugins.dvcs.event.DevSummaryChangedEvent;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.sync.impl.IssueKeyExtractor;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.Resource;

@Component
public class BranchServiceImpl implements BranchService
{
    @Resource
    private BranchDao branchDao;

    @Resource
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Resource
    private ThreadEvents threadEvents;

    @Resource
    private BranchQueryDSL branchQueryDSL;

    @Resource
    private QueryDSLFeatureHelper queryDSLFeatureHelper;

    private static final Logger log = LoggerFactory.getLogger(BranchServiceImpl.class);

    @Override
    public void removeAllBranchesInRepository(int repositoryId)
    {
        branchDao.removeAllBranchesInRepository(repositoryId);
    }

    @Override
    public void removeAllBranchHeadsInRepository(int repositoryId)
    {
        branchDao.removeAllBranchHeadsInRepository(repositoryId);
    }

    @Override
    public void updateBranches(final Repository repository, final List<Branch> newBranches)
    {
        // to remove possible branch duplicates
        Set<Branch> newBranchSet = new HashSet<Branch>(newBranches);

        List<Branch> oldBranches = branchDao.getBranches(repository.getId());
        Set<Branch> oldBranchesSet = removeDuplicatesIfNeeded(repository, oldBranches);

        for (Branch branch : newBranchSet)
        {
            if (!oldBranchesSet.contains(branch))
            {
                Set<String> issueKeys = IssueKeyExtractor.extractIssueKeys(branch.getName());
                branchDao.createBranch(repository.getId(), branch, issueKeys);

                broadcastBranchCreatedEvent(branch, issueKeys);
                threadEvents.broadcast(new DevSummaryChangedEvent(repository.getId(), repository.getDvcsType(), issueKeys));
            }
        }

        // Removing closed branches
        for (Branch oldBranch : oldBranchesSet)
        {
            if (!newBranchSet.contains(oldBranch))
            {
                branchDao.removeBranch(repository.getId(), oldBranch);
                Set<String> issueKeys = IssueKeyExtractor.extractIssueKeys(oldBranch.getName());
                threadEvents.broadcast(new DevSummaryChangedEvent(repository.getId(), repository.getDvcsType(), issueKeys));
            }
        }
    }

    private Set<Branch> removeDuplicatesIfNeeded(final Repository repository, final List<Branch> oldBranches)
    {
        Set<Branch> oldBranchesSet = new HashSet<Branch>(oldBranches);

        if (oldBranches.size() != oldBranchesSet.size())
        {
            log.info("Duplicate branches detected on repository '{}' [{}]", repository.getName(), repository.getId());
            Set<Branch> duplicates = findDuplicates(oldBranches);

            log.info("Removing duplicate branches ({}) on repository '{}'", duplicates.toString(), repository.getName());
            for (Branch branch : duplicates)
            {
                branchDao.removeBranch(repository.getId(), branch);
                log.info("Branch {} removed", branch);
            }
            oldBranchesSet.removeAll(duplicates);
        }

        return oldBranchesSet;
    }

    private Set<BranchHead> removeDuplicateBranchHeadIfNeeded(final Repository repository, final List<BranchHead> oldBranchHeads)
    {
        Set<BranchHead> oldBranchHeadsSet = new HashSet<BranchHead>(oldBranchHeads);
        if (oldBranchHeads.size() != oldBranchHeadsSet.size())
        {
            log.info("Duplicate branch heads detected on repository '{}' [{}]", repository.getName(), repository.getId());
            Set<BranchHead> duplicates = findDuplicates(oldBranchHeads);

            log.info("Removing duplicate branch heads ({}) on repository '{}'", duplicates.toString(), repository.getName());
            for (BranchHead branchHead : duplicates)
            {
                branchDao.removeBranchHead(repository.getId(), branchHead);
                log.info("Branch head {} removed", branchHead);
            }

            oldBranchHeadsSet.removeAll(duplicates);
        }

        return oldBranchHeadsSet;
    }

    private <T> Set<T> findDuplicates(List<T> list)
    {
        final Multiset<T> ms = HashMultiset.create(list);
        return Sets.filter(ms.elementSet(), new Predicate<T>()
        {
            @Override
            public boolean apply(@Nullable final T input)
            {
                return ms.count(input) > 1;
            }
        });
    }

    @Override
    public List<BranchHead> getListOfBranchHeads(Repository repository)
    {
        List<BranchHead> branchHeads = null;
        branchHeads = branchDao.getBranchHeads(repository.getId());
        return branchHeads;
    }

    @Override
    public void updateBranchHeads(Repository repository, List<Branch> newBranches, List<BranchHead> oldBranchHeads)
    {
        if (newBranches != null)
        {
            Set<BranchHead> oldBranchHeadsSet = removeDuplicateBranchHeadIfNeeded(repository, oldBranchHeads);

            Set<BranchHead> headAlreadyThere = new HashSet<BranchHead>();
            for (Branch branch : new HashSet<Branch>(newBranches))
            {
                for (BranchHead branchHead : branch.getHeads())
                {
                    if (oldBranchHeads == null || !oldBranchHeadsSet.contains(branchHead))
                    {
                        branchDao.createBranchHead(repository.getId(), branchHead);
                    }
                    else
                    {
                        headAlreadyThere.add(branchHead);
                    }
                }
            }
            // Removing old branch heads
            if (oldBranchHeads != null)
            {
                for (BranchHead oldBranchHead : oldBranchHeadsSet)
                {
                    if (!headAlreadyThere.contains(oldBranchHead))
                    {
                        branchDao.removeBranchHead(repository.getId(), oldBranchHead);
                    }
                }
            }
        }
    }

    @Override
    public List<Branch> getByIssueKey(Iterable<String> issueKeys)
    {
        return branchDao.getBranchesForIssue(issueKeys);
    }

    @Override
    public List<Branch> getByIssueKey(Iterable<String> issueKeys, String dvcsType)
    {
        if (queryDSLFeatureHelper.isRetrievalUsingQueryDSLEnabled())
        {
            return branchQueryDSL.getByIssueKeys(issueKeys, dvcsType);
        }
        return branchDao.getBranchesForIssue(issueKeys, dvcsType);
    }

    @Override
    public List<Branch> getForRepository(Repository repository)
    {
        return branchDao.getBranchesForRepository(repository.getId());
    }

    @Override
    public String getBranchUrl(Repository repository, Branch branch)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getBranchUrl(repository, branch);
    }

    private void broadcastBranchCreatedEvent(Branch branch, Set<String> issueKeys)
    {
        // unfortunately there is no way of figuring out the branch creation date
        threadEvents.broadcast(new BranchCreatedEvent(branch, issueKeys, new Date()));
    }
}
