package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.google.common.collect.Lists;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * This is a database integration test that uses the AO database test parent class to provide us with a working database
 * and connection.
 */
public class BranchQDSLGetByIssueKeyTest extends ChangesetQDSLDBTest
{
    @Test
    @NonTransactional
    public void testSimpleSearchMapsProperly() throws Exception
    {
        List<Branch> branches = branchQDSL.getByIssueKeys(ISSUE_KEYS, BITBUCKET);

        assertThat(branches.size(), equalTo(1));

        Branch branch = branches.get(0);

        assertThat(branch.getRepositoryId(), equalTo(branchMappingWithIssue.getRepository().getID()));
        assertThat(branch.getName(), equalTo(branchMappingWithIssue.getName()));

        assertThat(branch.getIssueKeys(), containsInAnyOrder(ISSUE_KEY));
    }

    @Test
    @NonTransactional
    public void testTwoIssueKeys()
    {
        final String secondKey = "SCN-2";
        branchAOPopulator.associateToIssue(branchMappingWithIssue, secondKey);

        List<Branch> branches = branchQDSL.getByIssueKeys(Lists.newArrayList(ISSUE_KEY, secondKey), BITBUCKET);

        assertThat(branches.size(), equalTo(1));

        Branch Branch = branches.get(0);
        assertThat(Branch.getIssueKeys(), containsInAnyOrder(ISSUE_KEY, secondKey));
    }

    @Test
    @NonTransactional
    public void testSimpleSearchMapsProperlyAcrossRepositoryAndOrg() throws Exception
    {
        OrganizationMapping org2 = organizationAOPopulator.create("Github", "gitbhu.", "gh fork");
        RepositoryMapping repo2 = repositoryAOPopulator.createRepository(org2, false, true, "fh/fork");
        branchAOPopulator.createBranch(branchMappingWithIssue.getName(), "other key", repo2);

        List<Branch> Branchs = branchQDSL.getByIssueKeys(ISSUE_KEYS, BITBUCKET);

        assertThat(Branchs.size(), equalTo(1));
    }

    @Test
    @NonTransactional
    public void testWithTwoBranchesTwoKeys() throws Exception
    {
        final String secondIssueKey = "IK-2";
        branchAOPopulator.createBranch("something else", secondIssueKey, enabledRepository);

        List<Branch> Branchs = branchQDSL.getByIssueKeys(Arrays.asList(ISSUE_KEY, secondIssueKey), BITBUCKET);

        assertThat(Branchs.size(), equalTo(2));
    }
}
