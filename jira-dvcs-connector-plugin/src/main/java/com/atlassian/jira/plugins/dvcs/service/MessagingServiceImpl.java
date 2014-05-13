package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageQueueItemMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.MessageTagMapping;
import com.atlassian.jira.plugins.dvcs.dao.MessageDao;
import com.atlassian.jira.plugins.dvcs.dao.MessageQueueItemDao;
import com.atlassian.jira.plugins.dvcs.dao.StreamCallback;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.model.DiscardReason;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.MessageState;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsChangesetsProcessor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.plugin.PluginException;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import net.java.ao.DBParam;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * A {@link MessagingService} implementation.
 *
 * @author Stanislav Dvorscak
 *
 */
public class MessagingServiceImpl implements MessagingService, DisposableBean
{

    private static final String SYNCHRONIZATION_REPO_TAG_PREFIX = "synchronization-repository-";
    private static final String SYNCHRONIZATION_AUDIT_TAG_PREFIX = "audit-id-";

    /**
     * Logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(MessagingServiceImpl.class);

    /**
     * Injected {@link ActiveObjects} dependency.
     */
    @Resource
    private ActiveObjects activeObjects;

    /**
     * Injected {@link MessageDao} dependency.
     */
    @Resource
    private MessageDao messageDao;

    /**
     * Injected {@link MessageQueueItemDao} dependency.
     */
    @Resource
    private MessageQueueItemDao messageQueueItemDao;

    /**
     * Injected {@link MessageConsumer}-s dependency.
     */
    @Resource
    private MessageConsumer<?>[] consumers;

    /**
     * Injected {@link MessageExecutor} dependency.
     */
    @Resource
    private MessageExecutor messageExecutor;

    /**
     * Injected {@link MessagePayloadSerializer}-s dependency.
     */
    @Resource
    private MessagePayloadSerializer<?>[] payloadSerializers;

    /**
     * Injected {@link MessageConsumer}-s dependency.
     */
    @Resource
    private MessageConsumer<?>[] messageConsumers;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    protected ChangesetService changesetService;

    @Resource
    private SmartcommitsChangesetsProcessor smartcCommitsProcessor;

    @Resource
    private SyncAuditLogDao syncAudit;

    @Resource
    private Synchronizer synchronizer;

    @Resource
    private HttpClientProvider httpClientProvider;

    private final Object endProgressLock = new Object();

    /**
     * Maps identity of message address to appropriate {@link MessageAddress}.
     */
    private final Map<String, MessageAddress<?>> idToMessageAddress = new ConcurrentHashMap<String, MessageAddress<?>>();

    /**
     * Maps between {@link MessagePayloadSerializer#getPayloadType()} and appropriate {@link MessagePayloadSerializer serializer}.
     */
    private final Map<Class<?>, MessagePayloadSerializer<?>> payloadTypeToPayloadSerializer = new ConcurrentHashMap<Class<?>, MessagePayloadSerializer<?>>();

    /**
     * Maps between {@link MessageConsumer#getQueue()} and appropriate {@link MessageConsumer}.
     */
    private final ConcurrentMap<String, MessageConsumer<?>> queueToMessageConsumer = new ConcurrentHashMap<String, MessageConsumer<?>>();

    /**
     * Maps between {@link MessageConsumer#getAddress()} and appropriate {@link MessageConsumer consumers}.
     */
    private final ConcurrentMap<String, List<MessageConsumer<?>>> addressToMessageConsumer = new ConcurrentHashMap<String, List<MessageConsumer<?>>>();

    /**
     * Contains all tags which are currently paused.
     */
    private final Set<String> pausedTags = new CopyOnWriteArraySet<String>();

    /**
     * Initializes been.
     */
    @PostConstruct
    public void init()
    {
        for (MessageConsumer<?> messageConsumer : messageConsumers)
        {
            queueToMessageConsumer.putIfAbsent(messageConsumer.getQueue(), messageConsumer);
            List<MessageConsumer<?>> byAddress = addressToMessageConsumer.get(messageConsumer.getAddress().getId());
            if (byAddress == null)
            {
                addressToMessageConsumer.putIfAbsent(messageConsumer.getAddress().getId(),
                        byAddress = new CopyOnWriteArrayList<MessageConsumer<?>>());
            }
            byAddress.add(messageConsumer);
        }

        for (MessagePayloadSerializer<?> payloadSerializer : payloadSerializers)
        {
            payloadTypeToPayloadSerializer.put(payloadSerializer.getPayloadType(), payloadSerializer);
        }
    }

