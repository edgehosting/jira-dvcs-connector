package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.PullRequestRef;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class EventsJsonMarshallingTest
{
    ObjectMapper objectMapper;
    ImmutableSet<String> issueKeys;

    Branch branch;
    PullRequest pullRequest;
    Changeset changeset;

    @BeforeMethod
    public void setUp() throws Exception
    {
        objectMapper = new ObjectMapper();
        issueKeys = ImmutableSet.of("ISSUE-1", "ISSUE-2");
    }

    @BeforeMethod
    public void setUpBranch() throws Exception
    {
        branch = new Branch(1, "my-branch", 2);
        BranchHead branchHead = new BranchHead("my-branch", "123");
        branch.setHeads(ImmutableList.of(branchHead));
    }

    @BeforeMethod
    public void setUpChangeset() throws Exception
    {
        changeset = new Changeset(1,
                "abcdef",
                "raw_author",
                "author",
                new Date(),
                "rawNode",
                "branch",
                "message",
                ImmutableList.of("parent1", "parent2"),
                ImmutableList.of(new ChangesetFile(ChangesetFileAction.ADDED, "readme.txt")),
                1,
                "author_email");
    }

    @BeforeMethod
    public void setUpPullRequest() throws Exception
    {
        pullRequest = new PullRequest(1);
        pullRequest.setAuthor("author");
        pullRequest.setCommentCount(23);
        pullRequest.setCreatedOn(new Date());
        pullRequest.setDestination(new PullRequestRef("dest-branch", "dest-repo", "dest-repo-url"));
        pullRequest.setName("my PR");
        pullRequest.setParticipants(ImmutableList.of(new Participant("a_user", true, "a_role")));
        pullRequest.setRemoteId(123);
        pullRequest.setRepositoryId(254);
        pullRequest.setSource(new PullRequestRef("source-branch", "source-repo", "source-repo-url"));
        pullRequest.setStatus(PullRequestStatus.OPEN);
        pullRequest.setUpdatedOn(new Date());
        pullRequest.setUrl("pr_url");
    }

    @Test
    public void branchCreated() throws Exception
    {
        assertThat(convertToJsonThenBackTo(new BranchCreatedEvent(branch, issueKeys, new Date())), instanceOf(BranchCreatedEvent.class));
    }

    @Test
    public void changesetCreated() throws Exception
    {
        assertThat(convertToJsonThenBackTo(new ChangesetCreatedEvent(changeset, issueKeys)), instanceOf(ChangesetCreatedEvent.class));
    }

    @Test
    public void changesetCreatedWithFileDetails() throws Exception
    {
        // when there are details both the files and fileDetails properties will return a  list of ChangesetFileDetail
        ImmutableList<ChangesetFileDetail> filesWithDetails = ImmutableList.of(new ChangesetFileDetail(ChangesetFileAction.ADDED, "readme.txt", 23, 0));
        changeset.setFiles(filesWithDetails);
        changeset.setFileDetails(filesWithDetails);

        assertThat(convertToJsonThenBackTo(new ChangesetCreatedEvent(changeset, issueKeys)), instanceOf(ChangesetCreatedEvent.class));
    }

    @Test
    public void pullRequestCreated() throws Exception
    {
        assertThat(convertToJsonThenBackTo(new PullRequestCreatedEvent(pullRequest)), instanceOf(PullRequestCreatedEvent.class));
    }

    @Test
    public void pullRequestUpdated() throws Exception
    {
        assertThat(convertToJsonThenBackTo(new PullRequestUpdatedEvent(pullRequest, pullRequest)), instanceOf(PullRequestUpdatedEvent.class));
    }

    @Test
    public void issuesChanged() throws Exception
    {
        final IssuesChangedEvent issueChangedEvent = convertToJsonThenBackTo(new IssuesChangedEvent(1, issueKeys));
        assertThat(issueChangedEvent, instanceOf(IssuesChangedEvent.class));
        assertThat(issueChangedEvent.getIssueKeys().containsAll(issueKeys), is(true));
        assertThat(issueChangedEvent.getRepositoryId(), is(1));
    }

    private <T> T convertToJsonThenBackTo(T item) throws IOException
    {
        String json = objectMapper.writeValueAsString(item);

        //noinspection unchecked
        return (T) objectMapper.readValue(json, item.getClass());
    }
}
