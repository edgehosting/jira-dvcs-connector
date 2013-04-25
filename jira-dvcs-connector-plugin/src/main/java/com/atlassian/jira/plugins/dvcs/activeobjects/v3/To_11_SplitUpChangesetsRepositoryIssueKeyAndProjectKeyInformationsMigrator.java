package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import java.util.HashMap;
import java.util.Map;

import net.java.ao.DBParam;
import net.java.ao.Query;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;

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
public class To_11_SplitUpChangesetsRepositoryIssueKeyAndProjectKeyInformationsMigrator implements ActiveObjectsUpgradeTask
{

    /**
     * Size of window for batch processing.
     */
    private static final int BATCH_SIZE = 8192;

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(To_11_SplitUpChangesetsRepositoryIssueKeyAndProjectKeyInformationsMigrator.class);

    /**
     * @see #getModelVersion()
     */
    private static final ModelVersion MODEL_VERSION = ModelVersion.valueOf("11");

    /**
     * Dummy class - necessary by transaction callback propagation.
     * 
     * @author Stanislav Dvorscak
     * 
     * @param <T>
     */
    private class Holder<T>
    {
        private ChangesetMapping value;
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
                        + "%] entities was already proceed");
            }
        }
    }

    /**
     * Process delta statistics, how much time was spent on several parts of program.
     * 
     * @author Stanislav Dvorscak
     * 
     * @param <E>
     *            type of part
     */
    private class DeltaTime<E extends Enum<?>>
    {

        /**
         * All parts of interest.
         */
        private final E[] parts;

        /**
         * When everything was started?
         */
        private final Long startTimestamp = System.currentTimeMillis();

        /**
         * Part to previous start timestamp.
         */
        private final Map<E, Long> partToPreviousTimestamp = new HashMap<E, Long>();

        /**
         * Part to spent time.
         */
        private final Map<E, Long> partToTime = new HashMap<E, Long>();

        /**
         * Constructors.
         * 
         * @param deltaTimeTypes
         *            all parts of interest
         */
        public DeltaTime(E[] deltaTimeTypes)
        {
            this.parts = deltaTimeTypes;
        }

        /**
         * Marks delta begin point.
         * 
         * @param part
         */
        public void begin(E part)
        {
            partToPreviousTimestamp.put(part, System.currentTimeMillis());
        }

        /**
         * Marks delta end point.
         * 
         * @param part
         */
        public void end(E part)
        {
            long deltaTime = System.currentTimeMillis() - partToPreviousTimestamp.get(part);
            Long previousTime = partToTime.get(part);
            partToTime.put(part, previousTime != null ? previousTime + deltaTime : deltaTime);
        }

        /**
         * Log statistics.
         */
        public void log()
        {
            long totalPartsTime = 0;
            long now = System.currentTimeMillis();

            for (Long partTime : partToTime.values())
            {
                totalPartsTime += partTime;
            }

            StringBuilder result = new StringBuilder();
            result.append("Delta time statistics:\n");
            for (E part : parts)
            {
                result.append(part.name().toLowerCase()).append(":\t").append(partToTime.get(part) * 100 / (now - startTimestamp))
                        .append("%\n");
            }

            result.append("total:\t").append((now - startTimestamp - totalPartsTime) * 100 / (now - startTimestamp)).append("%\n");

            logger.info(result.toString());
        }
    }

    /**
     * @see DeltaTime
     * @author Stanislav Dvorscak
     * 
     */
    enum DeltaTimePart
    {
        RETRIEVING, UPDATE, CACHE
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
    public void upgrade(ModelVersion currentVersion, final ActiveObjects ao)
    {
        logger.debug("upgrade [ " + getModelVersion() + " ]");

        ao.migrate(ChangesetMapping.class, IssueToChangesetMapping.class, RepositoryToChangesetMapping.class);

        final Holder<ChangesetMapping> uniqueChangeset = new Holder<ChangesetMapping>();

        final Progress progress = new Progress(ao.count(ChangesetMapping.class,
                Query.select().where(ChangesetMapping.ISSUE_KEY + " is not null ")));

        final DeltaTime<DeltaTimePart> deltaTime = new DeltaTime<DeltaTimePart>(DeltaTimePart.values());
        progress.update(0);

        // batch processing - until everything will be proceed
        while (true)
        {
            // finds next batch for processing
            Query query = Query.select().where(ChangesetMapping.ISSUE_KEY + " is not null ")
                    .order(ChangesetMapping.RAW_NODE + ", \"" + ChangesetMapping.NODE + "\", \"ID\" ASC ");
            query.setLimit(BATCH_SIZE);

            deltaTime.begin(DeltaTimePart.RETRIEVING);
            final ChangesetMapping[] founded = ao.find(ChangesetMapping.class, query);
            deltaTime.end(DeltaTimePart.RETRIEVING);

            // end condition - nothing else for processing <=> empty batch
            if (founded.length == 0)
            {
                break;
            }

            ao.executeInTransaction(new TransactionCallback<Void>()
            {

                @Override
                public Void doInTransaction()
                {
                    for (ChangesetMapping current : founded)
                    {
                        // skip it - it was already proceed
                        if (StringUtils.isBlank(current.getProjectKey()) || StringUtils.isBlank(current.getIssueKey()))
                        {
                            return null;
                        }

                        // hack - github commits are using only node, not raw nodes :(
                        String currentNode = resolveChangesetNode(current.getRawNode(), current.getNode());
                        String uniqueChangesetNode;

                        // restart processing
                        if (uniqueChangeset.value == null)
                        {
                            findUniqueChangesetAfterRestart(ao, uniqueChangeset, currentNode);
                        }
                        uniqueChangesetNode = uniqueChangeset.value != null ? resolveChangesetNode(uniqueChangeset.value.getRawNode(),
                                uniqueChangeset.value.getNode()) : null;

                        // false if current changeset is consider to be unique, otherwise it is duplicate
                        boolean isDuplicate = uniqueChangeset.value != null && uniqueChangesetNode.equals(currentNode);

                        // if it is unique changeset - it updates cursor for current valid unique changeset
                        if (!isDuplicate)
                        {
                            uniqueChangeset.value = current;
                        }

                        // skips non-existing issues
                        if (!"NON_EXISTING-0".equals(current.getProjectKey()))
                        {
                            deltaTime.begin(DeltaTimePart.UPDATE);
                            createIssueToChangsetRelation(ao, uniqueChangeset, current);
                            markAsUpdated(uniqueChangeset);
                            deltaTime.end(DeltaTimePart.UPDATE);
                        }

                        // repository relation is added on unique changeset
                        deltaTime.begin(DeltaTimePart.UPDATE);
                        createRepositoryToChangesetRelation(ao, uniqueChangeset, current);
                        deltaTime.end(DeltaTimePart.UPDATE);
                        // mark as updated
                        uniqueChangeset.value.setRepositoryId(0);

                        // if current changeset is not unique, means duplicated than will be removed
                        if (isDuplicate)
                        {
                            // deletes duplicate changeset
                            deltaTime.begin(DeltaTimePart.UPDATE);
                            ao.delete(current);
                            deltaTime.end(DeltaTimePart.UPDATE);

                        } else
                        {
                            // updates migrated entity - which is unique - not duplicate
                            deltaTime.begin(DeltaTimePart.UPDATE);
                            uniqueChangeset.value.save();
                            deltaTime.end(DeltaTimePart.UPDATE);
                        }

                    }
                    
                    // prevention in front of memory leak
                    deltaTime.begin(DeltaTimePart.CACHE);
                    ao.flushAll();
                    deltaTime.end(DeltaTimePart.CACHE);

                    progress.update(founded.length);
                    deltaTime.log();
                    return null;
                }

            });

        }
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
     * @param ao
     * @param uniqueChangeset
     * @param currentNode
     */
    private void findUniqueChangesetAfterRestart(final ActiveObjects ao, final Holder<ChangesetMapping> uniqueChangeset, String currentNode)
    {
        ChangesetMapping[] founded = ao.find(
                ChangesetMapping.class,
                Query.select().where(
                        ChangesetMapping.ISSUE_KEY + " is null AND ( " + ChangesetMapping.RAW_NODE + " = ? OR " + ChangesetMapping.NODE
                                + " = ? )", currentNode, currentNode));
        if (founded.length == 1)
        {
            uniqueChangeset.value = founded[0];

        } else if (founded.length == 0)
        {
            uniqueChangeset.value = null;

        } else
        {
            throw new RuntimeException("It should never happened - there are multiple proceed changesets with the same raw node: "
                    + currentNode);
        }
    }

    /**
     * Creates relation entity between issue and changeset.
     * 
     * @param ao
     * @param uniqueChangeset
     * @param current
     */
    private void createIssueToChangsetRelation(final ActiveObjects ao, final Holder<ChangesetMapping> uniqueChangeset,
            ChangesetMapping current)
    {
        ao.create(IssueToChangesetMapping.class, //
                new DBParam(IssueToChangesetMapping.CHANGESET_ID, uniqueChangeset.value), // foreign key to unique
                // changeset - current will be
                // maybe deleted
                new DBParam(IssueToChangesetMapping.PROJECT_KEY, current.getProjectKey()), //
                new DBParam(IssueToChangesetMapping.ISSUE_KEY, current.getIssueKey()) //
        );
    }

    /**
     * Marks changeset as proceed.
     * 
     * @param uniqueChangeset
     */
    private void markAsUpdated(final Holder<ChangesetMapping> uniqueChangeset)
    {
        uniqueChangeset.value.setProjectKey(null);
        uniqueChangeset.value.setIssueKey(null);
    }

    /**
     * Creates relation between repository and changset.
     * 
     * @param ao
     * @param uniqueChangeset
     * @param current
     */
    private void createRepositoryToChangesetRelation(final ActiveObjects ao, final Holder<ChangesetMapping> uniqueChangeset,
            ChangesetMapping current)
    {
        ao.create(RepositoryToChangesetMapping.class, //
                new DBParam(RepositoryToChangesetMapping.REPOSITORY_ID, current.getRepositoryId()), //
                new DBParam(RepositoryToChangesetMapping.CHANGESET_ID, uniqueChangeset.value) //
        );
    }

}
