package com.atlassian.jira.plugins.dvcs.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
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
		repositoryService = new RepositoryServiceImpl(dvcsCommunicatorProvider, repositoryDao, synchronizer, changesetService, applicationProperties);

	}
	
	@Test
	public void testDisableRepository() {
		
		Repository sampleRepository = createSampleRepository();
		Mockito.when(repositoryDao.get(0)).thenReturn(sampleRepository);
		Mockito.when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
		Mockito.when(applicationProperties.getBaseUrl()).thenReturn("https://bitbucket.org");
		
		repositoryService.enableRepository(0, false);
		
		Mockito.verify(repositoryDao).save(sampleRepository);
		Mockito.verify(bitbucketCommunicator).removePostcommitHook(Mockito.eq(sampleRepository),
				Mockito.eq(createPostcommitUrl(sampleRepository)));
	}

	@Test
	public void testEnableRepository() {
		
		Repository sampleRepository = createSampleRepository();
		Mockito.when(repositoryDao.get(0)).thenReturn(sampleRepository);
		Mockito.when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
		Mockito.when(applicationProperties.getBaseUrl()).thenReturn("https://bitbucket.org");
		
		repositoryService.enableRepository(0, true);
		
		Mockito.verify(repositoryDao).save(sampleRepository);
		Mockito.verify(bitbucketCommunicator).setupPostcommitHook(Mockito.eq(sampleRepository),
				Mockito.eq(createPostcommitUrl(sampleRepository)));
		
	}
	
	private String createPostcommitUrl(Repository forRepo) {
		return  "https://bitbucket.org" +  "/rest/bitbucket/1.0/repository/" + forRepo.getId() + "/sync";
		
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

}

