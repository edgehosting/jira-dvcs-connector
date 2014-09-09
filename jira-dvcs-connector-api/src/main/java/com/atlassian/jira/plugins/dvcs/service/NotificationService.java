package com.atlassian.jira.plugins.dvcs.service;

/**
 * Allows events to be sent, mainly exists because extracting the logic for Pull Request creation/updating is too
 * tangled so we can't send the events in better location.
 */
public interface NotificationService
{
    void broadcast(Object event);
}
