package com.atlassian.jira.plugins.dvcs.dao.impl.queryDSL;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.querydsl.v3.QChangesetMapping;
import com.atlassian.pocketknife.api.querydsl.ConnectionProvider;
import com.atlassian.pocketknife.api.querydsl.DialectProvider;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.internal.querydsl.QueryFactoryImpl;
import com.atlassian.pocketknife.spi.querydsl.DefaultDialectConfiguration;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import net.java.ao.RawEntity;
import net.java.ao.atlassian.AtlassianTableNameConverter;
import net.java.ao.atlassian.TablePrefix;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.test.ActiveObjectsIntegrationTest;
import net.java.ao.test.converters.NameConverters;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.sql.Connection;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.3
 */
@NameConverters (table = ChangesetQDSLDBTest.TestCreateTableTableNameConverter.class)
public class ChangesetQDSLDBTest extends ActiveObjectsIntegrationTest
{

    @Test
    @NonTransactional
    public void testAutoIncrement() throws Exception
    {
        entityManager.migrate(ChangesetMapping.class);

        ChangesetMapping csm = entityManager.create(ChangesetMapping.class);

        System.out.println(getTableName(ChangesetMapping.class));

        assertNotNull(csm);

        System.out.println(csm.getID());

        ConnectionProvider connectionProvider = new TestConnectionProvider(entityManager);

        final DialectProvider dialectProvider = new DefaultDialectConfiguration(connectionProvider);
        QueryFactory queryFactory = new QueryFactoryImpl(connectionProvider, dialectProvider);


        final Connection connection = connectionProvider.borrowConnection();

        try
        {
            SQLQuery select = queryFactory.select(connection);
            QChangesetMapping mappingInstance = new QChangesetMapping("CSM", "", QChangesetMapping.AO_TABLE_NAME);
            SQLQuery sql = select.from(mappingInstance);
            List<Tuple> result = sql.list(mappingInstance.ID, mappingInstance.NODE, mappingInstance.PARENTS_DATA);

            StringBuilder resultBuilder = new StringBuilder("result is: \n");

            for (Tuple tuple : result)
            {
                String resultLine = String.format("result is %s, %s, %s", new Object[] {
                        tuple.get(mappingInstance.ID), tuple.get(mappingInstance.NODE),
                        tuple.get(mappingInstance.PARENTS_DATA)
                });

                resultBuilder.append(resultLine);
                resultBuilder.append("\nParents:");

                String parents = tuple.get(mappingInstance.PARENTS_DATA);
                System.out.println("parents" + parents);
            }
            System.out.println(resultBuilder.toString());
        }
        finally
        {
            connectionProvider.returnConnection(connection);
        }
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
