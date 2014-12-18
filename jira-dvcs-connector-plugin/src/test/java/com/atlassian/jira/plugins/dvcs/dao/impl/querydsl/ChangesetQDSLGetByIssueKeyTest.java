package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.activeobjects.DvcsConnectorTableNameConverter;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.java.ao.test.converters.NameConverters;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.atlassian.jira.plugins.dvcs.dao.impl.DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY;
import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils.SQL_IN_CLAUSE_MAX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * This is a database integration test that uses the AO database test parent class to provide us with a working database
 * and connection.
 */
@NameConverters (table = DvcsConnectorTableNameConverter.class)
public class ChangesetQDSLGetByIssueKeyTest extends QueryDSLDBTest
{
    @Test
    @NonTransactional
    public void testSimpleSearchMapsPropertly() throws Exception
    {
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(1));

        Changeset changeset = changeSets.get(0);
        assertThat(changeset.getNode(), equalTo(changesetMappingWithIssue.getNode()));
        assertThat(changeset.getFileDetails().size(), equalTo(0));
        assertThat(changeset.getRawAuthor(), equalTo(changesetMappingWithIssue.getRawAuthor()));
        assertThat(changeset.getAuthor(), equalTo(changesetMappingWithIssue.getAuthor()));
        assertThat(changeset.getDate(), equalTo(changesetMappingWithIssue.getDate()));
        assertThat(changeset.getRawNode(), equalTo(changesetMappingWithIssue.getRawNode()));
        assertThat(changeset.getBranch(), equalTo(changesetMappingWithIssue.getBranch()));
        assertThat(changeset.getMessage(), equalTo(changesetMappingWithIssue.getMessage()));
        assertThat(changeset.getParents().size(), equalTo(0));
        assertThat(changeset.getAllFileCount(), equalTo(changesetMappingWithIssue.getFileCount()));
        assertThat(changeset.getAuthorEmail(), equalTo(changesetMappingWithIssue.getAuthorEmail()));
        assertThat(changeset.getVersion(), equalTo(changesetMappingWithIssue.getVersion()));
        assertThat(changeset.isSmartcommitAvaliable(), equalTo(changesetMappingWithIssue.isSmartcommitAvailable()));
    }

    @Test
    @NonTransactional
    public void testMultipleIssueKeys() throws Exception
    {
        final String secondKey = "TST-1";
        changesetAOPopulator.associateToIssue(changesetMappingWithIssue, secondKey);
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY, secondKey), BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(1));
        assertThat(changeSets.get(0).getIssueKeys(), containsInAnyOrder(ISSUE_KEY, secondKey));
    }

    @Test
    @NonTransactional
    public void testMultipleChangesets() throws Exception
    {
        ChangesetMapping olderChangeset = createOlderChangeset();
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, true);

        assertThat(changeSets.size(), equalTo(2));
        assertThat(changeSets.get(0).getId(), equalTo(changesetMappingWithIssue.getID()));
        assertThat(changeSets.get(1).getId(), equalTo(olderChangeset.getID()));
    }

    @Test
    @NonTransactional
    public void testMultipleChangesetsWithSortingOldestFirst() throws Exception
    {
        ChangesetMapping olderChangeset = createOlderChangeset();
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(2));
        assertThat(changeSets.get(0).getId(), equalTo(olderChangeset.getID()));
        assertThat(changeSets.get(1).getId(), equalTo(changesetMappingWithIssue.getID()));
    }

    private ChangesetMapping createOlderChangeset()
    {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(changesetMappingWithIssue.getDate());
        calendar.add(Calendar.YEAR, -1);
        Date olderDate = calendar.getTime();
        final ImmutableMap<String, Object> secondCSParams = ImmutableMap.<String, Object>of(
                ChangesetMapping.NODE, "ecd732b3f41ad7ac501ef8408931fe1f80ab2921",
                ChangesetMapping.DATE, olderDate);
        return changesetAOPopulator.createCSM(secondCSParams, ISSUE_KEY, enabledRepository);
    }

    @Test
    @NonTransactional
    public void testMultipleRepository() throws Exception
    {
        RepositoryMapping secondRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);
        ChangesetMapping secondMapping = changesetAOPopulator.createCSM("ecd732b3f41ad7ac501ef8408931fe1f80ab2921", ISSUE_KEY, secondRepository);
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(2));

        Collection<Integer> returnedIds = extractIds(changeSets);

        assertThat(returnedIds, containsInAnyOrder(changesetMappingWithIssue.getID(), secondMapping.getID()));
    }

    @Test
    @NonTransactional
    public void testMultipleRepositoryWithFork() throws Exception
    {
        RepositoryMapping secondRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);
        changesetAOPopulator.associateToRepository(changesetMappingWithIssue, secondRepository);
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(1));

        assertThat(changeSets.get(0).getRepositoryIds(), containsInAnyOrder(enabledRepository.getID(), secondRepository.getID()));
    }

    @Test
    @NonTransactional
    public void testSingleChangeSetMultipleIssueAndRepository() throws Exception
    {
        RepositoryMapping secondRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);
        changesetAOPopulator.associateToRepository(changesetMappingWithIssue, secondRepository);

        final String secondKey = "TST-1";
        changesetAOPopulator.associateToIssue(changesetMappingWithIssue, secondKey);

        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY, secondKey), BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(1));
        assertThat(changeSets.get(0).getRepositoryIds(), containsInAnyOrder(enabledRepository.getID(), secondRepository.getID()));
        assertThat(changeSets.get(0).getIssueKeys(), containsInAnyOrder(ISSUE_KEY, secondKey));
    }

    @Test
    @NonTransactional
    public void testTwoChangeSetMultipleIssueAndRepositoryMaintainsOrder() throws Exception
    {
        ChangesetMapping secondChangeset = createOlderChangeset();

        RepositoryMapping secondRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);
        changesetAOPopulator.associateToRepository(changesetMappingWithIssue, secondRepository);
        changesetAOPopulator.associateToRepository(secondChangeset, secondRepository);

        final String secondKey = "TST-1";
        changesetAOPopulator.associateToIssue(changesetMappingWithIssue, secondKey);
        changesetAOPopulator.associateToIssue(secondChangeset, secondKey);

        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(2));
        assertThat(changeSets.get(0).getId(), equalTo(secondChangeset.getID()));
        assertThat(changeSets.get(1).getId(), equalTo(changesetMappingWithIssue.getID()));
    }

    @Test
    @NonTransactional
    public void testMultipleRepositoryOneDisabled() throws Exception
    {
        RepositoryMapping secondRepository = repositoryAOPopulator.createRepository(bitbucketOrganization, true, false);
        changesetAOPopulator.createCSM("ecd732b3f41ad7ac501ef8408931fe1f80ab2921", ISSUE_KEY, secondRepository);
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(1));
        assertThat(changeSets.get(0).getId(), equalTo(changesetMappingWithIssue.getID()));
    }

    @Test
    @NonTransactional
    public void testMultipleOrganization() throws Exception
    {
        OrganizationMapping secondOrganization = organizationAOPopulator.create("bogus");
        RepositoryMapping secondRepository = repositoryAOPopulator.createEnabledRepository(secondOrganization);
        changesetAOPopulator.createCSM("ecd732b3f41ad7ac501ef8408931fe1f80ab2921", ISSUE_KEY, secondRepository);
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(1));
        assertThat(changeSets.get(0).getId(), equalTo(changesetMappingWithIssue.getID()));
    }

    @Test
    @NonTransactional
    public void testMultipleEverything() throws Exception
    {
        // Changeset for a different DVCS Type
        OrganizationMapping bogusOrganization = organizationAOPopulator.create("bogus");
        RepositoryMapping repositoryForBogusOrg = repositoryAOPopulator.createEnabledRepository(bogusOrganization);
        changesetAOPopulator.createCSM("ecd732b3f41ad7ac501ef8408931fe1f80ab2921", ISSUE_KEY, repositoryForBogusOrg);

        // Second changeset in this org, separate repository
        RepositoryMapping secondEnabledRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);
        ChangesetMapping secondMapping = changesetAOPopulator.createCSM("a3d91a6bdf0e59dbc5b793baa2b4a289c91fd931", ISSUE_KEY, secondEnabledRepository);
        changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        // Disabled repository in this org
        RepositoryMapping thirdDisabledRepository = repositoryAOPopulator.createRepository(bitbucketOrganization, true, false);
        changesetAOPopulator.createCSM("0b137d202a56b712f4ef326e9900c7bc4d0835c6", ISSUE_KEY, thirdDisabledRepository);

        // Another CS in this repo
        ChangesetMapping secondCSInFirstRepo = changesetAOPopulator.createCSM("9bd67f04ab3ff831741e3edb7ff8edfa5623cd93", ISSUE_KEY, enabledRepository);

        // Some other random CS that is unrelated
        changesetAOPopulator.createCSM("721101938287c5dfcdc56b35a210761f6bc5d4ba", "TTT-222", enabledRepository);

        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(3));

        Collection<Integer> returnedIds = extractIds(changeSets);

        assertThat(returnedIds, containsInAnyOrder(changesetMappingWithIssue.getID(), secondMapping.getID(), secondCSInFirstRepo.getID()));
    }

    @Test
    @NonTransactional
    public void testAtInLimit() throws Exception
    {
        runMultipleChangeSetTest(SQL_IN_CLAUSE_MAX);
    }

    @Test
    @NonTransactional
    public void testOneMoreThanInLimit() throws Exception
    {
        runMultipleChangeSetTest(SQL_IN_CLAUSE_MAX + 1);
    }

    @Test
    @NonTransactional
    public void testMultipleOverInLimit() throws Exception
    {
        runMultipleChangeSetTest(SQL_IN_CLAUSE_MAX * 3 + 1);
    }

    private void runMultipleChangeSetTest(int number)
    {
        final ArrayList<String> issueKeys = new ArrayList<String>();
        final String projectKey = "NOT-";
        for (int i = 0; i < number; i++)
        {
            final String issueKey = projectKey + i;
            issueKeys.add(issueKey);
            ChangesetMapping changeset = changesetAOPopulator.createCSM("f" + i, issueKey, enabledRepository);
            changeset.setDate(new Date());
            changeset.save();
        }
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(issueKeys, BITBUCKET, false);

        int targetNumber = number > MAXIMUM_ENTITIES_PER_ISSUE_KEY ? MAXIMUM_ENTITIES_PER_ISSUE_KEY : number;

        assertThat(changeSets.size(), equalTo(targetNumber));
    }

    @Test
    @NonTransactional
    public void testSortingTooManyChangesetsForSingleNewestFirst() throws Exception
    {
        final String issueKey = "NNN-1";
        final int number = MAXIMUM_ENTITIES_PER_ISSUE_KEY * 2;
        Calendar calendar = Calendar.getInstance();
        List<ChangesetMapping> createdChangesets = new ArrayList<ChangesetMapping>();
        for (int i = 0; i < number; i++)
        {
            ChangesetMapping changeset = changesetAOPopulator.createCSM("f" + i, issueKey, enabledRepository);
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            changeset.setDate(calendar.getTime());
            changeset.save();
            createdChangesets.add(changeset);
        }
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ImmutableList.of(issueKey), BITBUCKET, true);

        assertThat(changeSets.size(), equalTo(MAXIMUM_ENTITIES_PER_ISSUE_KEY));

        for (int i = 0; i < MAXIMUM_ENTITIES_PER_ISSUE_KEY; i++)
        {
            assertThat(changeSets.get(i).getId(), equalTo(createdChangesets.get(i).getID()));
        }
    }

    @Test
    @NonTransactional
    public void testSortingTooManyChangesetsForSingleNewestLast() throws Exception
    {
        final String issueKey = "NNN-1";
        final int number = MAXIMUM_ENTITIES_PER_ISSUE_KEY * 2;
        Calendar calendar = Calendar.getInstance();
        List<ChangesetMapping> createdChangesets = new ArrayList<ChangesetMapping>();
        for (int i = 0; i < number; i++)
        {
            ChangesetMapping changeset = changesetAOPopulator.createCSM("f" + i, issueKey, enabledRepository);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            changeset.setDate(calendar.getTime());
            changeset.save();
            createdChangesets.add(changeset);
        }
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ImmutableList.of(issueKey), BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(MAXIMUM_ENTITIES_PER_ISSUE_KEY));

        for (int i = 0; i < MAXIMUM_ENTITIES_PER_ISSUE_KEY; i++)
        {
            assertThat(changeSets.get(i).getId(), equalTo(createdChangesets.get(i).getID()));
        }
    }

    @Test
    @NonTransactional
    public void testUnlinkedOrganization() throws Exception
    {
        enabledRepository.setLinked(false);
        enabledRepository.save();
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(0));
    }

    @Test
    @NonTransactional
    public void testDeletedOrganization() throws Exception
    {
        enabledRepository.setDeleted(true);
        enabledRepository.save();
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(0));
    }

    @Test
    @NonTransactional
    public void testWrongDvcsType() throws Exception
    {
        bitbucketOrganization.setDvcsType("bogus");
        bitbucketOrganization.save();
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(0));
    }

    @Test
    @NonTransactional
    public void testNoDvcsType() throws Exception
    {
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, null, false);

        assertThat(changeSets.size(), equalTo(1));
    }
}
