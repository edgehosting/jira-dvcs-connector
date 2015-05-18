package com.atlassian.jira.plugins.dvcs.service.admin;

import com.atlassian.jira.cluster.ClusterSafe;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Encapsulates the state of the priming of the dev summary cache operation.
 *
 * Based on DevSummaryReindexStatus from dev summary plugin.
 *
 * This class is a singleton, concurrent priming operations are prevented by callers calling #startExclusively first. If
 * they get true back then they are allowed to update the Status, otherwise they should wait until they can start
 * exclusively, {@link DevSummaryChangedEventServiceImpl}.
 *
 * Note that the #issueKeyCount and #pullRequestCount values will not necessarily equal the respective #totalIssueCount
 * and #totalPullRequestCount as we count the number of distinct issue keys BUT a given issue key may be processed
 * several times if it is in multiple repositories or organisations. These values should be used as 'indicative' of how
 * far through processing we are.
 */
@JsonIgnoreProperties ({ "result", "suppressed" })
@ThreadSafe
@ClusterSafe ("Only intended for Cloud use")
@Component
public class DevSummaryCachePrimingStatus
{
    private final AtomicBoolean inProgress;
    private final AtomicBoolean stopped;
    private final AtomicInteger issueKeyCount;
    private final AtomicInteger totalIssueKeyCount;
    private final AtomicInteger pullRequestCount;
    private final AtomicInteger totalPullRequestCount;
    private final AtomicReference<String> timeTaken;
    private final AtomicReference<Exception> exception;

    /**
     * Constructor for production use.
     */
    public DevSummaryCachePrimingStatus()
    {
        inProgress = new AtomicBoolean();
        stopped = new AtomicBoolean();
        issueKeyCount = new AtomicInteger();
        totalIssueKeyCount = new AtomicInteger();
        pullRequestCount = new AtomicInteger();
        totalPullRequestCount = new AtomicInteger();
        timeTaken = new AtomicReference<String>();
        exception = new AtomicReference<Exception>();
    }

    /**
     * Constructor for deserialisation from JSON (for example in the unit test).
     *
     * @param inProgress whether indexing is in progress
     * @param issueKeyCount the number of issues keys fetched so far
     * @param totalIssueKeyCount the total number of issues keys to fetch
     * @param pullRequestCount the number of pull requests fetched so far
     * @param totalPullRequestCount the total number of pull requests to fetch
     * @param exception any exception that occurred during reindexing
     * @param stopped whether the user has stopped the reindexing
     */
    @JsonCreator
    DevSummaryCachePrimingStatus(
            @JsonProperty ("inProgress") final boolean inProgress,
            @JsonProperty ("issueKeyCount") final int issueKeyCount,
            @JsonProperty ("totalIssueKeyCount") final int totalIssueKeyCount,
            @JsonProperty ("pullRequestCount") final int pullRequestCount,
            @JsonProperty ("totalPullRequestCount") final int totalPullRequestCount,
            @JsonProperty ("error") final Exception exception,
            @JsonProperty ("stopped") final boolean stopped,
            @JsonProperty ("timeTaken") final String timeTaken)
    {
        this();
        this.exception.set(exception);
        this.inProgress.set(inProgress);
        this.stopped.set(stopped);
        this.issueKeyCount.set(issueKeyCount);
        this.totalIssueKeyCount.set(totalIssueKeyCount);
        this.pullRequestCount.set(pullRequestCount);
        this.totalPullRequestCount.set(totalPullRequestCount);
        this.timeTaken.set(timeTaken);
    }

    // --------------------------------------- Accessors --------------------------------------

    @JsonProperty
    public int getIssueKeyCount()
    {
        return issueKeyCount.get();
    }

    @JsonProperty
    public int getTotalIssueKeyCount()
    {
        return totalIssueKeyCount.get();
    }

    @JsonProperty
    public int getPullRequestCount()
    {
        return pullRequestCount.get();
    }

    @JsonProperty
    public int getTotalPullRequestCount()
    {
        return totalPullRequestCount.get();
    }

    @JsonProperty
    public String getTimeTaken() {return timeTaken.get();}

    @JsonProperty
    public boolean isInProgress()
    {
        return inProgress.get();
    }

    @JsonProperty
    public Exception getError()
    {
        return exception.get();
    }

    /**
     * Indicates whether the user has asked to stop the reindexing operation.
     *
     * @return false otherwise, e.g. if the operation completed normally
     */
    @JsonProperty
    public boolean isStopped()
    {
        return stopped.get();
    }

    // --------------------------------------- Mutators --------------------------------------

    /**
     * Signals that the caller wants to start indexing, if successful (returns true) then this status instance will be
     * zeroed to the initial state.
     *
     * Users of this class should call this method prior to performing any other operations.
     *
     * @return true if the operation can start
     */
    public boolean startExclusively(final int totalIssueCount, final int totalPullRequestCount)
    {
        boolean canStart = inProgress.compareAndSet(false, true);

        if (canStart)
        {

            this.exception.set(null);
            this.issueKeyCount.set(0);
            this.totalIssueKeyCount.set(totalIssueCount);
            this.pullRequestCount.set(0);
            this.totalPullRequestCount.set(totalPullRequestCount);
            this.stopped.set(false);
            this.timeTaken.set("");
        }

        return canStart;
    }

    /**
     * Signals that we have processed a block of issue keys
     */
    public int completedIssueKeyBatch(int numberInBatch)
    {
        return issueKeyCount.addAndGet(numberInBatch);
    }

    /**
     * Signals that we have processed a block of pull request issue keys
     */
    public int completedPullRequestIssueKeyBatch(int numberInBatch)
    {
        return pullRequestCount.addAndGet(numberInBatch);
    }

    /**
     * Signals that some part of the reindexing process failed.
     */
    public void failed(final Exception exception, String timeTaken)
    {
        this.exception.set(exception);
        this.inProgress.set(false);
        this.stopped.set(false);
        this.timeTaken.set(timeTaken);
    }

    /**
     * Signals that the operation has been stopped by the user before completing. This signal is idempotent. #inProgress
     * is set separately to indicate that the stopped signal has been received and processing has stopped.
     */
    public void stopped()
    {
        stopped.set(true);
    }

    /**
     * Signals that reindexing has finished, successfully or otherwise.
     */
    public void finished(String timeTaken)
    {
        inProgress.set(false);
        this.timeTaken.set(timeTaken);
    }
}
