package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.jira.plugins.dvcs.activeobjects.DvcsConnectorTableNameConverter;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.java.ao.test.converters.NameConverters;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * This is a database integration test that uses the AO database test parent class to provide us with a working database
 * and connection.
 */
@NameConverters (table = DvcsConnectorTableNameConverter.class)
public class ChangesetQDSLDB_getByIssueKeyTest extends ChangesetQDSLDBTest
{
    @Test
    @NonTransactional
    public void testSimpleSearchMapsPropertly() throws Exception
    {
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(1));

        Changeset changeset = changeSets.get(0);
        Map<String, Object> defaultValues = changesetAOPopulator.getDefaultCSParams();
        assertThat(changeset.getNode(), equalTo(defaultValues.get(ChangesetMapping.NODE)));
        assertThat(changeset.getFileDetails().size(), equalTo(0));
        assertThat(changeset.getRawAuthor(), equalTo(defaultValues.get(ChangesetMapping.RAW_AUTHOR)));
        assertThat(changeset.getAuthor(), equalTo(defaultValues.get(ChangesetMapping.AUTHOR)));
        // Too hard for now
//        assertThat(changeset.getDate(), equalTo(defaultValues.get(ChangesetMapping.DATE)));
        assertThat(changeset.getRawNode(), equalTo(defaultValues.get(ChangesetMapping.RAW_NODE)));
        assertThat(changeset.getBranch(), equalTo(defaultValues.get(ChangesetMapping.BRANCH)));
        assertThat(changeset.getMessage(), equalTo(defaultValues.get(ChangesetMapping.MESSAGE)));
        assertThat(changeset.getParents().size(), equalTo(0));
        assertThat(changeset.getAllFileCount(), equalTo(defaultValues.get(ChangesetMapping.FILE_COUNT)));
        assertThat(changeset.getAuthorEmail(), equalTo(defaultValues.get(ChangesetMapping.AUTHOR_EMAIL)));
        assertThat(changeset.getVersion(), equalTo(defaultValues.get(ChangesetMapping.VERSION)));
        assertThat(changeset.isSmartcommitAvaliable(), equalTo(defaultValues.get(ChangesetMapping.SMARTCOMMIT_AVAILABLE)));
    }

    @Test
    @NonTransactional
    public void testMultipleIssueKeys() throws Exception
    {
        final String secondKey = "TST-1";
        changesetAOPopulator.associateToIssue(changesetMappingWithIssue, secondKey);
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY, secondKey), BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(2));
        // Should return duplicates
        assertThat((new HashSet<Changeset>(changeSets)).size(), equalTo(1));

        Collection<Integer> returnedIds = extractIds(changeSets);

        assertThat(returnedIds, containsInAnyOrder(changesetMappingWithIssue.getID(), changesetMappingWithIssue.getID()));
    }

    @Test
    @NonTransactional
    public void testMultipleChangesets() throws Exception
    {
        ChangesetMapping secondMapping = changesetAOPopulator.createCSM(new HashMap<String, Object>(), ISSUE_KEY, enabledRepository);
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(2));

        Collection<Integer> returnedIds = extractIds(changeSets);

        assertThat(returnedIds, containsInAnyOrder(changesetMappingWithIssue.getID(), secondMapping.getID()));
    }

    @Test
    @NonTransactional
    public void testMultipleRepository() throws Exception
    {
        RepositoryMapping secondRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);
        final ImmutableMap<String, Object> csParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, "ecd732b3f41ad7ac501ef8408931fe1f80ab2921");
        ChangesetMapping secondMapping = changesetAOPopulator.createCSM(csParams, ISSUE_KEY, secondRepository);
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(2));

        Collection<Integer> returnedIds = extractIds(changeSets);

        assertThat(returnedIds, containsInAnyOrder(changesetMappingWithIssue.getID(), secondMapping.getID()));
    }

    @Test
    @NonTransactional
    public void testMultipleRepositoryOneDisabled() throws Exception
    {
        RepositoryMapping secondRepository = repositoryAOPopulator.createRepository(bitbucketOrganization, true, false);
        final ImmutableMap<String, Object> csParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, "ecd732b3f41ad7ac501ef8408931fe1f80ab2921");
        ChangesetMapping secondMapping = changesetAOPopulator.createCSM(csParams, ISSUE_KEY, secondRepository);
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
        final ImmutableMap<String, Object> csParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, "ecd732b3f41ad7ac501ef8408931fe1f80ab2921");
        ChangesetMapping secondMapping = changesetAOPopulator.createCSM(csParams, ISSUE_KEY, secondRepository);
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
        final ImmutableMap<String, Object> boguesCsParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, "ecd732b3f41ad7ac501ef8408931fe1f80ab2921");
        changesetAOPopulator.createCSM(boguesCsParams, ISSUE_KEY, repositoryForBogusOrg);

        // Second changeset in this org, separate repository
        RepositoryMapping secondEnabledRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);
        final ImmutableMap<String, Object> secondCsParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, "a3d91a6bdf0e59dbc5b793baa2b4a289c91fd931");
        ChangesetMapping secondMapping = changesetAOPopulator.createCSM(secondCsParams, ISSUE_KEY, secondEnabledRepository);
        changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        // Disabled repository in this org
        RepositoryMapping thirdDisabledRepository = repositoryAOPopulator.createRepository(bitbucketOrganization, true, false);
        final ImmutableMap<String, Object> disabledCsParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, "0b137d202a56b712f4ef326e9900c7bc4d0835c6");
        changesetAOPopulator.createCSM(disabledCsParams, ISSUE_KEY, thirdDisabledRepository);

        // Another CS in this repo
        final ImmutableMap<String, Object> secondCSInFirstRepoParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, "9bd67f04ab3ff831741e3edb7ff8edfa5623cd93");
        ChangesetMapping secondCSInFirstRepo = changesetAOPopulator.createCSM(secondCSInFirstRepoParams, ISSUE_KEY, enabledRepository);

        // Some other random CS that is unrelated
        final ImmutableMap<String, Object> thirdCSInFirstRepoParams = ImmutableMap.<String, Object>of(ChangesetMapping.NODE, "721101938287c5dfcdc56b35a210761f6bc5d4ba");
        changesetAOPopulator.createCSM(thirdCSInFirstRepoParams, "TTT-222", enabledRepository);

        List<Changeset> changeSets = changesetQDSL.getByIssueKey(ISSUE_KEYS, BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(3));

        Collection<Integer> returnedIds = extractIds(changeSets);

        assertThat(returnedIds, containsInAnyOrder(changesetMappingWithIssue.getID(), secondMapping.getID(), secondCSInFirstRepo.getID()));
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
