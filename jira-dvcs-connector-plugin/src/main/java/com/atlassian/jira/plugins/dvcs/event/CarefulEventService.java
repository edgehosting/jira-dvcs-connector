package com.atlassian.jira.plugins.dvcs.event;

/**
 * An EventService that respects the <code>{@value com.atlassian.jira.plugins.dvcs.event.EventsFeature#FEATURE_NAME}</code> kill switch.
 */
public interface CarefulEventService extends EventService
{
}
