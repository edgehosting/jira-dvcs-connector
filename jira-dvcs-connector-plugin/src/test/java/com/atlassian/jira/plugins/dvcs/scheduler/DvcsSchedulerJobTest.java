package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsModuleMetaData;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.scheduler.compat.JobInfo;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DvcsSchedulerJobTest
{
    // Constants
    private static final int ACTIVE_ORGANIZATION_ID = 21;
    private static final int ORPHAN_ORGANIZATION_ID = 22;

    // Fixture
    private DvcsSchedulerJob job;
    @Mock private JobInfo mockJobInfo;
    @Mock private OrganizationService mockOrganizationService;
    @Mock private RepositoryService mockRepositoryService;
    @Mock private ActiveObjects activeObjects;
    @Mock private ActiveObjectsModuleMetaData activeObjectsModuleMetaData;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(activeObjects.moduleMetaData()).thenReturn(activeObjectsModuleMetaData);
        when(activeObjectsModuleMetaData.isDataSourcePresent()).thenReturn(true);

        job = new DvcsSchedulerJob(mockOrganizationService, mockRepositoryService, activeObjects);
    }

    @Test
    public void jobRunDoesTheJobWhenDataSourceIsPresent() throws Exception
    {
        // Set up
        mockDataSourcePresent(true);

        final Organization mockOrganization = mock(Organization.class);
        when(mockOrganizationService.getAll(false)).thenReturn(singletonList(mockOrganization));
        final Repository mockActiveRepository = getMockRepository(ACTIVE_ORGANIZATION_ID, mockOrganization);
        final Repository mockOrphanRepository = getMockRepository(ORPHAN_ORGANIZATION_ID, null);
        when(mockRepositoryService.getAllRepositories(true))
                .thenReturn(asList(mockActiveRepository, mockOrphanRepository));

        // Invoke
        job.execute(mockJobInfo);

        // Check
        verify(mockRepositoryService).syncRepositoryList(mockOrganization);
        verify(mockRepositoryService).removeOrphanRepositories(singletonList(mockOrphanRepository));
    }

    @Test
    public void jobRunDoesNotDoTheJobWhenDataSourceIsNOTPresent() throws Exception
    {
        // Set up
        mockDataSourcePresent(false);

        // Invoke
        job.execute(mockJobInfo);

        // Check
        verifyNoMoreInteractions(mockOrganizationService);
        verifyNoMoreInteractions(mockRepositoryService);
    }

    private void mockDataSourcePresent(final boolean isPresent)
    {
        when(activeObjectsModuleMetaData.isDataSourcePresent()).thenReturn(isPresent);
    }

    private Repository getMockRepository(final int organizationId, final Organization organization)
    {
        when(mockOrganizationService.get(organizationId, false)).thenReturn(organization);
        final Repository mockRepository = mock(Repository.class);
        when(mockRepository.getOrganizationId()).thenReturn(organizationId);
        return mockRepository;
    }
}
