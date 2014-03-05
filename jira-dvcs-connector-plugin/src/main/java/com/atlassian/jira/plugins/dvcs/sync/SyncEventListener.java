package com.atlassian.jira.plugins.dvcs.sync;

/**
 * Marker interface for sync event listeners. Implementations should use the {@link com.google.common.eventbus.EventBus
 * Guava EventBus} <code>{@literal @Subscribe}</code> annotation to indicate what events they listen to as in the
 * following example.
 * <pre>
 * public class PullRequestListener
 * {
 *     {@literal @}Subscribe
 *     public void onCreatePR(PullRequestCreatedEvent pullRequestCreated)
 *     {
 *         System.out.println("Pull request created!");
 *     }
 * }
 * </pre>
 */
public interface SyncEventListener
{
}
