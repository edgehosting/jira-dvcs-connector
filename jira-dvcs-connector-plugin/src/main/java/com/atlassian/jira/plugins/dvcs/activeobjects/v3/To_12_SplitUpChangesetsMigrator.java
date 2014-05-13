package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import net.java.ao.DatabaseProvider;
import net.java.ao.Entity;
import net.java.ao.EntityManager;
import net.java.ao.Query;
import net.java.ao.schema.TableNameConverter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import javax.wsdl.Types;

/**
 * Realizes migration of issue key and project key from one table: {@link ChangesetMapping} into the {@link ChangesetMapping} and a
 * {@link IssueToChangesetMapping}.
 * 
 * For more information see BBC-415.
 * 
 * @author stanislav-dvorscak
 * 
 */
// suppress deprecation - we want to have migrators stable as much as possible
@SuppressWarnings("deprecation")
public class To_12_SplitUpChangesetsMigrator implements ActiveObjectsUpgradeTask
{

    /**
     * Size of window for batch processing - commit window.
     */
    // too big value has not so big performance benefit,
    // it was also tested with 32768 (opposite to 8192), and there were less than 5% time benefit
    private static final int COMMIT_BATCH_SIZE = 8192;

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(To_12_SplitUpChangesetsMigrator.class);

    /**
     * @see #getModelVersion()
     */
    private static final ModelVersion MODEL_VERSION = ModelVersion.valueOf("12");

    /**
     * Reference to injected {@link ActiveObjects}
     */
    private ActiveObjects activeObjects;

    /**
     * Counts and logs progress information
     */
    private Progress progress;

    /**
     * Reference to entity manager.
     */
    private EntityManager entityManager;

    /**
     * Reference to database provider.
     */
    private DatabaseProvider databaseProvider;

    /**
     * JDBC SQL connection.
     */
    private Connection connection;

    /**
     * Table name generator strategy.
     */
    private TableNameConverter tableNameConverter;

    /**
     * Contains quote string, which helps to escape reserved words - identifier. Necessary also by Postgres as prevention in lower casing of
     * column names.
     */
    private String quote;

    /**
     * View of over JDBC result.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private class ChangesetResult
    {

        /**
         * @see ChangesetResult#ChangesetResult(int, String, String, int, String, String)
         */
        private final int id;

        /**
         * @see ChangesetResult#ChangesetResult(int, String, String, int, String, String)
         */
        private final String rawNode;

        /**
         * @see ChangesetResult#ChangesetResult(int, String, String, int, String, String)
         */
        private final String node;

        /**
         * @see ChangesetResult#ChangesetResult(int, String, String, int, String, String)
         */
        private final int repositoryId;

        /**
         * @see ChangesetResult#ChangesetResult(int, String, String, int, String, String)
         */
        private final String projectKey;

        /**
         * @see ChangesetResult#ChangesetResult(int, String, String, int, String, String)
         */
        private final String issueKey;

