package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketRemoteClientFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLinkHandlerName;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Implementation of BitbucketLinker that configures repository links on bitbucket repositories
 * 
 * https://confluence.atlassian.com/display/BITBUCKET/Repository+links
 */
public class BitbucketLinkerImpl implements BitbucketLinker
{
    private final Logger log = LoggerFactory.getLogger(BitbucketLinkerImpl.class);
    private final String baseUrl;
	private final ProjectManager projectManager;
	private final Encryptor encryptor;

	public BitbucketLinkerImpl(ApplicationProperties applicationProperties,
	        ProjectManager projectManager, Encryptor encryptor)
    {
		this.projectManager = projectManager;
		this.encryptor = encryptor;
        baseUrl = applicationProperties.getBaseUrl();
    }

    @Override
    public void unlinkRepository(Repository repository)
    {
    	List<BitbucketRepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(repository);
		if (log.isDebugEnabled())
		{
			log.debug("Configuring links for "+repository.getRepositoryUrl() + ". " +
					"LinksToRemove: " + currentlyLinkedProjects);
		}
    	removeLinks(repository, currentlyLinkedProjects);
    }
    
	@Override
    public void linkRepository(Repository repository)
	{
		Set<String> projectsList = getProjectKeys(); 
		
		List<BitbucketRepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(repository);
		
		List<BitbucketRepositoryLink> linksToRemove = calculateLinksToRemove(currentlyLinkedProjects, projectsList);
		List<String> linksToAdd = calculateLinksToAdd(currentlyLinkedProjects, projectsList);
		if (log.isDebugEnabled())
		{
			log.debug("Configuring links for "+repository.getRepositoryUrl() + ". " +
					"All projects: " + projectsList +
					", existingLinks: " + currentlyLinkedProjects +
					", linksToRemove: " + linksToRemove +
					", linksToAdd:" + linksToAdd);
		}
		
		removeLinks(repository, linksToRemove);
		addLinks(repository, linksToAdd);
	}
    
    private Set<String> getProjectKeys()
    {
    	Set<String> projectKeys = Sets.newHashSet();
		List<Project> projectObjects = projectManager.getProjectObjects();
		for (Project project : projectObjects)
        {
			projectKeys.add(project.getKey());
        }
		return projectKeys; 
    }

	private void addLinks(Repository repository, List<String> linksToAdd)
    {
        RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint =
                BitbucketRemoteClientFactory.fromRepository(repository).getRepositoryLinksRest();
        
        for (String key : linksToAdd)
        {
            String owner = repository.getOrgName();
            String slug = repository.getSlug();

            try
            {
                repositoryLinkRemoteRestpoint.addRepositoryLink(owner,
                                                                slug,
                                                                BitbucketRepositoryLinkHandlerName.JIRA.toString(),
                                                                baseUrl,
                                                                key);
            } catch (BitbucketRequestException e)
            {
                log.error("Error adding Repository Link [" + baseUrl + ", " + key + "] to "
                    + repository.getRepositoryUrl() + ": " + e.getMessage());
            }
        }
       
//        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(repository);
//        for (String key : linksToAdd)
//        {
//            String owner = repository.getOrgName();
//            String slug = repository.getSlug();
//
//            try
//            {
//                repositoryLinksService.addRepositoryLink(owner, slug, RepositoryLink.TYPE_JIRA, baseUrl, key);
//            } catch (BitbucketClientException e)
//            {
//                log.error("Error adding Repository Link [" + baseUrl + ", " + key + "] to "
//                    + repository.getRepositoryUrl() + ": " + e.getMessage());
//            }
//        }
    }

    public void removeLinks(Repository repository, List<BitbucketRepositoryLink> linksToRemove)
    {
        RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint =
                BitbucketRemoteClientFactory.fromRepository(repository).getRepositoryLinksRest();

        for (BitbucketRepositoryLink repositoryLink : linksToRemove)
        {
            String owner = repository.getOrgName();
            String slug = repository.getSlug();

            try
            {
                repositoryLinkRemoteRestpoint.removeRepositoryLink(owner, slug, repositoryLink.getId());
            } catch (BitbucketRequestException e)
            {
                log.error("Error removing Repository Link [" + repositoryLink + "] from "
                    + repository.getRepositoryUrl() + ": " + e.getMessage());
            }
            
        }
        
//        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(repository);
//        for (RepositoryLink repositoryLink : linksToRemove)
//        {
//            String owner = repository.getOrgName();
//            String slug = repository.getSlug();
//
//            try
//            {
//                repositoryLinksService.removeRepositoryLink(owner, slug, repositoryLink.getId());
//            } catch (BitbucketClientException e)
//            {
//                log.error("Error removing Repository Link [" + repositoryLink + "] from "
//                    + repository.getRepositoryUrl() + ": " + e.getMessage());
//            }
//            
//        }
    }

