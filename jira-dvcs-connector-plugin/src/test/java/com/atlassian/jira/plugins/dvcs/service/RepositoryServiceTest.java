package com.atlassian.jira.plugins.dvcs.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.ApplicationProperties;

public class RepositoryServiceTest
{

	@Mock
	private DvcsCommunicatorProvider dvcsCommunicatorProvider;

	@Mock
	private RepositoryDao repositoryDao;

	@Mock
	private Synchronizer synchronizer;

	@Mock
	private ChangesetService changesetService;

	@Mock
	private ApplicationProperties applicationProperties;

	@Mock
	private DvcsCommunicator bitbucketCommunicator;

	// tested object
	private RepositoryService repositoryService;

	public RepositoryServiceTest()
	{
		super();
	}

	@Before
	public void setup()
	{
		MockitoAnnotations.initMocks(this);
		repositoryService = new RepositoryServiceImpl(dvcsCommunicatorProvider, repositoryDao, synchronizer,
				changesetService, applicationProperties);

	}

	@Test
	public void testDisableRepository()
	{

		Repository sampleRepository = createSampleRepository();
		Mockito.when(repositoryDao.get(0)).thenReturn(sampleRepository);
		Mockito.when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
		Mockito.when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");

		repositoryService.enableRepository(0, false);

		Mockito.verify(repositoryDao).save(sampleRepository);
		Mockito.verify(bitbucketCommunicator).removePostcommitHook(Mockito.eq(sampleRepository),
				Mockito.eq(createPostcommitUrl(sampleRepository)));
	}

	@Test
	public void testEnableRepository()
	{

		Repository sampleRepository = createSampleRepository();
		Mockito.when(repositoryDao.get(0)).thenReturn(sampleRepository);
		Mockito.when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
		Mockito.when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");

		repositoryService.enableRepository(0, true);

		Mockito.verify(repositoryDao).save(sampleRepository);
		Mockito.verify(bitbucketCommunicator).setupPostcommitHook(Mockito.eq(sampleRepository),
				Mockito.eq(createPostcommitUrl(sampleRepository)));

	}

	@Test
	public void testSyncRepositoryList()
	{

		Repository sampleRepository1 = createSampleRepository();
		sampleRepository1.setId(1);
		sampleRepository1.setSlug("sampleRepository1");

		Repository sampleRepository2 = createSampleRepository();
		sampleRepository2.setId(2);
		sampleRepository2.setSlug("sampleRepository2");

		Repository sampleRepository3 = createSampleRepository();
		sampleRepository3.setId(3);
		sampleRepository3.setSlug("sampleRepository3");

		Repository sampleRepository4 = createSampleRepository();
		sampleRepository4.setId(4);
		sampleRepository4.setSlug("sampleRepository4");

		List<Repository> storedRepos = new ArrayList<Repository>();
		storedRepos.add(sampleRepository1);
		storedRepos.add(sampleRepository2);
		List<Repository> remoteRepos = new ArrayList<Repository>();
		// first one deleted, 3, 4 added
		remoteRepos.add(sampleRepository2);
		remoteRepos.add(sampleRepository3);
		remoteRepos.add(sampleRepository4);

		Organization sampleOrganization = new Organization();
		sampleOrganization.setId(5);
		sampleOrganization.setDvcsType("bitbucket");
		sampleOrganization.setAutolinkNewRepos(true);

		Mockito.when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
		Mockito.when(bitbucketCommunicator.getRepositories(sampleOrganization)).thenReturn(remoteRepos);
		Mockito.when(repositoryDao.getAllByOrganization(5, true)).thenReturn(storedRepos);
		Mockito.when(repositoryDao.save(sampleRepository3)).thenReturn(sampleRepository3);
		Mockito.when(repositoryDao.save(sampleRepository4)).thenReturn(sampleRepository4);
		Mockito.when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");
		
		repositoryService.syncRepositoryList(sampleOrganization);

		// 2 has been updated
		Mockito.verify(repositoryDao, Mockito.times(1)).save(sampleRepository2);

		// 1 has been deleted
		Mockito.verify(repositoryDao, Mockito.times(1)).save(Mockito.argThat(new ArgumentMatcher<Repository>()
		{
			@Override
			public boolean matches(Object argument)
			{
				Repository repo = (Repository) argument;
				return repo.getId() == 1 && repo.isDeleted();
			}
		}));

		// 3, 4 has been added
		Mockito.verify(repositoryDao, Mockito.times(2)).save(Mockito.argThat(new ArgumentMatcher<Repository>()
		{
			@Override
			public boolean matches(Object argument)
			{
				Repository repo = (Repository) argument;
				return repo.getId() == 3 || repo.getId() == 4;
			}
		}));
		// ... with false linking
		Mockito.verify(bitbucketCommunicator).setupPostcommitHook(sampleRepository3, createPostcommitUrl(sampleRepository3));
		Mockito.verify(bitbucketCommunicator).setupPostcommitHook(sampleRepository4, createPostcommitUrl(sampleRepository4));
		
	}

	@Test
	public void testRemoveRepository()
	{
		Repository sampleRepository = createSampleRepository();
		sampleRepository.setId(8);

		repositoryService.remove(sampleRepository);

		Mockito.verify(changesetService).removeAllInRepository(8);
		Mockito.verify(repositoryDao).remove(8);
	}

	@Test
	public void testRemoveRepositoryIsLinked()
	{
		Repository sampleRepository = createSampleRepository();
		sampleRepository.setId(8);
		sampleRepository.setLinked(true);
		Mockito.when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
		Mockito.when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");

		repositoryService.remove(sampleRepository);

		Mockito.verify(changesetService).removeAllInRepository(8);
		Mockito.verify(repositoryDao).remove(8);

		Mockito.verify(bitbucketCommunicator).removePostcommitHook(Mockito.eq(sampleRepository),
				Mockito.eq(createPostcommitUrl(sampleRepository)));
	}

	private Repository createSampleRepository()
	{
		Repository repository = new Repository();
		repository.setName("doesnotmatter_repo");
		repository.setDvcsType("bitbucket");
		repository.setOrganizationId(1);
		repository.setSlug("doesnotmatter-repo");
		return repository;
	}
	
	private String createPostcommitUrl(Repository forRepo)
	{
		return "https://myjira.org" + "/rest/bitbucket/1.0/repository/" + forRepo.getId() + "/sync";
	}

}