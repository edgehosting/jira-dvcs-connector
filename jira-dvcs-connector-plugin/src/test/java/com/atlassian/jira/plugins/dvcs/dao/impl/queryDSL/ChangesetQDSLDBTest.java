package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QOrganizationMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QRepositoryToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.DialectProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SelectQuery;
import com.atlassian.pocketknife.api.querydsl.StreamyResult;
import com.atlassian.pocketknife.internal.querydsl.QueryFactoryImpl;
import com.atlassian.pocketknife.spi.querydsl.DefaultDialectConfiguration;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import net.java.ao.RawEntity;
import net.java.ao.atlassian.AtlassianTableNameConverter;
import net.java.ao.atlassian.TablePrefix;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.test.ActiveObjectsIntegrationTest;
import net.java.ao.test.converters.NameConverters;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.*;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.3
 */
@NameConverters (table = ChangesetQDSLDBTest.TestCreateTableTableNameConverter.class)
public class ChangesetQDSLDBTest extends ActiveObjectsIntegrationTest
{
    private static final String ISSUE_KEY = "QDSL-1";

    private ConnectionProvider connectionProvider;
    private QueryFactory queryFactory;

    private ChangesetQDSL changesetQDSL;

    private ChangesetMapping changesetMappingWithIssue;
    private IssueToChangesetMapping issueToChangesetMapping;
    private RepositoryMapping enabledRepository;
    private OrganizationMapping bitbucketOrganization;

    @Before
    public void setup() throws SQLException
    {
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

        enabledRepository = entityManager.create(RepositoryMapping.class);
        enabledRepository.setDeleted(false);
        enabledRepository.setLinked(true);
        enabledRepository.setOrganizationId(bitbucketOrganization.getID());
        enabledRepository.save();

        changesetMappingWithIssue = entityManager.create(ChangesetMapping.class);
        changesetMappingWithIssue.save();

        issueToChangesetMapping = entityManager.create(IssueToChangesetMapping.class);
        issueToChangesetMapping.setIssueKey(ISSUE_KEY);
        issueToChangesetMapping.setChangeset(changesetMappingWithIssue);
        issueToChangesetMapping.save();

        Map<String, Object> rtcMapping = ImmutableMap.of(
                RepositoryToChangesetMapping.REPOSITORY_ID, (Object) enabledRepository.getID(),
                RepositoryToChangesetMapping.CHANGESET_ID, changesetMappingWithIssue.getID());
        RepositoryToChangesetMapping rtc = entityManager.create(RepositoryToChangesetMapping.class, rtcMapping);
    }

    @Test
    @NonTransactional
    public void testSimpleSearch() throws Exception
    {
        List<Changeset> changeSets = changesetQDSL.getByIssueKey(Lists.newArrayList(ISSUE_KEY), BITBUCKET, false);

        assertThat(changeSets.size(), equalTo(1));

//        final QChangesetMapping changesetMapping = new QChangesetMapping("CSM", "", QChangesetMapping.AO_TABLE_NAME);
//        final QRepositoryToChangesetMapping rtcMapping = new QRepositoryToChangesetMapping("RTC", "", QRepositoryToChangesetMapping.AO_TABLE_NAME);
//        final QRepositoryMapping repositoryMapping = new QRepositoryMapping("REPO", "", QRepositoryMapping.AO_TABLE_NAME);
//        final QOrganizationMapping orgMapping = new QOrganizationMapping("ORG", "", QOrganizationMapping.AO_TABLE_NAME);
//
//        final Connection connection = connectionProvider.borrowConnection();
//
//        try
//        {
//            SQLQuery select = queryFactory.select(connection).from(changesetMapping)
//                    .join(rtcMapping).on(changesetMapping.ID.eq(rtcMapping.CHANGESET_ID))
//                    .join(repositoryMapping).on(repositoryMapping.ID.eq(rtcMapping.REPOSITORY_ID))
//                    .join(orgMapping).on(orgMapping.ID.eq(repositoryMapping.ORGANIZATION_ID))
//                    .where(
//                            repositoryMapping.DELETED.eq(false)
//                                    .and(repositoryMapping.LINKED.eq(true))
//                                    .and(orgMapping.DVCS_TYPE.eq(BITBUCKET))
//                                    .and(changesetMapping.ISSUE_KEY.in(Lists.newArrayList(ISSUE_KEY))));
//
//            List<Tuple> res = select.list(changesetMapping.FILE_DETAILS_JSON,
//                    repositoryMapping.ID,
//                    changesetMapping.NODE,
//                    changesetMapping.RAW_AUTHOR,
//                    changesetMapping.AUTHOR,
//                    changesetMapping.DATE,
//                    changesetMapping.RAW_NODE,
//                    changesetMapping.BRANCH,
//                    changesetMapping.MESSAGE,
//                    changesetMapping.PARENTS_DATA,
//                    changesetMapping.FILE_COUNT,
//                    changesetMapping.AUTHOR_EMAIL,
//                    changesetMapping.ID,
//                    changesetMapping.VERSION,
//                    changesetMapping.SMART_COMMIT_AVAILABLE);
//
//
//            assertThat(res.size(), equalTo(1));
//        }
//        finally
//        {
//            connectionProvider.returnConnection(connection);
//        }

//        entityManager.migrate(ChangesetMapping.class);
//
//
//        ChangesetMapping csm = entityManager.create(ChangesetMapping.class);
//
//        System.out.println(getTableName(ChangesetMapping.class));
//
//        assertNotNull(csm);
//
//        System.out.println(csm.getID());
//
//
//        final Connection connection = connectionProvider.borrowConnection();
//
//        try
//        {
//            SQLQuery select = queryFactory.select(connection);
//            QChangesetMapping mappingInstance = new QChangesetMapping("CSM", "", QChangesetMapping.AO_TABLE_NAME);
//            SQLQuery sql = select.from(mappingInstance);
//            List<Tuple> result = sql.list(mappingInstance.ID, mappingInstance.NODE, mappingInstance.PARENTS_DATA);
//
//            StringBuilder resultBuilder = new StringBuilder("result is: \n");
//
//            for (Tuple tuple : result)
//            {
//                String resultLine = String.format("result is %s, %s, %s", new Object[] {
//                        tuple.get(mappingInstance.ID), tuple.get(mappingInstance.NODE),
//                        tuple.get(mappingInstance.PARENTS_DATA)
//                });
//
//                resultBuilder.append(resultLine);
//                resultBuilder.append("\nParents:");
//
//                String parents = tuple.get(mappingInstance.PARENTS_DATA);
//                System.out.println("parents" + parents);
//            }
//            System.out.println(resultBuilder.toString());
//        }
//        finally
//        {
//            connectionProvider.returnConnection(connection);
//        }
    }

    public static final class TestCreateTableTableNameConverter implements TableNameConverter
    {
        private final TableNameConverter delegate;

        public TestCreateTableTableNameConverter()
        {
            delegate = new AtlassianTableNameConverter(new TestPrefix());
        }

        @Override
        public String getName(Class<? extends RawEntity<?>> clazz)
        {
            return delegate.getName(clazz);
        }
    }

    public static final class TestPrefix implements TablePrefix
    {
        public String prepend(String string)
        {
            return new StringBuilder().append("AO_E8B6CC_").append(string).toString();
        }
    }
}
