package com.atlassian.jira.plugins.dvcs.dao.impl.querydsl;

import com.atlassian.jira.plugins.dvcs.dao.impl.DAOConstants;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.pocketknife.api.querydsl.QueryFactory;
import com.atlassian.pocketknife.api.querydsl.SchemaProvider;
import com.google.common.collect.ImmutableList;
import com.mysema.query.Tuple;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

public class PullRequestQDSLByIssueKeyClosureTest
{
    private static final Integer PR_MAPPING_ID = 5;
    private static final String ISSUE_KEY = "HHH-123";
    private static final String PARTICIPANT_NAME = "bob";

    @Mock
    private QueryFactory queryFactory;

    @Mock
    private SchemaProvider schemaProvider;

    @Mock
    private Tuple tuple;

    private PullRequestQDSL.PullRequestByIssueKeyClosure issueKeyClosure;
    private Map<Integer, PullRequest> pullRequestsById;
    private PullRequest existingPullRequest;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);

        existingPullRequest = new PullRequest(PR_MAPPING_ID);
        existingPullRequest.setId(PR_MAPPING_ID);
        existingPullRequest.getIssueKeys().add(ISSUE_KEY);

        pullRequestsById = new HashMap<Integer, PullRequest>();

        when(schemaProvider.getSchema(argThat(any(String.class)))).thenReturn("something");
        issueKeyClosure = new PullRequestQDSL.PullRequestByIssueKeyClosure(BITBUCKET, ImmutableList.of(ISSUE_KEY), schemaProvider);

        when(tuple.get(issueKeyClosure.prMapping.ID)).thenReturn(PR_MAPPING_ID);
        when(tuple.get(issueKeyClosure.participantMapping.USERNAME)).thenReturn(PARTICIPANT_NAME);

        when(tuple.get(issueKeyClosure.issueMapping.ISSUE_KEY)).thenReturn(ISSUE_KEY);
        // let the other values return null, they won't hurt

        pullRequestsById.put(PR_MAPPING_ID, existingPullRequest);
    }

    @Test
    public void testAddsOneToEmpty()
    {
        pullRequestsById.clear();
        issueKeyClosure.getFoldFunction().apply(pullRequestsById, tuple);

        assertThat(pullRequestsById.size(), equalTo(1));
        assertThat(pullRequestsById.get(PR_MAPPING_ID).getParticipants().size(), equalTo(1));
        assertThat(pullRequestsById.get(PR_MAPPING_ID).getIssueKeys(), containsInAnyOrder(ISSUE_KEY));
    }

    @Test
    public void testDoesNotUpdateOnDuplicateIssue()
    {
        issueKeyClosure.getFoldFunction().apply(pullRequestsById, tuple);

        assertThat(pullRequestsById.size(), equalTo(1));
        assertThat(pullRequestsById.get(PR_MAPPING_ID).getParticipants().size(), equalTo(1));
        assertThat(pullRequestsById.get(PR_MAPPING_ID).getIssueKeys(), containsInAnyOrder(ISSUE_KEY));
    }

    @Test
    public void testDoesNotUpdateOnDuplicateParticipant()
    {
        existingPullRequest.setParticipants(new ArrayList<Participant>());
        final String role = "dev";
        final Participant participant = new Participant(PARTICIPANT_NAME, true, role);
        existingPullRequest.getParticipants().add(participant);
        when(tuple.get(issueKeyClosure.participantMapping.APPROVED)).thenReturn(true);
        when(tuple.get(issueKeyClosure.participantMapping.ROLE)).thenReturn(role);

        issueKeyClosure.getFoldFunction().apply(pullRequestsById, tuple);

        assertThat(pullRequestsById.size(), equalTo(1));
        assertThat(pullRequestsById.get(PR_MAPPING_ID).getParticipants().size(), equalTo(1));
        assertThat(pullRequestsById.get(PR_MAPPING_ID).getParticipants(), containsInAnyOrder(participant));
        assertThat(pullRequestsById.get(PR_MAPPING_ID).getIssueKeys(), containsInAnyOrder(ISSUE_KEY));
    }

    @Test
    public void testStopsAtLimit()
    {
        pullRequestsById.clear();

        for (int i = 0; i < DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY; i++)
        {
            int id = 1 + i + PR_MAPPING_ID;
            pullRequestsById.put(id, new PullRequest(id));
        }

        assertThat(pullRequestsById.size(), equalTo(DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY));

        issueKeyClosure.getFoldFunction().apply(pullRequestsById, tuple);

        assertThat(pullRequestsById.size(), equalTo(DAOConstants.MAXIMUM_ENTITIES_PER_ISSUE_KEY));
        assertThat(pullRequestsById.keySet(), not(contains(PR_MAPPING_ID)));
    }
}