    private List<BitbucketRepositoryLink> calculateLinksToRemove(List<BitbucketRepositoryLink> currentlyLinkedProjects, Set<String> projectsList)
    {
        List<BitbucketRepositoryLink> linksToRemove = Lists.newArrayList();
        for (BitbucketRepositoryLink repositoryLink : currentlyLinkedProjects)
        {
            if (!projectsList.contains(repositoryLink.getHandler().getKey())                // remove links to the project that doen't exist in our jira
                && BitbucketRepositoryLinkHandlerName.JIRA.toString().equals(repositoryLink.getHandler().getName())   // make sure this is the jira type link
//                && RepositoryLink.TYPE_JIRA.equals(repositoryLink.getHandler().getName())   // make sure this is the jira type link
                && baseUrl.equals(repositoryLink.getHandler().getUrl()))                    // make sure we are only removing links to OUR jira instance
            {
                linksToRemove.add(repositoryLink);
            }
        }
        return linksToRemove;
    }

    private List<String> calculateLinksToAdd(List<BitbucketRepositoryLink> currentlyLinkedProjects, Set<String> projectsList)
    {
        // find which projects are not linked yet
        Set<String> projectsToAdd = Sets.newHashSet(projectsList);
        for (BitbucketRepositoryLink repositoryLink : currentlyLinkedProjects)
        {
            if (projectsToAdd.contains(repositoryLink.getHandler().getKey()))
            {
                projectsToAdd.remove(repositoryLink.getHandler().getKey());
            }
        }
        
        // create link objects to be added
        List<String> linksToAdd = Lists.newArrayList();
        for (String projectKey : projectsToAdd)
        {
            linksToAdd.add(projectKey);
        }
        return linksToAdd;
    }

    private List<BitbucketRepositoryLink> getCurrentlyLinkedProjects(Repository repository)
    {
        RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint =
                BitbucketRemoteClientFactory.fromRepository(repository).getRepositoryLinksRest();

        try
        {
            String owner = repository.getOrgName();
            String slug = repository.getSlug();
            return repositoryLinkRemoteRestpoint.getRepositoryLinks(owner, slug);
        } catch (BitbucketRequestException e)
        {
            log.error("Error retrieving Repository links from " + repository.getRepositoryUrl());
            return Collections.emptyList();
        }
        
//        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(repository);
//        try
//        {
//            String owner = repository.getOrgName();
//            String slug = repository.getSlug();
//            return repositoryLinksService.getRepositoryLinks(owner, slug);
//        } catch (BitbucketClientException e)
//        {
//            log.error("Error retrieving Repository links from " + repository.getRepositoryUrl());
//            return Collections.emptyList();
//        }
    }

//    private RepositoryLinksService getRepositoryLinksService(Repository repository)
//    {
//		String apiUrl = BitbucketCommunicator.getApiUrl(repository.getOrgHostUrl());
//        BitbucketClient bitbucketClient = new BitbucketClient(apiUrl);
//		String unencryptedPassword = encryptor.decrypt(repository.getCredential()
//		        .getAdminPassword(), repository.getOrgName(), repository.getOrgHostUrl());
//        bitbucketClient.setAuthorisation(repository.getCredential().getAdminUsername(), unencryptedPassword);
//		return new RepositoryLinksService(bitbucketClient);
//    }

//    private RepositoryLinkRemoteRestpoint getRepositoryLinkRemoteRestpoint(Repository repository)
//    {
////		String unencryptedPassword = encryptor.decrypt(repository.getCredential().getAdminPassword(),
////                                                       repository.getOrgName(),
////                                                       repository.getOrgHostUrl());
//        
////        BitbucketRemoteClient bitbucketRemoteClient =
////                BitbucketRemoteClientFactory.fromBasicAuthorizationData(repository,
////                                                                        repository.getCredential().getAdminUsername(),
////                                                                        repository.getCredential().getAdminPassword()/*unencryptedPassword*/);
//        
//        return bitbucketRemoteClient.getRepositoryLinksRest();
//    }
}
//TODO netbeans action list neupdatuje spravne cisla dole, aj ked sa obsah updatuje spravne