    /**
     * Wait until AO is fully accessible.
     */
    private boolean waitForAO()
    {
        int countOfRetry = 15;
        do
        {
            try
            {
                log.debug("Attempting to wait for AO.");
                activeObjects.count(MessageMapping.class);
                log.debug("Attempting to wait for AO - DONE.");
                stop = true;
                return true;
            } catch (PluginException e)
            {
                countOfRetry--;
                try
                {
                    Thread.sleep(5000);
                } catch (InterruptedException ie)
                {
                    // nothing to do
                }
            }
        } while (countOfRetry > 0 && !stop);
        log.debug("Attempting to wait for AO - UNSUCCESSFUL.");
        return false;
    }

    /**
     * Marks failed all messages, which are in state running
     */
    private void initRunningToFail()
    {
        log.debug("Setting messages in running state to fail");
        messageQueueItemDao.getByState(MessageState.RUNNING, new StreamCallback<MessageQueueItemMapping>()
        {

            @Override
            public void callback(MessageQueueItemMapping e)
            {
                Message<HasProgress> message = new Message<HasProgress>();
                @SuppressWarnings("unchecked")
                MessageConsumer<HasProgress> consumer = (MessageConsumer<HasProgress>) queueToMessageConsumer.get(e.getQueue());

                toMessage(message, e.getMessage());
                fail(consumer, message, new RuntimeException("Synchronization has been interrupted (probably plugin un/re/install)."));
            }

        });
    }

