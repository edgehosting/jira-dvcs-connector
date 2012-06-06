package com.atlassian.jira.plugins.dvcs.sync.impl;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Executors;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * @author Martin Skurla
 */
public final class TestDefaultSynchronizer
{
    private Repository        repositoryMock;
    private ChangesetService  changesetServiceMock;

    private Capture<Changeset> savedChangeset = new Capture<Changeset>();

    private Changeset changeset = new Changeset(123, "node", "message MES-123 text", new Date());

    @Before
    public void initializeMocks()
    {
        repositoryMock        = createNiceMock(Repository.class);
        changesetServiceMock  = createNiceMock(ChangesetService.class);
    }

    @After
    public void verifyMocks()
    {
        verify(repositoryMock, changesetServiceMock);
    }

    //TODO if soft sync, bude sa volat lastCommitDate, inak nie???

    @Test
    public void testSoftSynchronize_ShouldAddSingleMapping() throws InterruptedException
    {
        Date lastCommitDate = new Date();

        expect(repositoryMock.getLastCommitDate()).andReturn(lastCommitDate);

        expect(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock), eq(lastCommitDate)))
                .andReturn(Collections.singletonList(changeset));
        expect(changesetServiceMock.save(capture(savedChangeset))).andReturn(null);

        replay(repositoryMock, changesetServiceMock);

        SynchronisationOperation synchronisationOperation =
                new DefaultSynchronisationOperation(repositoryMock,
                                                    createNiceMock(RepositoryService.class),
                                                    changesetServiceMock,
                                                    true); // soft sync

        Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor());
        synchronizer.synchronize(repositoryMock, synchronisationOperation);

        waitUntilProgressEnds(synchronizer);

        assertThat(savedChangeset.getValue().getIssueKey(), is("MES-123"));
    }

    private void waitUntilProgressEnds(Synchronizer synchronizer) throws InterruptedException
    {
        Progress progress = synchronizer.getProgress(repositoryMock);

        while (!progress.isFinished())
        {
            Thread.sleep(50);
        }
    }
}
