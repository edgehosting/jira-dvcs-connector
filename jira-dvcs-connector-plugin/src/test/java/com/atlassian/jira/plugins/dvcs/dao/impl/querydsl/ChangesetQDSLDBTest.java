package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.activeobjects.ChangesetAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.OrganizationAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.RepositoryAOPopulator;
import com.atlassian.jira.plugins.dvcs.activeobjects.TestConnectionProvider;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
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
import net.java.ao.test.jdbc.Data;
import org.junit.Before;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;

@Data ()
public abstract class ChangesetQDSLDBTest extends ActiveObjectsIntegrationTest
{
    protected static final String ISSUE_KEY = "QDSL-1";
    protected static final ArrayList<String> ISSUE_KEYS = Lists.newArrayList(ISSUE_KEY);

    protected ChangesetAOPopulator changesetAOPopulator;
    protected RepositoryAOPopulator repositoryAOPopulator;
    protected OrganizationAOPopulator organizationAOPopulator;

    protected ConnectionProvider connectionProvider;
    protected QueryFactory queryFactory;
    protected SchemaProvider schemaProvider;

    protected ChangesetQDSL changesetQDSL;

    protected ChangesetMapping changesetMappingWithIssue;
    protected RepositoryMapping enabledRepository;
    protected OrganizationMapping bitbucketOrganization;

    @Before
    public void setup() throws SQLException
    {
        changesetAOPopulator = new ChangesetAOPopulator(entityManager);
        repositoryAOPopulator = new RepositoryAOPopulator(entityManager);
        organizationAOPopulator = new OrganizationAOPopulator(entityManager);

        connectionProvider = new TestConnectionProvider(entityManager);

        final DialectProvider dialectProvider = new DefaultDialectConfiguration(connectionProvider);
        queryFactory = new QueryFactoryImpl(connectionProvider, dialectProvider);

        schemaProvider = new DefaultSchemaProvider(connectionProvider);

        entityManager.migrateDestructively(ChangesetMapping.class, RepositoryToChangesetMapping.class,
                RepositoryMapping.class, OrganizationMapping.class, IssueToChangesetMapping.class);

        changesetQDSL = new ChangesetQDSL(queryFactory, schemaProvider);

        bitbucketOrganization = organizationAOPopulator.create(BITBUCKET);

        enabledRepository = repositoryAOPopulator.createEnabledRepository(bitbucketOrganization);

        changesetMappingWithIssue = changesetAOPopulator.createCSM(changesetAOPopulator.getDefaultCSParams(), ISSUE_KEY, enabledRepository);
    }

    protected Collection<Integer> extractIds(Collection<Changeset> changeSets)
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
