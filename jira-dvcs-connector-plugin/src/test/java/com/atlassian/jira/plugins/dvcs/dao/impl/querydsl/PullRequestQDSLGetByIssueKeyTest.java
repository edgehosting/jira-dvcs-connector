package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.google.common.collect.Lists;
import net.java.ao.test.jdbc.NonTransactional;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * This is a database integration test that uses the AO database test parent class to provide us with a working database
 * and connection.
 */
public class PullRequestQDSLGetByIssueKeyTest extends QueryDSLDBTest
{
    @Test
    @NonTransactional
    public void testSimpleSearchMapsProperly() throws Exception
    {
        List<PullRequest> pullRequests = pullRequestQDSL.getByIssueKeys(ISSUE_KEYS, BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));

        PullRequest pullRequest = pullRequests.get(0);

        assertThat(pullRequest.getRemoteId(), equalTo(pullRequestMappingWithIssue.getRemoteId()));
        assertThat(pullRequest.getRepositoryId(), equalTo(pullRequestMappingWithIssue.getToRepositoryId()));
        assertThat(pullRequest.getName(), equalTo(pullRequestMappingWithIssue.getName()));
        assertThat(pullRequest.getUrl(), equalTo(pullRequestMappingWithIssue.getUrl()));
        assertThat(pullRequest.getStatus().name(), equalTo(pullRequestMappingWithIssue.getLastStatus()));
        assertThat(pullRequest.getCreatedOn(), equalTo(pullRequestMappingWithIssue.getCreatedOn()));
        assertThat(pullRequest.getUpdatedOn(), equalTo(pullRequestMappingWithIssue.getUpdatedOn()));
        assertThat(pullRequest.getAuthor(), equalTo(pullRequestMappingWithIssue.getAuthor()));
        assertThat(pullRequest.getCommentCount(), equalTo(pullRequestMappingWithIssue.getCommentCount()));
        assertThat(pullRequest.getExecutedBy(), equalTo(pullRequestMappingWithIssue.getExecutedBy()));

        assertThat(pullRequest.getIssueKeys(), containsInAnyOrder(ISSUE_KEY));

        assertThat(pullRequest.getParticipants().size(), equalTo(1));
        Participant participant = pullRequest.getParticipants().get(0);

        assertThat(participant.getUsername(), equalTo(participant.getUsername()));
        assertThat(participant.isApproved(), equalTo(participant.isApproved()));
        assertThat(participant.getRole(), equalTo(participant.getRole()));
    }

    @Test
    @NonTransactional
    public void testTwoIssueKeys()
    {
        final String secondKey = "SCN-2";
        pullRequestAOPopulator.associateToIssue(pullRequestMappingWithIssue, secondKey);

        List<PullRequest> pullRequests = pullRequestQDSL.getByIssueKeys(Lists.newArrayList(ISSUE_KEY, secondKey), BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));

        PullRequest pullRequest = pullRequests.get(0);
        assertThat(pullRequest.getIssueKeys(), containsInAnyOrder(ISSUE_KEY, secondKey));
    }

    @Test
    @NonTransactional
    public void testSimpleSearchMapsProperlyAcrossRepositoryAndOrg() throws Exception
    {
        OrganizationMapping org2 = organizationAOPopulator.create("Github", "gitbhu.", "gh fork");
        RepositoryMapping repo2 = repositoryAOPopulator.createRepository(org2, false, true, "fh/fork");
        pullRequestAOPopulator.createPR("something else", "other key", repo2);

        List<PullRequest> pullRequests = pullRequestQDSL.getByIssueKeys(ISSUE_KEYS, BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));
    }

    @Test
    @NonTransactional
    public void testWithTwoParticipants() throws Exception
    {
        final String user2 = "bill";
        pullRequestAOPopulator.createParticipant(user2, true, "someguy", pullRequestMappingWithIssue);

        List<PullRequest> pullRequests = pullRequestQDSL.getByIssueKeys(ISSUE_KEYS, BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));

        PullRequest pullRequest = pullRequests.get(0);

        assertThat(pullRequest.getRemoteId(), equalTo(pullRequestMappingWithIssue.getRemoteId()));
        assertThat(pullRequest.getRepositoryId(), equalTo(pullRequestMappingWithIssue.getToRepositoryId()));
        assertThat(pullRequest.getName(), equalTo(pullRequestMappingWithIssue.getName()));
        assertThat(pullRequest.getUrl(), equalTo(pullRequestMappingWithIssue.getUrl()));
        assertThat(pullRequest.getStatus().name(), equalTo(pullRequestMappingWithIssue.getLastStatus()));
        assertThat(pullRequest.getCreatedOn(), equalTo(pullRequestMappingWithIssue.getCreatedOn()));
        assertThat(pullRequest.getUpdatedOn(), equalTo(pullRequestMappingWithIssue.getUpdatedOn()));
        assertThat(pullRequest.getAuthor(), equalTo(pullRequestMappingWithIssue.getAuthor()));
        assertThat(pullRequest.getCommentCount(), equalTo(pullRequestMappingWithIssue.getCommentCount()));
        assertThat(pullRequest.getExecutedBy(), equalTo(pullRequestMappingWithIssue.getExecutedBy()));

        final List<Participant> participants = pullRequest.getParticipants();
        assertThat(participants.size(), equalTo(2));
        assertThat(Lists.newArrayList(participants.get(0).getUsername(), participants.get(1).getUsername()), containsInAnyOrder(user2, pullRequestParticipant.getUsername()));
    }

    @Test
    @NonTransactional
    public void testWithNoParticipant() throws Exception
    {
        final String secondIssueKey = "IK-2";
        RepositoryPullRequestMapping secondPR = pullRequestAOPopulator.createPR("something else", secondIssueKey, enabledRepository);

        List<PullRequest> pullRequests = pullRequestQDSL.getByIssueKeys(Arrays.asList(secondIssueKey), BITBUCKET);

        assertThat(pullRequests.size(), equalTo(1));
        assertThat(pullRequests.get(0).getId(), equalTo(secondPR.getID()));
    }

    @Test
    @NonTransactional
    public void testWithTwoPRsTwoKeys() throws Exception
    {
        final String secondIssueKey = "IK-2";
        pullRequestAOPopulator.createPR("something else", secondIssueKey, enabledRepository);

        List<PullRequest> pullRequests = pullRequestQDSL.getByIssueKeys(Arrays.asList(ISSUE_KEY, secondIssueKey), BITBUCKET);

        assertThat(pullRequests.size(), equalTo(2));
    }
}
