package com.atlassian.jira.plugins.dvcs.event;

/**
 * An EventService that respects the <code>{@value com.atlassian.jira.plugins.dvcs.event.EventsFeature#FEATURE_NAME}</code> kill switch.
 *
 * @since v6.1
 */
public interface CarefulEventService extends EventService
{
}