        /**
         * Constructor.
         * 
         * @param id
         *            {@link ChangesetMapping#getID()}
         * @param rawNode
         *            {@link ChangesetMapping#getRawNode()}
         * @param node
         *            {@link ChangesetMapping#getNode()}
         * @param repositoryId
         *            {@link ChangesetMapping#getRepositoryId()}
         * @param projectKey
         *            {@link ChangesetMapping#getProjectKey()}
         * @param issueKey
         *            {@link ChangesetMapping#getIssueKey()}
         */
        public ChangesetResult(int id, String rawNode, String node, int repositoryId, String projectKey, String issueKey)
        {
            this.id = id;
            this.rawNode = rawNode;
            this.node = node;
            this.repositoryId = repositoryId;
            this.projectKey = projectKey;
            this.issueKey = issueKey;
        }

    }

    /**
     * Makes cursor/mapping between JDBC and {@link ChangesetResult}.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private class ChangesetResultCursor implements Iterator<ChangesetResult>
    {

        /**
         * @see ChangesetResultCursor#ChangesetResultCursor(ResultSet)
         */
        private final ResultSet resultSet;

        /**
         * @see #hasNext()
         */
        private boolean hasNext;

        /**
         * Constructor.
         * 
         * @param resultSet
         */
        public ChangesetResultCursor(ResultSet resultSet)
        {
            this.resultSet = resultSet;
            try
            {
                hasNext = resultSet.next();
            } catch (SQLException e)
            {
                throw new RuntimeException(e);
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext()
        {
            return hasNext;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ChangesetResult next()
        {
            try
            {
                ChangesetResult result = new ChangesetResult( //
                        resultSet.getInt(1), // id
                        resultSet.getString(2), // raw node
                        resultSet.getString(3), // node
                        resultSet.getInt(4), // repository id
                        resultSet.getString(5), // project key
                        resultSet.getString(6) // issue key
                );
                hasNext = resultSet.next();
                return result;

            } catch (SQLException e)
            {
                throw new RuntimeException(e);

            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Calculates and logs progress of migration - only for logging purposes.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private class Progress
    {

        /**
         * count of all entities for processing
         */
        private final int totalCount;

        /**
         * @see count of current proceed entities
         */
        int currentCount;

        /**
         * Constructor.
         * 
         * @param totalCount
         *            how much entities are awaiting.
         */
        public Progress(int totalCount)
        {
            this.totalCount = totalCount;
        }

        /**
         * Update progress by provided count of proceed entities.
         * 
         * @param proceedCount
         */
        private void update(int proceedCount)
        {
            if (totalCount > 0)
            {
                currentCount += proceedCount;
                logger.info(currentCount + " from " + totalCount + " [" + currentCount * 100 / totalCount
                        + "%] entities have been already processed");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelVersion getModelVersion()
    {
        return MODEL_VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void upgrade(ModelVersion currentVersion, final ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;

        logger.info("upgrade [ " + getModelVersion() + " ]: started");

        // initializes migration process
        try
        {
            activeObjects.migrate(OrganizationMapping.class, RepositoryMapping.class, ChangesetMapping.class,
                    IssueToChangesetMapping.class, RepositoryToChangesetMapping.class);

            if (init())
            {
                sanityClean();

                // global parameters
                int totalCount = activeObjects.count(ChangesetMapping.class,
                        Query.select().where(ChangesetMapping.ISSUE_KEY + " is not null "));
                int readBatchSize = Math.max(COMMIT_BATCH_SIZE, totalCount / 4); // at least commit batch size, or a quarter of total
                                                                                 // size

                // prepares progress bar
                this.progress = new Progress(totalCount);
                progress.update(0);

                ChangesetResult uniqueChangeset = null;
                // batch processing - until everything will be proceed
                while (true)
                {
                    // finds next batch for processing
                    Statement batchStatement = connection.createStatement();
                    batchStatement.setMaxRows(readBatchSize);
                    ResultSet founded = batchStatement.executeQuery(newBatchSQL());

                    ChangesetResultCursor changesetCursor = new ChangesetResultCursor(founded);

                    // end condition <=> nothing else for processing
                    if (!changesetCursor.hasNext())
                    {
                        break;
                    }

                    //
                    uniqueChangeset = processBatch(changesetCursor, uniqueChangeset);
                    batchStatement.close();
                }
            }

            logger.info("upgrade [ " + getModelVersion() + " ]: finished");

        } catch (SQLException e)
        {
            if (e.getNextException() != null)
            {
                logger.error("Next exception of statement was: ", e.getNextException());
            }
            throw new RuntimeException(e);

        } finally
        {
            try
            {
                if (connection != null)
                {
                    connection.close();
                }

            } catch (SQLException e)
            {
                // silently ignored
            }
        }

    }

    /**
     * Removes all changesets pointed to on an un-existing repository.
     * 
     * @throws SQLException
     */
    private void sanityClean() throws SQLException
    {
        Statement sanityStatement = connection.createStatement();
        sanityStatement.executeUpdate(//
                "delete from " + table(ChangesetMapping.class) //
                        + " where (" + column(ChangesetMapping.REPOSITORY_ID) + " != 0 " //
                        + " and " + column(ChangesetMapping.REPOSITORY_ID) + " not in (" //
                        + " select " + column("ID") + " from " + table(RepositoryMapping.class) //
                        + " )) or " + column(ChangesetMapping.REPOSITORY_ID) + " is null ");
        sanityStatement.close();
        connection.commit();
    }

    /**
     * Processes provided batch.
     * 
     * @param changesetCursor
     *            cursor over batch
     * @param uniqueChangeset
     *            last known unique changeset
     * @return new last known unique changeset
     * @throws SQLException
     */
    private ChangesetResult processBatch(ChangesetResultCursor changesetCursor, ChangesetResult uniqueChangeset) throws SQLException
    {
        // until whole batch was proceed
        do
        {

            // counts proceed rows
            int count = 0;

            // prepares batch statements
            PreparedStatement markAsUpdatedStatement = newMarkAsUpdatedStatement();
            PreparedStatement deleteChangesetStatement = newDeleteChangesetStatement();
            PreparedStatement issueToChangesetStatement = newIssueToChangesetStatement();
            PreparedStatement repositoryToChangesetStatement = newRepositoryToChangesetStatement();

            while (changesetCursor.hasNext() && count < COMMIT_BATCH_SIZE)
            {
                // next batch item
                ChangesetResult current = changesetCursor.next();
                count++;

                // hack - github commits are using only node, not raw nodes :(
                String currentNode = resolveChangesetNode(current.rawNode, current.node);
                String uniqueChangesetNode = null;

                // false if current changeset is consider to be unique, otherwise it is duplicate
                boolean isDuplicate = false;

                // restart processing - tries to find previous proceed changeset, or null if it is first attempt
                if (uniqueChangeset == null)
                {
                    uniqueChangeset = findUniqueChangesetAfterRestart(activeObjects, currentNode);
                }

                if (uniqueChangeset != null)
                {
                	uniqueChangesetNode = resolveChangesetNode(uniqueChangeset.rawNode, uniqueChangeset.node);
                	if (StringUtils.isBlank(uniqueChangesetNode))
                	{
                		logger.warn("The changeset with no hash found, it will be deleted.");
                		addDeleteChangesetStatement(deleteChangesetStatement, uniqueChangeset);
                		continue;
                	
                	} else if (currentNode.equals(uniqueChangesetNode))
                    {
                		isDuplicate = true;
                    }                
                }

                // if it is unique changeset - it updates cursor for current valid unique changeset
                if (!isDuplicate)
                {
                    uniqueChangeset = current;
                }

                // skips non-existing issues
                if (current.projectKey != null && !"NON_EXISTING".equals(current.projectKey))
                {
                    addIssueToChnagesetStatement(issueToChangesetStatement, uniqueChangeset, current);
                }

                // repository relation is added on unique changeset
                addRepositoryToChangesetStatement(repositoryToChangesetStatement, uniqueChangeset, current);

                // if current changeset is not unique, means duplicated than will be removed
                if (isDuplicate)
                {
                    addDeleteChangesetStatement(deleteChangesetStatement, current);

                } else
                {
                    addMarkAsUpdatedStatement(markAsUpdatedStatement, uniqueChangeset);
                }

            }

            // execute update inside transaction
            boolean rollback = true;
            try
            {
                issueToChangesetStatement.executeBatch();
                issueToChangesetStatement.close();

                markAsUpdatedStatement.executeBatch();
                markAsUpdatedStatement.close();

                deleteChangesetStatement.executeBatch();
                deleteChangesetStatement.close();

                repositoryToChangesetStatement.executeBatch();
                repositoryToChangesetStatement.close();

                connection.commit();
                rollback = false;

            } finally
            {
                if (rollback)
                {
                    connection.rollback();
                }
            }

            progress.update(count);

        } while (changesetCursor.hasNext());

        return uniqueChangeset;
    }

    /**
     * 
     * @param rawNode
     *            {@link ChangesetMapping#getRawNode()}
     * @param node
     *            {@link ChangesetMapping#getNode()}
     * @return resolved node - not blank string - raw node takes precedence - hack for Git entries, which at this state do not fill raw node
     */
    private String resolveChangesetNode(String rawNode, String node)
    {
        if (StringUtils.isBlank(rawNode))
        {
            return node;
        } else
        {
            return rawNode;
        }
    }

    /**
     * It is responsible to find unique changeset after restart. It means to find changeset, which was already proceed for current node.
     * 
     * @param activeObjects
     * @param currentNode
     * @return founded unique changeset
     */
    private ChangesetResult findUniqueChangesetAfterRestart(final ActiveObjects activeObjects, String currentNode)
    {
        ChangesetMapping[] founded = activeObjects.find(
                ChangesetMapping.class,
                Query.select().where(
                        ChangesetMapping.ISSUE_KEY + " is null AND ( " + ChangesetMapping.RAW_NODE + " = ? OR " + ChangesetMapping.NODE
                                + " = ? )", currentNode, currentNode));

        if (founded.length == 1)
        {
            ChangesetMapping result = founded[0];
            return new ChangesetResult( //
                    result.getID(), //
                    result.getRawNode(), //
                    result.getNode(), //
                    result.getRepositoryId(), //
                    result.getProjectKey(), //
                    result.getIssueKey() //
            );

        } else if (founded.length == 0)
        {
            return null;

        } else
        {
            throw new RuntimeException("It should never happened - there are multiple proceed changesets with the same raw node: "
                    + currentNode);
        }
    }

    /**
     * @return should migration continue? is there something for processing?
     * @throws SQLException
     */
    private boolean init() throws SQLException
    {
        Query query = Query.select().where(ChangesetMapping.ISSUE_KEY + " is not null ");
        query.setLimit(1);
        ChangesetMapping[] founded = activeObjects.find(ChangesetMapping.class, query);

        if (founded.length != 1)
        {
            return false;
        }

        this.entityManager = founded[0].getEntityManager();
        this.tableNameConverter = entityManager.getTableNameConverter();
        this.databaseProvider = entityManager.getProvider();
        this.connection = databaseProvider.getConnection();
        this.connection.setAutoCommit(false);
        this.quote = connection.getMetaData().getIdentifierQuoteString();

        return true;
    }

    /**
     * @return SQL which finds out new batch for processing.
     */
    private String newBatchSQL()
    {
        return "select " //
                + StringUtils.join(new String[] { //
                        column("ID"), //
                                column(ChangesetMapping.RAW_NODE), //
                                column(ChangesetMapping.NODE), //
                                column(ChangesetMapping.REPOSITORY_ID), //
                                column(ChangesetMapping.PROJECT_KEY), //
                                column(ChangesetMapping.ISSUE_KEY) //
                        }, ", ") //
                + " from " + table(ChangesetMapping.class) //
                + " where " + column(ChangesetMapping.PROJECT_KEY) + " is not null and " //
                + column(ChangesetMapping.ISSUE_KEY) + " is not null " //
                + " order by " + column(ChangesetMapping.RAW_NODE) + ", " + column(ChangesetMapping.NODE) + ", " + column("ID") + " ASC";
    }

    /**
     * @return new Batch statement - which adds relation between Issue and appropriate Changeset.
     * @throws SQLException
     */
    private PreparedStatement newIssueToChangesetStatement() throws SQLException
    {
        String sql = "insert into " + table(IssueToChangesetMapping.class) //
                // fields
                + " (" //
                + StringUtils.join(new String[] { //
                        column(IssueToChangesetMapping.CHANGESET_ID), //
                                column(IssueToChangesetMapping.PROJECT_KEY), //
                                column(IssueToChangesetMapping.ISSUE_KEY) //
                        }, ", ") //
                + ") "
                // values
                + "values (?, ?, ?)";

        return connection.prepareStatement(sql);
    }

    /**
     * Fills {@link #newIssueToChangesetStatement()} by parameters for next batch part.
     * 
     * @param statement
     *            get by {@link #newIssueToChangesetStatement()}
     * @param unique
     *            on which changeset
     * @param current
     *            from which changeset
     * @throws SQLException
     */
    private void addIssueToChnagesetStatement(PreparedStatement statement, ChangesetResult unique, ChangesetResult current)
            throws SQLException
    {
        statement.setInt(1, unique.id);
        statement.setString(2, current.projectKey);
        statement.setString(3, current.issueKey);
        statement.addBatch();
    }

    /**
     * @return new Batch statement - which adds relation between Repository and appropriate Changeset.
     * @throws SQLException
     */
    private PreparedStatement newRepositoryToChangesetStatement() throws SQLException
    {
        String sql = "insert into " + table(RepositoryToChangesetMapping.class) //
                // fields
                + " (" //
                + StringUtils.join(new String[] { //
                        column(RepositoryToChangesetMapping.CHANGESET_ID), //
                                column(RepositoryToChangesetMapping.REPOSITORY_ID) //
                        }, ", ") //
                + ") "
                // values
                + "values (?, ?)";
        return connection.prepareStatement(sql);
    }

    /**
     * Fills {@link #newRepositoryToChangesetStatement()} by parameters for next batch part.
     * 
     * @param statement
     *            get by {@link #newRepositoryToChangesetStatement()}
     * @param unique
     *            to which changeset
     * @param current
     *            from which changeset
     * @throws SQLException
     */
    private void addRepositoryToChangesetStatement(PreparedStatement statement, ChangesetResult unique, ChangesetResult current)
            throws SQLException
    {
        statement.setInt(1, unique.id);
        statement.setInt(2, current.repositoryId);
        statement.addBatch();
    }

    /**
     * @return Creates batch statement, which is able to delete single {@link ChangesetMapping}.
     * @throws SQLException
     */
    private PreparedStatement newDeleteChangesetStatement() throws SQLException
    {
        String sql = "delete from " + table(ChangesetMapping.class) + " where " + column("ID") + " = ?";
        return connection.prepareStatement(sql);
    }

    /**
     * Fills {@link #newDeleteChangesetStatement()} by parameters for next batch part.
     * 
     * @param statement
     *            get by {@link #newDeleteChangesetStatement()}
     * @param current
     *            which changeset
     * @throws SQLException
     */
    private void addDeleteChangesetStatement(PreparedStatement statement, ChangesetResult current) throws SQLException
    {
        statement.setInt(1, current.id);
        statement.addBatch();
    }

    /**
     * @return Creates batch statement, which marks changeset as proceed.
     * @throws SQLException
     */
    private PreparedStatement newMarkAsUpdatedStatement() throws SQLException
    {
        String sql = "update " + table(ChangesetMapping.class) + " set " //
                + column(ChangesetMapping.REPOSITORY_ID) + " = ?, " //
                + column(ChangesetMapping.PROJECT_KEY) + " = ?, " //
                + column(ChangesetMapping.ISSUE_KEY) + " = ? " //
                + " where " + column("ID") + " = ? ";
        return connection.prepareStatement(sql);
    }

    /**
     * Fills {@link #newIssueToChangesetStatement()} by parameters for next batch.
     * 
     * @param statement
     * @param unique
     * @throws SQLException
     */
    private void addMarkAsUpdatedStatement(PreparedStatement statement, ChangesetResult unique) throws SQLException
    {
        statement.setInt(1, 0);
        statement.setNull(2, Types.STRING_TYPE);
        statement.setNull(3, Types.STRING_TYPE);
        statement.setInt(4, unique.id);
        statement.addBatch();
    }

    /**
     * Creates name of table for provided entity. The name is also sanitized.
     * 
     * @param entity
     *            for which database entity
     * @return name of table
     */
    private String table(Class<? extends Entity> entity)
    {
        String tableName = tableNameConverter.getName(entity);
        return databaseProvider.withSchema(tableName);
    }

    /**
     * Sanitized provided name of column.
     * 
     * @param columnName
     *            which will be sanitized
     * @return sanitized column
     */
    private String column(String columnName)
    {
        if (!StringUtils.isBlank(quote))
        {
            return quote + columnName + quote;
        } else
        {
            return columnName;
        }
    }

}
