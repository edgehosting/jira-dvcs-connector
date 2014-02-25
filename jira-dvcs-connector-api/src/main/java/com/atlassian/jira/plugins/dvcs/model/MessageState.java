package com.atlassian.jira.plugins.dvcs.model;

/**
 * State, which tracks current state of message which is processed.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public enum MessageState
{

    /**
     * Message is waiting for processing.
     */
    PENDING,

    /**
     * Message is currently processed.
     */
    RUNNING,

    /**
     * Processing of an message was failed and message is waiting for running.
     */
    WAITING_FOR_RETRY,

    /**
     * Message is sleeping and it is waiting for resuming.
     */
    SLEEPING,

    /**
     * Message is discarded
     */
    DISCARDED
}
