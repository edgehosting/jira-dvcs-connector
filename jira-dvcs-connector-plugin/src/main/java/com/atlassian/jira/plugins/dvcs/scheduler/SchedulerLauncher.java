package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;

import static com.atlassian.jira.plugins.dvcs.scheduler.SchedulerLauncher.LifecycleEvent.LIFECYCLE_AWARE_ON_START;
import static com.atlassian.jira.plugins.dvcs.scheduler.SchedulerLauncher.LifecycleEvent.PLUGIN_ENABLED;
import static com.atlassian.jira.plugins.dvcs.util.DvcsConstants.PLUGIN_KEY;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper class that listens to the events required to confirm that the plugin is fully started
 * and is ready to schedule a scheduler job.
 */
@ExportAsService (LifecycleAware.class)
@Component
public class SchedulerLauncher implements LifecycleAware
{
    private static final Logger log = LoggerFactory.getLogger(SchedulerLauncher.class);

    private final EventPublisher eventPublisher;

    @GuardedBy ("this")
    private final Set<LifecycleEvent> lifecycleEvents = EnumSet.noneOf(LifecycleEvent.class);

    private final List<SchedulerLauncherJob> jobs = new LinkedList<SchedulerLauncherJob>();

    @Autowired
    public SchedulerLauncher(@ComponentImport final EventPublisher eventPublisher)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
    }

    @PostConstruct
    public void postConstruct()
    {
        eventPublisher.register(this);
        log.debug("registered event listener");
    }

    /**
     * This is received from the plugin system after the plugin is fully initialized.  It is not safe to use
     * Active Objects before this event is received.
     */
    @EventListener
    public void onPluginEnabled(final PluginEnabledEvent event)
    {
        if (PLUGIN_KEY.equals(event.getPlugin().getKey()))
        {
            onLifecycleEvent(PLUGIN_ENABLED);
        }
    }

    @Override
    public void onStart()
    {
        onLifecycleEvent(LIFECYCLE_AWARE_ON_START);
    }

    @PreDestroy
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
        log.debug("SchedulerLauncher destroyed");
    }

    /**
     * Schedule a job to be run after all events have been received.
     * The job will be run immediately in the current thread, if all events have already been received.
     * <p>
     * This is {@code synchronized} so that we could keep {@code jobs} in a consistent state
     *  when this method and {@link #onLifecycleEvent(LifecycleEvent)} are accessed from multiple threads.
     */
    public synchronized void runWhenReady(SchedulerLauncherJob job)
    {
        if (isLifecycleReady())
        {
            // run the job immediately if all events have already occurred
            log.debug("all events have been received, executing job immediately: {}", job);
            runSingleJob(job);
        }
        else
        {
            // otherwise add the job to the list so that it could be run later when ready
            log.debug("not all events have been received, adding job to queue: {}", job);
            jobs.add(job);
        }
    }

    /**
     * The latch which ensures all of the plugin/application lifecycle progress is completed before we call
     * {@code launch()}.
     * <p>
     * This is {@code synchronized} so that we could keep {@code jobs} in a consistent state
     *  when this method and {@link #runWhenReady(SchedulerLauncherJob)} are accessed from multiple threads.
     */
    private synchronized void onLifecycleEvent(final LifecycleEvent event)
    {
        log.debug("onLifecycleEvent: {}", event);
        if (isLifecycleReady(event))
        {
            log.debug("Got the last lifecycle event... Time to get started!");
            // we don't need to listen to events anymore
            eventPublisher.unregister(this);

            // now run all jobs
            for (SchedulerLauncherJob job : jobs)
            {
                runSingleJob(job);
            }

            jobs.clear();
        }
    }

    private void runSingleJob(final SchedulerLauncherJob job)
    {
        try
        {
            job.run();
        }
        catch (Exception ex)
        {
            log.error("Unexpected error while running launch job " + job, ex);
        }
    }

    /**
     * The event latch.
     * <p>
     * When something related to the plugin initialization happens, we call this with
     * the corresponding type of the event.  We will return {@code true}, when the very last type
     * of event is triggered.
     * </p>
     * <p>
     * The invocation of this method is {@code synchronized} (by the caller) because {@code EnumSet} is not
     * thread-safe and because we have multiple accesses to {@code lifecycleEvents} that need to happen
     * atomically for correct behaviour.
     * </p>
     *
     * @param event the lifecycle event that occurred
     * @return {@code true} if this completes the set of initialization-related events; {@code false} otherwise
     */
    private boolean isLifecycleReady(final LifecycleEvent event)
    {
        lifecycleEvents.add(event);

        return isLifecycleReady();
    }

    /**
     * We will return {@code true} at most once, when the very last type
     * of event is triggered.
     * <p/>
     * The invocation of this method is {@code synchronized} (by the callers) because {@code EnumSet} is not
     * thread-safe and because we have multiple accesses to {@code lifecycleEvents} that need to happen
     * atomically for correct behaviour.
     * <p/>
     *
     * @return {@code true} if the set of initialization-related events have all occurred; {@code false} otherwise
     */
    private boolean isLifecycleReady()
    {
        return lifecycleEvents.size() == LifecycleEvent.values().length;
    }

    /**
     * Keeps track of everything that needs to happen before we are sure that it is safe
     * to talk to all of the components we need to use, particularly the {@code SchedulerService}
     * and Active Objects.  We will not try to initialize until all of them have happened.
     */
    static enum LifecycleEvent
    {
        PLUGIN_ENABLED,            // when a plugin is started/stopped
        LIFECYCLE_AWARE_ON_START   // when the server has started
    }

    public static interface SchedulerLauncherJob
    {
        void run();
    }
}
