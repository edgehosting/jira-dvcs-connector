package com.atlassian.jira.plugins.dvcs.model;

/**
 * Enumeration of reasons why the message was discarded
 *
 * @author Miroslav Stencel
 *
 */
public enum DiscardReason
{
    /**
     * Message deserializaion failed
     */
    FAILED_DESERIALIZATION,

    /**
     * Retry count exceeded
     */
    RETRY_COUNT_EXCEEDED
}