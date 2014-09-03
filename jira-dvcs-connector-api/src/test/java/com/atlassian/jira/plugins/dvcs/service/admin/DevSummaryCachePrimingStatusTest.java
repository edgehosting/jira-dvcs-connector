package com.atlassian.jira.plugins.dvcs.service.admin;

import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class DevSummaryCachePrimingStatusTest
{
    private static final Exception PREVIOUS_ERROR = new Exception("previous exception");
    private static final int ISSUE_KEY_COUNT = 1000;
    private static final int TOTAL_ISSUE_KEY_COUNT = 2000;
    private static final int PULL_REQUEST_COUNT = 200;
    private static final int TOTAL_PULL_REQUEST_COUNT = 400;

    @Test
    public void shouldBeAbleToStartExclusivelyIfNoPrimingRunning()
    {
        assertStartExclusively(false, true);
    }

    @Test
    public void shouldNotBeAbleToStartExclusivelyIfPrimingRunning()
    {
        assertStartExclusively(true, false);
    }

    /**
     * Convenience method for asserting whether a priming operation has started.
     *
     * @param alreadyStarted whether the operation should already appear to be started
     * @param expectedToBeStarted the new expected state of the operation
     */
    private static void assertStartExclusively(
            final boolean alreadyStarted, final boolean expectedToBeStarted)
    {
        // Set up
        final DevSummaryCachePrimingStatus status =
                new DevSummaryCachePrimingStatus(alreadyStarted, ISSUE_KEY_COUNT, TOTAL_ISSUE_KEY_COUNT, PULL_REQUEST_COUNT,
                        TOTAL_PULL_REQUEST_COUNT, PREVIOUS_ERROR, true, "FOO");

        // Invoke
        final boolean started = status.startExclusively(TOTAL_ISSUE_KEY_COUNT, TOTAL_PULL_REQUEST_COUNT);

        // Check
        assertThat(started).isEqualTo(expectedToBeStarted);
    }

    /**
     * Convenience method for asserting the state of a DevSummaryCachePrimingStatus.
     *
     * @param status the instance to check
     * @param expectedError the expected value
     * @param expectedIssueKeyCount the expected value
     * @param expectedInProgress the expected value
     * @param expectedTotalIssueCount the expected value
     * @param expectedToBeStopped whether the status is expected to be stopped
     */
    private static void assertStatus(final DevSummaryCachePrimingStatus status, final Exception expectedError,
            final boolean expectedInProgress, final int expectedIssueKeyCount, final int expectedTotalIssueCount,
            final int expectedPullRequestCount, final int expectedTotalPullRequestCount,
            final boolean expectedToBeStopped)
    {
        assertThat(status.getError()).isEqualTo(expectedError);
        assertThat(status.isInProgress()).isEqualTo(expectedInProgress);
        assertThat(status.getIssueKeyCount()).isEqualTo(expectedIssueKeyCount);
        assertThat(status.getTotalIssueKeyCount()).isEqualTo(expectedTotalIssueCount);
        assertThat(status.getPullRequestCount()).isEqualTo(expectedPullRequestCount);
        assertThat(status.getTotalPullRequestCount()).isEqualTo(expectedTotalPullRequestCount);
        assertThat(status.isStopped()).isEqualTo(expectedToBeStopped);
    }

    @Test
    public void newStatusShouldHaveCorrectInitialState()
    {
        final DevSummaryCachePrimingStatus status = new DevSummaryCachePrimingStatus();

        assertStatus(status, null, false, 0, 0, 0, 0, false);
    }

    @Test
    public void failedShouldRetainIssueCounts()
    {
        final Exception exception = new Exception("oops");
        final DevSummaryCachePrimingStatus status =
                new DevSummaryCachePrimingStatus(true, ISSUE_KEY_COUNT, TOTAL_ISSUE_KEY_COUNT, PULL_REQUEST_COUNT,
                        TOTAL_PULL_REQUEST_COUNT, null, true, "FOO");

        status.failed(exception, "FPP");

        assertStatus(status, exception, false, ISSUE_KEY_COUNT, TOTAL_ISSUE_KEY_COUNT,
                PULL_REQUEST_COUNT, TOTAL_PULL_REQUEST_COUNT, false);
    }

    @Test
    public void startingPrimingShouldClearPreviousState()
    {
        final int previousTotalIssueCount = 1;
        final int previousTotalPullRequestCount = 2;
        final DevSummaryCachePrimingStatus status =
                new DevSummaryCachePrimingStatus(false, ISSUE_KEY_COUNT, previousTotalIssueCount,
                        PULL_REQUEST_COUNT, previousTotalPullRequestCount, PREVIOUS_ERROR, true, "FOO");
        status.startExclusively(TOTAL_ISSUE_KEY_COUNT, TOTAL_PULL_REQUEST_COUNT);

        assertStatus(status, null, true, 0, TOTAL_ISSUE_KEY_COUNT, 0, TOTAL_PULL_REQUEST_COUNT,
                false);
    }

    @Test
    public void startingNextIssueShouldIncrementThePrimingIssueCount()
    {
        final DevSummaryCachePrimingStatus status = new DevSummaryCachePrimingStatus();
        status.startExclusively(TOTAL_ISSUE_KEY_COUNT, TOTAL_PULL_REQUEST_COUNT);

        final int numberInBatch = 50;
        int result = status.completedIssueKeyBatch(numberInBatch);

        assertThat(numberInBatch).isEqualTo(result);

        assertStatus(status, null, true, numberInBatch, TOTAL_ISSUE_KEY_COUNT,
                0, TOTAL_PULL_REQUEST_COUNT, false);
    }

    @Test
    public void startingNextPRShouldIncrementThePRCount()
    {
        final DevSummaryCachePrimingStatus status = new DevSummaryCachePrimingStatus();
        status.startExclusively(TOTAL_ISSUE_KEY_COUNT, TOTAL_PULL_REQUEST_COUNT);

        status.completedPullRequestIssueKeyBatch(10);

        assertStatus(status, null, true, 0, TOTAL_ISSUE_KEY_COUNT,
                10, TOTAL_PULL_REQUEST_COUNT, false);
    }

    @Test
    public void successfulPrimingShouldRetainItsValues()
    {
        final DevSummaryCachePrimingStatus status = new DevSummaryCachePrimingStatus();
        final int totalIssueCount = 2;
        final int totalPRCount = 1;
        status.startExclusively(totalIssueCount, totalPRCount);
        status.completedIssueKeyBatch(2);
        status.completedPullRequestIssueKeyBatch(1);

        status.finished("FOO");

        // Finished but we didn't manually stop so expect stopped to be false
        assertStatus(status, null, false, totalIssueCount, totalIssueCount,
                totalPRCount, totalPRCount, false);
    }

    @Test
    public void stoppingShouldSetStoppedFlag()
    {
        final DevSummaryCachePrimingStatus status = new DevSummaryCachePrimingStatus();
        status.startExclusively(0, 0);

        status.stopped();

        assertStatus(status, null, true, 0, 0, 0, 0, true);
    }

    @Test
    public void shouldBeRoundTrippableViaJson() throws Exception
    {
        // Set up
        final DevSummaryCachePrimingStatus statusIn = new DevSummaryCachePrimingStatus(true, ISSUE_KEY_COUNT,
                TOTAL_ISSUE_KEY_COUNT, PULL_REQUEST_COUNT, TOTAL_PULL_REQUEST_COUNT, null, false, "FOO");

        // Invoke
        final String json = statusIn.asJson();

        // Check
        final DevSummaryCachePrimingStatus statusOut = new ObjectMapper().readValue(json, DevSummaryCachePrimingStatus.class);
        assertStatus(statusOut,
                statusIn.getError(),
                statusIn.isInProgress(),
                statusIn.getIssueKeyCount(),
                statusIn.getTotalIssueKeyCount(),
                statusIn.getPullRequestCount(),
                statusIn.getTotalPullRequestCount(),
                statusIn.isStopped()
        );
    }

    @Test
    public void jsonShouldIncludeErrorDetailsIfPrimingFailed()
    {
        final String errorMessage = "Oops!";

        final DevSummaryCachePrimingStatus status = new DevSummaryCachePrimingStatus(true, ISSUE_KEY_COUNT,
                TOTAL_ISSUE_KEY_COUNT, PULL_REQUEST_COUNT, TOTAL_PULL_REQUEST_COUNT, new Exception(errorMessage), false, "FOO");

        status.startExclusively(TOTAL_ISSUE_KEY_COUNT, TOTAL_PULL_REQUEST_COUNT);
        final String timeTaken = "foo";
        status.failed(new Exception(errorMessage), timeTaken);

        final String json = status.asJson();

        assertThat(json).contains(errorMessage);
        assertThat(json).contains(status.getTimeTaken());
    }
}
