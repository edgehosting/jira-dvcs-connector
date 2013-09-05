package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.impl.ChangesetDaoImpl;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommandsResults;
import com.atlassian.jira.plugins.dvcs.smartcommits.model.CommitCommands;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.plugins.dvcs.sync.impl.DefaultSynchronizer;
import com.atlassian.sal.api.transaction.TransactionCallback;

import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.Executors;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("all")
public class SmartcommitOperationTest
{

    private static final int CHANGESET_ID = 1;
    private static final int REPOSITORY_ID = 100;

    private CommitMessageParser commitMessageParser = new DefaultCommitMessageParser();

	@Mock
	private SmartcommitsService smartcommitsServiceMock;

    @Mock
    private SmartcommitsChangesetsProcessor changesetsProcessorMock;

    @Mock
    private ActiveObjects activeObjectsMock;
    
    @Mock
    private QueryHelper queryHelper;

    @Mock
    private Repository repositoryMock;

    @Mock
    private ChangesetService changesetServiceMock;

    ChangesetDao changesetDao;

	SmartcommitOperation operation;
    private Synchronizer synchronizer;

    public SmartcommitOperationTest()
    {
		super();
	}

    @SuppressWarnings("unchecked")
	@BeforeMethod
	public void setUp()
    {
        MockitoAnnotations.initMocks(this);

		changesetDao = new ChangesetDaoImpl(activeObjectsMock, queryHelper);

        synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(), changesetsProcessorMock);
		operation = new SmartcommitOperation(changesetDao, commitMessageParser, smartcommitsServiceMock, synchronizer, repositoryMock, changesetServiceMock);

        final ChangesetMapping sampleChangesetMapping = (ChangesetMapping) sampleChangesetMapping();

        Mockito.doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                ((EntityStreamCallback)invocation.getArguments()[2]).onRowRead(sampleChangesetMapping);
                return null;
            }

        }).when(activeObjectsMock).stream(Mockito.isA(Class.class), Mockito.isA(Query.class), Mockito.isA(EntityStreamCallback.class));

        when(activeObjectsMock.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
            }
        });


        when (activeObjectsMock.get(eq( ChangesetMapping.class ), eq (CHANGESET_ID) )).thenReturn(sampleChangesetMapping);
        when(repositoryMock.getId()).thenReturn(REPOSITORY_ID);
    }

	@SuppressWarnings("unchecked")
	@Test
	public void testRunOperation ()
    {
        when(smartcommitsServiceMock.doCommands(any(CommitCommands.class))).thenReturn(new CommandsResults());

		operation.run();

		verify(smartcommitsServiceMock).doCommands(any(CommitCommands.class));
	}

    @SuppressWarnings("unchecked")
    @Test
    public void testSmartCommitWithError()
    {
        final ChangesetMapping changesetMapping = sampleChangesetMapping();
        when(activeObjectsMock.find(eq( ChangesetMapping.class ), (Query) any())).thenReturn(new ChangesetMapping[]{changesetMapping});

        final CommandsResults commandsResults = new CommandsResults();
        commandsResults.addGlobalError("errorMsg");
        when(smartcommitsServiceMock.doCommands(any(CommitCommands.class))).thenReturn(commandsResults);

        final DefaultProgress progress = new DefaultProgress();
        synchronizer.putProgress(repositoryMock, progress);

        when(changesetServiceMock.getCommitUrl((Repository) any(), (Changeset) any())).thenReturn("http://host/path");

        operation.run();

        assertThat(progress.getSmartCommitErrors()).hasSize(1);
        assertThat(progress.getSmartCommitErrors().get(0).getShortChangesetNode()).isEqualTo("abcd123");
        assertThat(progress.getSmartCommitErrors().get(0).getCommitUrl()).isEqualTo("http://host/path");
        assertThat(progress.getSmartCommitErrors().get(0).getError()).isEqualTo("errorMsg");

    }



    private ChangesetMapping sampleChangesetMapping()
	{
		ChangesetMapping changesetMappigMock = Mockito.mock(ChangesetMapping.class);
        final RepositoryMapping repositoryMapping = sampleRepositoryMapping();

        when(changesetMappigMock.getID()).thenReturn(CHANGESET_ID);
		when(changesetMappigMock.getRepositories()).thenReturn(new RepositoryMapping[]{repositoryMapping});
		when(changesetMappigMock.getAuthorEmail()).thenReturn("sam@example.com");
        when(changesetMappigMock.getNode()).thenReturn("abcd1234efgh5678");
		when(changesetMappigMock.getMessage()).thenReturn("HAD-4 #comment mighty comment");

		return changesetMappigMock;
	}

    private RepositoryMapping sampleRepositoryMapping() {

        RepositoryMapping repositoryMappingMock = Mockito.mock(RepositoryMapping.class);

        when(repositoryMappingMock.getID()).thenReturn(REPOSITORY_ID);

        return repositoryMappingMock;
    }
}

