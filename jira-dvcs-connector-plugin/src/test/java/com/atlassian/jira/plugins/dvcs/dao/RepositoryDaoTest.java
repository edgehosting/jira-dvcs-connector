package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToProjectMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.RepositoryDaoImpl;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.RepositoryTransformer;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import net.java.ao.Query;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings ("unchecked")
public class RepositoryDaoTest
{

    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private Synchronizer synchronizer;

    @Mock
    private OrganizationMapping organizationMapping;

    @Mock
    private RepositoryMapping repositoryMapping;


    private String[] sampleProjectKeys = { "TEST", "ASDF", "PROJ" };

    // tested object
    private RepositoryDao repositoryDao;

    private static final Date SAMPLE_DATE = new Date();

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        repositoryDao = new RepositoryDaoImpl(activeObjects);
        ReflectionTestUtils.setField(repositoryDao, "synchronizer", synchronizer);
        ReflectionTestUtils.setField(repositoryDao, "repositoryTransformer", new RepositoryTransformer());
    }

    @Test
    public void testSave()
    {

        Repository sampleRepository = createSampleRepository();
        when(activeObjects.get(eq(OrganizationMapping.class), eq(1))).thenReturn(
                organizationMapping);
        when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(
                new Answer<Object>()
                {
                    @SuppressWarnings ("rawtypes")
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
                    }
                });
        when(activeObjects.create(eq(RepositoryMapping.class), isA(Map.class))).thenReturn(
                repositoryMapping);
        when(activeObjects.find(eq(RepositoryMapping.class), anyString(), any())).thenReturn(
                new RepositoryMapping[] { repositoryMapping });

        repositoryDao.save(sampleRepository);

        verify(activeObjects).create(eq(RepositoryMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
                    public boolean matches(Object argument)
                    {
                        Map<String, Object> values = (Map<String, Object>) argument;
                        boolean val = true;
                        val = values.get(RepositoryMapping.ORGANIZATION_ID).equals(1)
                                && values.get(RepositoryMapping.SLUG).equals("doesnotmatter-repo")
                                && values.get(RepositoryMapping.NAME).equals("doesnotmatter_repo")
                                && values.get(RepositoryMapping.LAST_COMMIT_DATE).equals(SAMPLE_DATE)
                                && values.get(RepositoryMapping.LINKED).equals(true)
                                && values.get(RepositoryMapping.DELETED).equals(true);
                        return val;
                    }
                }));
    }

    @Test
    public void testUpdate()
    {
        Repository sampleRepository = createSampleRepository();
        sampleRepository.setId(85);

        when(activeObjects.get(eq(RepositoryMapping.class), eq(85))).thenReturn(
                repositoryMapping);
        when(activeObjects.get(eq(OrganizationMapping.class), eq(1))).thenReturn(
                organizationMapping);
        when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(
                new Answer<Object>()
                {
                    @SuppressWarnings ("rawtypes")
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
                    }
                });
        when(activeObjects.create(eq(RepositoryMapping.class), isA(Map.class))).thenReturn(
                repositoryMapping);

        repositoryDao.save(sampleRepository);

        verify(repositoryMapping).setSlug(eq("doesnotmatter-repo"));
        verify(repositoryMapping).setName(eq("doesnotmatter_repo"));
        verify(repositoryMapping).setLastCommitDate(eq(SAMPLE_DATE));
        verify(repositoryMapping).setLinked(eq(true));
        verify(repositoryMapping).setDeleted(eq(true));

        verify(repositoryMapping).save();
    }

    @Test
    public void testGetPreviouslyLinkedProjects()
    {
        RepositoryToProjectMapping[] sampleMappings = createSampleMappings();
        when(activeObjects.find(eq(RepositoryToProjectMapping.class), any(Query.class))).thenReturn(
                sampleMappings);
        Assert.assertEquals(repositoryDao.getPreviouslyLinkedProjects(1), Arrays.asList(sampleProjectKeys));

    }

    private Repository createSampleRepository()
    {
        Repository repository = new Repository();
        repository.setName("doesnotmatter_repo");
        repository.setDvcsType("bitbucket");
        repository.setOrganizationId(1);
        repository.setSlug("doesnotmatter-repo");
        repository.setLastCommitDate(SAMPLE_DATE);
        repository.setLinked(true);
        repository.setDeleted(true);
        return repository;
    }

    private RepositoryToProjectMapping[] createSampleMappings()
    {
        RepositoryToProjectMapping[] sampleMappings = new RepositoryToProjectMapping[3];

        for (int i = 0; i < 3; i++)
        {
            sampleMappings[i] = mock(RepositoryToProjectMapping.class);
            when(sampleMappings[i].getProjectKey()).thenReturn(sampleProjectKeys[i]);
            when(sampleMappings[i].getRepository()).thenReturn(repositoryMapping);
        }
        return sampleMappings;


    }


}
