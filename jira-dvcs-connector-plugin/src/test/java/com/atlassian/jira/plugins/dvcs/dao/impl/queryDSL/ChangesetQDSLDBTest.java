package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.jira.plugins.dvcs.activeobjects.ChangesetAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.DvcsConnectorTableNameConverter;
import com.atlassian.jira.plugins.dvcs.activeobjects.OrganizationAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.RepositoryAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.DialectProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.internal.querydsl.QueryFactoryImpl;
import com.atlassian.pocketknife.spi.querydsl.DefaultDialectConfiguration;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.java.ao.test.ActiveObjectsIntegrationTest;
import net.java.ao.test.converters.NameConverters;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * This is a database integration test that uses the AO database test parent class to provide us with a working
 * database and connection.
 */
@NameConverters (table = DvcsConnectorTableNameConverter.class)
public class ChangesetQDSLDBTest extends ActiveObjectsIntegrationTest
{
    private static final String ISSUE_KEY = "QDSL-1";

    private ChangesetAOPopulator changesetAOPopulator;
    private RepositoryAOPopulator repositoryAOPopulator;
    private OrganizationAOPopulator organizationAOPopulator;

    private ConnectionProvider connectionProvider;
    private QueryFactory queryFactory;

    private ChangesetQDSL changesetQDSL;

    private ChangesetMapping changesetMappingWithIssue;
    private RepositoryMapping enabledRepository;
    private OrganizationMapping bitbucketOrganization;

    @Before
    public void setup() throws SQLException
    {
        changesetAOPopulator = new ChangesetAOPopulator(entityManager);
        repositoryAOPopulator = new RepositoryAOPopulator(entityManager);
        organizationAOPopulator = new OrganizationAOPopulator(entityManager);

        connectionProvider = new TestConnectionProvider(entityManager);

        final DialectProvider dialectProvider = new DefaultDialectConfiguration(connectionProvider);
        queryFactory = new QueryFactoryImpl(connectionProvider, dialectProvider);

        changesetQDSL = new ChangesetQDSL(connectionProvider, queryFactory);

        entityManager.migrateDestructively(ChangesetMapping.class, RepositoryToChangesetMapping.class,
                RepositoryMapping.class, OrganizationMapping.class, IssueToChangesetMapping.class);

        changesetQDSL = new ChangesetQDSL(connectionProvider, queryFactory);

        bitbucketOrganization = entityManager.create(OrganizationMapping.class);
        bitbucketOrganization.setDvcsType(BITBUCKET);
        bitbucketOrganization.save();

        enabledRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);

        changesetMappingWithIssue = changesetAOPopulator.createCSM(changesetAOPopulator.getDefaultCSParams(), ISSUE_KEY, enabledRepository);
    }

    @Test
    @NonTransactional
    public void testSimpleSearchMapsPropertly() throws Exception
    {
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

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
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

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
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

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
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

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
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

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
        changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

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

        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

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
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(0));
    }

    @Test
    @NonTransactional
    public void testDeletedOrganization() throws Exception
    {
        enabledRepository.setDeleted(true);
        enabledRepository.save();
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(0));
    }

    @Test
    @NonTransactional
    public void testWrongDvcsType() throws Exception
    {
        bitbucketOrganization.setDvcsType("bogus");
        bitbucketOrganization.save();
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(0));
    }

    @Test
    @NonTransactional
    public void testNoDvcsType() throws Exception
    {
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), null, false);

        assertThat(changeSets.size(), equalTo(1));
    }

    private Collection<Integer> extractIds(Collection<Changeset> changeSets)
    {
        return Collections2.transform(changeSets, new Function<Changeset, Integer>()
        {
            @Override
            public Integer apply(@Nullable final Changeset input)
            {
                return input.getId();
            }
        });
    }
}