    /**
     * Restart consumers.
     */
    private void restartConsumers()
    {
        log.debug("Restarting message consumers");
        Set<String> addresses = new HashSet<String>();
        for (MessageConsumer<?> consumer : consumers)
        {
            addresses.add(consumer.getAddress().getId());
        }

        for (String address : addresses)
        {
            messageExecutor.notify(address);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void publish(MessageAddress<P> address, P payload, String... tags)
    {
        publish(address, payload, MessagingService.DEFAULT_PRIORITY, tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void publish(MessageAddress<P> address, P payload, int priority, String... tags)
    {
        MessageState state = MessageState.PENDING;
        for (String tag : tags)
        {
            if (pausedTags.contains(tag))
            {
                state = MessageState.SLEEPING;
                break;
            }
        }

        @SuppressWarnings("unchecked")
        MessagePayloadSerializer<P> payloadSerializer = (MessagePayloadSerializer<P>) payloadTypeToPayloadSerializer.get(payload.getClass());

        Message<P> message = new Message<P>();
        message.setAddress(address);
        message.setPayload(payloadSerializer.serialize(payload));
        message.setPayloadType(address.getPayloadType());
        message.setTags(tags);
        message.setPriority(priority);

        createMessage(message, state, tags);

        messageExecutor.notify(address.getId());
    }

    protected <P extends HasProgress> void createMessage(Message<P> message, MessageState state, String... tags)
    {
        MessageMapping messageMapping = messageDao.create(toMessageMap(message), tags);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<MessageConsumer<P>> byAddress = (List) addressToMessageConsumer.get(message.getAddress().getId());
        for (MessageConsumer<P> consumer : byAddress)
        {
            messageQueueItemDao.create(messageQueueItemToMap(messageMapping.getID(), consumer.getQueue(), state, null));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pause(String tag)
    {
        pausedTags.add(tag);
        final Set<Integer> syncAudits = new LinkedHashSet<Integer>();
        messageDao.getByTag(tag, new StreamCallback<MessageMapping>()
        {

            @Override
            public void callback(final MessageMapping message)
            {
                activeObjects.executeInTransaction(new TransactionCallback<Void>()
                {

                    @Override
                    public Void doInTransaction()
                    {
                        for (MessageQueueItemMapping messageQueueItem : message.getQueuesItems())
                        {
                            // messages, which are running can not be paused!
                            if (!MessageState.RUNNING.name().equals(messageQueueItem.getState()))
                            {
                                messageQueueItem.setState(MessageState.SLEEPING.name());
                                messageQueueItemDao.save(messageQueueItem);
                            }
                        }
                        return null;
                    }

                });
                
                int syncAuditId = getSynchronizationAuditIdFromTags(transformTags(message.getTags()));
                if (syncAuditId != 0)
                {
                    syncAudits.add(syncAuditId);
                }
            }

        });
        for (Integer syncAuditId : syncAudits)
        {
            syncAudit.pause(syncAuditId);
        }
    }

    @Override
    public <P extends  HasProgress> P deserializePayload(Message<P> message)
    {
        @SuppressWarnings("unchecked")
        MessagePayloadSerializer<P> payloadSerializer = (MessagePayloadSerializer<P>) payloadTypeToPayloadSerializer.get(message.getPayloadType());
        return payloadSerializer.deserialize(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resume(String tag)
    {
        pausedTags.remove(tag);
        final Set<String> addresses = new HashSet<String>();
        final Set<Integer> syncAudits = new HashSet<Integer>();

        messageQueueItemDao.getByTagAndState(tag, MessageState.SLEEPING, new StreamCallback<MessageQueueItemMapping>()
        {

            @Override
            public void callback(MessageQueueItemMapping e)
            {
                e.setState(MessageState.PENDING.name());
                messageQueueItemDao.save(e);
                addresses.add(e.getMessage().getAddress());

                int syncAuditId = getSynchronizationAuditIdFromTags(transformTags(e.getMessage().getTags()));
                if (syncAuditId != 0)
                {
                    syncAudits.add(syncAuditId);
                }


            }

        });

        for (String address : addresses)
        {
            messageExecutor.notify(address);
        }

        for (Integer syncAuditId : syncAudits)
        {
            syncAudit.resume(syncAuditId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void retry(final String tag, final int auditId)
    {
        final Set<String> addresses = new HashSet<String>();
        messageDao.getByTag(tag, new StreamCallback<MessageMapping>()
        {

            @Override
            public void callback(MessageMapping e)
            {
                messageQueueItemDao.getByTagAndState(tag, MessageState.WAITING_FOR_RETRY, new StreamCallback<MessageQueueItemMapping>()
                {

                    @Override
                    public void callback(final MessageQueueItemMapping e)
                    {
                        activeObjects.executeInTransaction(new TransactionCallback<Void>()
                        {

                            @Override
                            public Void doInTransaction()
                            {
                                updateSyncAuditId(auditId, e);
                                addresses.add(e.getMessage().getAddress());
                                e.setState(MessageState.PENDING.name());
                                messageQueueItemDao.save(e);
                                return null;
                            }
                        });

                    }

                    private void updateSyncAuditId(final int auditId, MessageQueueItemMapping e)
                    {
                        String newSyncAuditIdLog = getTagForAuditSynchronization(auditId);

                        // removes obsolete tag for synchronization audit
                        for (MessageTagMapping tag : e.getMessage().getTags())
                        {
                            if (tag.getTag().startsWith(SYNCHRONIZATION_AUDIT_TAG_PREFIX))
                            {
                                activeObjects.delete(tag);
                                break;
                            }
                        }

                        // adds new synchronization audit id tag
                        activeObjects.create(MessageTagMapping.class, //
                                new DBParam(MessageTagMapping.MESSAGE, e.getMessage().getID()), //
                                new DBParam(MessageTagMapping.TAG, newSyncAuditIdLog) //
                                );
                    }

                });
            }

        });

        for (String address : addresses)
        {
            messageExecutor.notify(address);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancel(String tag)
    {
        messageDao.getByTag(tag, new StreamCallback<MessageMapping>()
        {

            @Override
            public void callback(final MessageMapping message)
            {
                activeObjects.executeInTransaction(new TransactionCallback<Void>()
                {

                    @Override
                    public Void doInTransaction()
                    {
                        MessageMapping fullMessage = messageDao.getById(message.getID());
                        if (fullMessage.getQueuesItems() != null)
                        {
                            for (MessageQueueItemMapping queueItem : fullMessage.getQueuesItems())
                            {
                                messageQueueItemDao.delete(queueItem);
                            }
                        }
                        messageDao.delete(fullMessage);
                        return null;
                    }

                });
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void running(MessageConsumer<P> consumer, Message<P> message)
    {
        MessageQueueItemMapping queueItem = messageQueueItemDao.getByQueueAndMessage(consumer.getQueue(), message.getId());
        queueItem.setState(MessageState.RUNNING.name());
        messageQueueItemDao.save(queueItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void ok(MessageConsumer<P> consumer, Message<P> message)
    {
        messageQueueItemDao.delete(messageQueueItemDao.getByQueueAndMessage(consumer.getQueue(), message.getId()));
        MessageMapping messageMapping = messageDao.getById(message.getId());
        if (messageMapping.getQueuesItems().length == 0)
        {
            messageDao.delete(messageMapping);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> void fail(MessageConsumer<P> consumer, Message<P> message, Throwable t)
    {
        MessageQueueItemMapping queueItem = messageQueueItemDao.getByQueueAndMessage(consumer.getQueue(), message.getId());
        queueItem.setRetriesCount(queueItem.getRetriesCount() + 1);
        queueItem.setState(MessageState.WAITING_FOR_RETRY.name());
        messageQueueItemDao.save(queueItem);
        syncAudit.setException(getSynchronizationAuditIdFromTags(message.getTags()), t, false);
    }

    @Override
    public <P extends HasProgress> void discard(final MessageConsumer<P> consumer, final Message<P> message, final DiscardReason discardReason)
    {
        MessageQueueItemMapping queueItem = messageQueueItemDao.getByQueueAndMessage(consumer.getQueue(), message.getId());
        queueItem.setState(MessageState.DISCARDED.name());
        queueItem.setStateInfo(discardReason.name());
        messageQueueItemDao.save(queueItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P extends HasProgress> Message<P> getNextMessageForConsuming(MessageConsumer<P> consumer, String address)
    {
        MessageQueueItemMapping messageQueueItem = messageQueueItemDao.getNextItemForProcessing(consumer.getQueue(), address);
        if (messageQueueItem == null)
        {
            return null;
        }

        Message<P> result = new Message<P>();
        toMessage(result, messageQueueItem.getMessage());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getQueuedCount(String tag)
    {
        return messageDao.getMessagesForConsumingCount(tag);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <P extends HasProgress> MessageAddress<P> get(final Class<P> payloadType, final String id)
    {
        MessageAddress<P> result;

        synchronized (idToMessageAddress)
        {
            result = (MessageAddress<P>) idToMessageAddress.get(id);
            if (result == null)
            {
                idToMessageAddress.put(id, result = new MessageAddress<P>()
                {

                    @Override
                    public String getId()
                    {
                        return id;
                    }

                    @Override
                    public Class<P> getPayloadType()
                    {
                        return payloadType;
                    }

                });
            }
        }

        return (MessageAddress<P>) result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTagForSynchronization(Repository repository)
    {
        return SYNCHRONIZATION_REPO_TAG_PREFIX + repository.getId();
    }

    @Override
    public String getTagForAuditSynchronization(int id)
    {
        return SYNCHRONIZATION_AUDIT_TAG_PREFIX + id;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getSynchronizationAuditIdFromTags(String[] tags)
    {
        for (String tag : tags)
        {
            try
            {
                if (StringUtils.startsWith(tag, SYNCHRONIZATION_AUDIT_TAG_PREFIX))
                {
                    return Integer.parseInt(tag.substring(SYNCHRONIZATION_AUDIT_TAG_PREFIX.length()));

                }
            } catch (NumberFormatException e)
            {
                log.error("Synchronization audit id tag has invalid format, tag was: " + tag);
                // we don't stop, maybe there is still a valid tag
            }
        }

        // no tag was resolved
        return 0;
    }


    @Override
    public <P extends HasProgress> Repository getRepositoryFromMessage(Message<P> message)
    {
        for (String tag : message.getTags())
        {
            if (StringUtils.startsWith(tag, SYNCHRONIZATION_REPO_TAG_PREFIX))
            {
                int repositoryId;
                try
                {
                    repositoryId = Integer.parseInt(tag.substring(SYNCHRONIZATION_REPO_TAG_PREFIX.length()));
                    return repositoryService.get(repositoryId);

                } catch (NumberFormatException e)
                {
                    log.warn("Get repo ID from message: " + e.getMessage());
                }
            }
        }

        log.warn("Can't get repository ID from tags for message with ID {}", message.getId());
        return null;
    }

    /**
     * Re-maps provided {@link Message} to parameters.
     *
     * @param source
     *            of mapping
     * @return mapped entity
     */
    private <P extends HasProgress> Map<String, Object> toMessageMap(Message<P> source)
    {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put(MessageMapping.ADDRESS, source.getAddress().getId());
        result.put(MessageMapping.PRIORITY, source.getPriority());
        result.put(MessageMapping.PAYLOAD_TYPE, source.getPayloadType().getCanonicalName());
        result.put(MessageMapping.PAYLOAD, source.getPayload());

        return result;
    }

    /**
     * Re-maps provided {@link MessageMapping} to {@link Message}.
     *
     * @param target
     *            of mapping
     * @param source
     *            of mapping
     */
    @SuppressWarnings("unchecked")
    private <P extends HasProgress> void toMessage(Message<P> target, MessageMapping source)
    {
        Class<P> payloadType;
        try
        {
            payloadType = (Class<P>) Class.forName(source.getPayloadType(), true, getClass().getClassLoader());
        } catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        int retriesCount = 0;
        for (MessageQueueItemMapping queueItem : source.getQueuesItems())
        {
            retriesCount = Math.max(retriesCount, queueItem.getRetriesCount());
        }

        target.setId(source.getID());
        target.setAddress(get(payloadType, source.getAddress()));
        target.setPayload(source.getPayload());
        target.setPayloadType(payloadType);
        target.setPriority(source.getPriority());
        target.setTags(transformTags(source.getTags()));
        target.setRetriesCount(retriesCount);
    }

    private String[] transformTags(MessageTagMapping[] messageTags)
    {
        return Iterables.toArray(Iterables.transform(Arrays.asList(messageTags), new Function<MessageTagMapping, String>()
        {

            @Override
            public String apply(MessageTagMapping input)
            {
                return input.getTag();
            }

        }), String.class);
    }

    /**
     * Re-maps provided data to {@link MessageQueueItemMapping} parameters.
     *
     * @param messageId
     *            {@link Message#getId()}
     * @param queue
     * @param state
     * @return mapped entity
     */
    private Map<String, Object> messageQueueItemToMap(int messageId, String queue, MessageState state, String stateInfo)
    {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put(MessageQueueItemMapping.MESSAGE, messageId);
        result.put(MessageQueueItemMapping.QUEUE, queue);
        result.put(MessageQueueItemMapping.STATE, state.name());
        result.put(MessageQueueItemMapping.STATE_INFO, stateInfo);
        result.put(MessageQueueItemMapping.RETRIES_COUNT, 0);

        return result;

    }

    @Override
    public <P extends HasProgress> void tryEndProgress(Repository repository, Progress progress, MessageConsumer<P> consumer, int auditId)
    {
        boolean finished = endProgress(repository, progress);
        if (finished)
        {
            pausedTags.remove(getTagForSynchronization(repository));

            if (auditId > 0)
            {
                final Date finishDate;
                final Date firstRequestDate;
                final int numRequests;
                final int flightTimeMs;

                if (progress == null)
                {
                    finishDate = new Date();
                    firstRequestDate = null;
                    numRequests = 0;
                    flightTimeMs = 0;
                }
                else
                {
                    finishDate = new Date(progress.getFinishTime());
                    firstRequestDate = progress.getFirstMessageTime();
                    numRequests = progress.getNumRequests();
                    flightTimeMs = progress.getFlightTimeMs();
                }

                syncAudit.finish(auditId, firstRequestDate, numRequests, flightTimeMs, finishDate);
            }
        }
    }

    private boolean endProgress(Repository repository, Progress progress)
    {
        int queuedCount;
        synchronized(endProgressLock)
        {
            queuedCount = getQueuedCount(getTagForSynchronization(repository));
        }
        if (queuedCount == 0)
        {
            try
            {
                // TODO error could be in PR synchronization and thus we can process smartcommits
                if (progress == null || progress.getError() == null)
                {
                    smartcCommitsProcessor.startProcess(progress, repository, changesetService);
                }
                if (progress != null && !progress.isFinished())
                {
                    progress.finish();

                    EnumSet<SynchronizationFlag> flags = progress.getRunAgainFlags();
                    if (flags != null)
                    {
                        progress.setRunAgainFlags(null);
                        // to be sure that soft sync will be run
                        flags.add(SynchronizationFlag.SOFT_SYNC);
                        synchronizer.doSync(repository, flags);
                    }
                }

                return true;
            } finally
            {
                httpClientProvider.closeIdleConnections();
            }
        }

        return false;
    }

    @Override
    public void onStart()
    {
        new Thread(new Runnable()
        {

            @Override
            public void run()
            {
                if (waitForAO())
                {
                    initRunningToFail();
                    restartConsumers();
                }
            }

        }, "WaitForAO").start();
    }

    private volatile boolean stop = false;

    @Override
    public void destroy() throws Exception
    {
        stop = true;
    }
}
