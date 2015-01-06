package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.activeobjects.BranchAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.ChangesetAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.DvcsConnectorTableNameConverter;
import com.atlassian.jira.plugins.dvcs.activeobjects.OrganizationAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.PullRequestAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.RepositoryAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.TestConnectionProvider;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToBranchMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.BranchDaoImpl;
import com.atlassian.jira.plugins.dvcs.dao.impl.ChangesetDaoImpl;
import com.atlassian.jira.plugins.dvcs.dao.impl.QueryDslFeatureHelper;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.DialectProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.atlassian.pocketknife.internal.querydsl.DefaultSchemaProvider;
import com.atlassian.pocketknife.internal.querydsl.QueryFactoryImpl;
import com.atlassian.pocketknife.spi.querydsl.DefaultDialectConfiguration;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import net.java.ao.test.ActiveObjectsIntegrationTest;
import net.java.ao.test.converters.NameConverters;
import org.junit.Before;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.mockito.Mockito.when;

@NameConverters (table = DvcsConnectorTableNameConverter.class)
public abstract class QueryDSLDatabaseTest extends ActiveObjectsIntegrationTest
{
    protected static final String ISSUE_KEY = "QDSL-1";
    protected static final ArrayList<String> ISSUE_KEYS = Lists.newArrayList(ISSUE_KEY);

    protected ChangesetAOPopulator changesetAOPopulator;
    protected RepositoryAOPopulator repositoryAOPopulator;
    protected OrganizationAOPopulator organizationAOPopulator;
    protected PullRequestAOPopulator pullRequestAOPopulator;
    protected BranchAOPopulator branchAOPopulator;

    protected ConnectionProvider connectionProvider;
    protected QueryFactory queryFactory;
    protected SchemaProvider schemaProvider;

    protected ChangesetDaoQueryDsl changesetDaoQueryDsl;
    protected PullRequestDaoQueryDsl pullRequestDaoQueryDsl;
    protected BranchDaoQueryDsl branchDaoQueryDsl;

    protected RepositoryMapping enabledRepository;
    protected OrganizationMapping bitbucketOrganization;

    protected ChangesetMapping changesetMappingWithIssue;
    protected RepositoryPullRequestMapping pullRequestMappingWithIssue;
    protected PullRequestParticipantMapping pullRequestParticipant;
    protected BranchMapping branchMappingWithIssue;
    protected ChangesetDaoImpl changesetDao;
    protected QueryDslFeatureHelper queryDslFeatureHelper;
    protected BranchDaoImpl branchDao;

    @Before
    public void setup() throws SQLException
    {
        changesetAOPopulator = new ChangesetAOPopulator(entityManager);
        repositoryAOPopulator = new RepositoryAOPopulator(entityManager);
        organizationAOPopulator = new OrganizationAOPopulator(entityManager);
        pullRequestAOPopulator = new PullRequestAOPopulator(entityManager);
        branchAOPopulator = new BranchAOPopulator(entityManager);

        connectionProvider = new TestConnectionProvider(entityManager);

        final DialectProvider dialectProvider = new DefaultDialectConfiguration(connectionProvider);
        queryFactory = new QueryFactoryImpl(connectionProvider, dialectProvider);

        schemaProvider = new DefaultSchemaProvider(connectionProvider);

        entityManager.migrateDestructively(ChangesetMapping.class, RepositoryToChangesetMapping.class,
                RepositoryMapping.class, OrganizationMapping.class, IssueToChangesetMapping.class,
                RepositoryPullRequestMapping.class, PullRequestParticipantMapping.class,
                RepositoryPullRequestIssueKeyMapping.class, BranchMapping.class, IssueToBranchMapping.class);

        queryDslFeatureHelper = Mockito.mock(QueryDslFeatureHelper.class);
        when(queryDslFeatureHelper.isRetrievalUsingQueryDSLEnabled()).thenReturn(true);

        changesetDao = Mockito.mock(ChangesetDaoImpl.class);
        changesetDaoQueryDsl = new ChangesetDaoQueryDsl(queryFactory, schemaProvider, changesetDao, queryDslFeatureHelper);
        pullRequestDaoQueryDsl = new PullRequestDaoQueryDsl(queryFactory, schemaProvider);
        branchDao = Mockito.mock(BranchDaoImpl.class);
        branchDaoQueryDsl = new BranchDaoQueryDsl(queryFactory, schemaProvider, branchDao, queryDslFeatureHelper);

        bitbucketOrganization = organizationAOPopulator.create(BITBUCKET);

        enabledRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);

        changesetMappingWithIssue = changesetAOPopulator.createCSM(changesetAOPopulator.getDefaultCSParams(), ISSUE_KEY, enabledRepository);

        pullRequestMappingWithIssue = pullRequestAOPopulator.createPR("PR FOR" + ISSUE_KEY, ISSUE_KEY, enabledRepository);
        pullRequestParticipant = pullRequestAOPopulator.createParticipant("hoo", false, "reviewer", pullRequestMappingWithIssue);
        pullRequestMappingWithIssue = entityManager.get(RepositoryPullRequestMapping.class, pullRequestMappingWithIssue.getID());

        branchMappingWithIssue = branchAOPopulator.createBranch("something branch", ISSUE_KEY, enabledRepository);
    }

    final protected Collection<Integer> extractIds(Collection<Changeset> changeSets)
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
