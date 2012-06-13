package com.atlassian.jira.plugins.dvcs.dao;

import static org.mockito.Matchers.isA;

import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.RepositoryDaoImpl;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.transaction.TransactionCallback;

@SuppressWarnings("unchecked")
public class RepositoryDaoTest
{

	private static final Date SAMPLE_DATE = new Date();

	@Mock
	private ActiveObjects activeObjects;

	@Mock
	private Synchronizer synchronizer;

	@Mock
	private OrganizationMapping organizationMapping;

	@Mock
	private RepositoryMapping repositoryMapping;

	// tested object
	private RepositoryDao repositoryDao;

	public RepositoryDaoTest()
	{
		super();
	}

	@Before
	public void setup()
	{
		MockitoAnnotations.initMocks(this);
		repositoryDao = new RepositoryDaoImpl(activeObjects, synchronizer);
	}

	@Test
	public void testSave()
	{

		Repository sampleRepository = createSampleRepository();
		Mockito.when(activeObjects.get(Mockito.eq(OrganizationMapping.class), Mockito.eq(1))).thenReturn(
				organizationMapping);
		Mockito.when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(
				new Answer<Object>()
				{
					@SuppressWarnings("rawtypes")
					@Override
					public Object answer(InvocationOnMock invocationOnMock) throws Throwable
					{
						return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
					}
				});
		Mockito.when(activeObjects.create(Mockito.eq(RepositoryMapping.class), Mockito.isA(Map.class))).thenReturn(
				repositoryMapping);

		repositoryDao.save(sampleRepository);

		Mockito.verify(activeObjects).create(Mockito.eq(RepositoryMapping.class),
				Mockito.argThat(new ArgumentMatcher<Map<String, Object>>()
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

		Mockito.when(activeObjects.get(Mockito.eq(RepositoryMapping.class), Mockito.eq(85))).thenReturn(
				repositoryMapping);
		Mockito.when(activeObjects.get(Mockito.eq(OrganizationMapping.class), Mockito.eq(1))).thenReturn(
				organizationMapping);
		Mockito.when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(
				new Answer<Object>()
				{
					@SuppressWarnings("rawtypes")
					@Override
					public Object answer(InvocationOnMock invocationOnMock) throws Throwable
					{
						return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
					}
				});
		Mockito.when(activeObjects.create(Mockito.eq(RepositoryMapping.class), Mockito.isA(Map.class))).thenReturn(
				repositoryMapping);

		repositoryDao.save(sampleRepository);

		Mockito.verify(repositoryMapping).setSlug(Mockito.eq("doesnotmatter-repo"));
		Mockito.verify(repositoryMapping).setName(Mockito.eq("doesnotmatter_repo"));
		Mockito.verify(repositoryMapping).setLastCommitDate(Mockito.eq(SAMPLE_DATE));
		Mockito.verify(repositoryMapping).setLinked(Mockito.eq(true));
		Mockito.verify(repositoryMapping).setDeleted(Mockito.eq(true));

		Mockito.verify(repositoryMapping).save();
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

}
