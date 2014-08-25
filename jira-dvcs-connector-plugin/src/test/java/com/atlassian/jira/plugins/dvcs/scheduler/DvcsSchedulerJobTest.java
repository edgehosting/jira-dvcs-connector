package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.scheduler.compat.JobInfo;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DvcsSchedulerJobTest
{
    // Constants
    private static final int ACTIVE_ORGANIZATION_ID = 21;
    private static final int ORPHAN_ORGANIZATION_ID = 22;
    private static final int UNAUTZ_ORGANISATION_ID = 23;
    private static final int ACTIVE_2_ORGANIZATION_ID = 24;

    // Fixture
    private DvcsSchedulerJob job;
    @Mock private JobInfo mockJobInfo;
    @Mock private OrganizationService mockOrganizationService;
    @Mock private RepositoryService mockRepositoryService;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        job = new DvcsSchedulerJob(mockOrganizationService, mockRepositoryService);
    }

    @Test
    public void testRunJob() throws Exception
    {
        // Set up
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
    public void testExpiredOrganizationCredential() throws Exception
    {
        final Organization org1 = new Organization(ACTIVE_ORGANIZATION_ID, "", "", "", false, null, "", false, Collections.<Group>emptySet());
        final Organization org2 = new Organization(UNAUTZ_ORGANISATION_ID, "", "", "", false, null, "", false, Collections.<Group>emptySet());
        final Organization org3 = new Organization(ACTIVE_2_ORGANIZATION_ID, "", "", "", false, null, "", false, Collections.<Group>emptySet());

        when(mockOrganizationService.getAll(anyBoolean())).thenReturn(newArrayList(org1, org2, org3));

        doThrow(new SourceControlException.UnauthorisedException("Stubbed authorization exception"))
            .when(mockRepositoryService).syncRepositoryList(eq(org2));

        job.execute(mockJobInfo);

        verify(mockRepositoryService).syncRepositoryList(org1);
        verify(mockRepositoryService).syncRepositoryList(org2);
        verify(mockRepositoryService).syncRepositoryList(org3);
    }

    private Repository getMockRepository(final int organizationId, final Organization organization)
    {
        when(mockOrganizationService.get(organizationId, false)).thenReturn(organization);
        final Repository mockRepository = mock(Repository.class);
        when(mockRepository.getOrganizationId()).thenReturn(organizationId);
        return mockRepository;
    }
}
