package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.google.common.collect.ImmutableSet;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Listeners(MockitoTestNgListener.class)
public class LimitedEventTest
{
    @Mock
    Changeset cs;

    @Mock
    Branch branch;

    Set<String> issueKeys;

    @BeforeMethod
    public void setUp() throws Exception
    {
        issueKeys = ImmutableSet.of("ISSUE-1", "ABC-2");
    }

    @Test
    public void changesetCreatedEventsShouldBeLimited() throws Exception
    {
        assertThat(new ChangesetCreatedEvent(cs, issueKeys).getEventLimit(), equalTo(EventLimit.COMMIT));
    }

    @Test
    public void branchCreatedEventsShouldBeLimited() throws Exception
    {
        assertThat(new BranchCreatedEvent(branch, issueKeys).getEventLimit(), equalTo(EventLimit.BRANCH));
    }
}
