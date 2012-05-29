package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.BitbucketClient;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.BitbucketClientException;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.RepositoryLink;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.RepositoryLinksService;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Repository;
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
        this.baseUrl = applicationProperties.getBaseUrl();
    }

    @Override
    public void unlinkRepository(Repository repository)
    {
    	List<RepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(repository);
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
		
		List<RepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(repository);
		
		List<RepositoryLink> linksToRemove = calculateLinksToRemove(currentlyLinkedProjects, projectsList);
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
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(repository);
        for (String key : linksToAdd)
        {
            String owner = repository.getOrgName();
            String slug = repository.getSlug();

            try
            {
                repositoryLinksService.addRepositoryLink(owner, slug, RepositoryLink.TYPE_JIRA, baseUrl, key);
            } catch (BitbucketClientException e)
            {
                log.error("Error adding Repository Link [" + baseUrl + ", " + key + "] to "
                    + repository.getRepositoryUrl() + ": " + e.getMessage());
            }
        }
    }

    public void removeLinks(Repository repository, List<RepositoryLink> linksToRemove)
    {
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(repository);
        for (RepositoryLink repositoryLink : linksToRemove)
        {
            String owner = repository.getOrgName();
            String slug = repository.getSlug();

            try
            {
                repositoryLinksService.removeRepositoryLink(owner, slug, repositoryLink.getId());
            } catch (BitbucketClientException e)
            {
                log.error("Error removing Repository Link [" + repositoryLink + "] from "
                    + repository.getRepositoryUrl() + ": " + e.getMessage());
            }
            
        }
    }

    private List<RepositoryLink> calculateLinksToRemove(List<RepositoryLink> currentlyLinkedProjects, Set<String> projectsList)
    {
        List<RepositoryLink> linksToRemove = Lists.newArrayList();
        for (RepositoryLink repositoryLink : currentlyLinkedProjects)
        {
            if (!projectsList.contains(repositoryLink.getHandler().getKey())                // remove links to the project that doen't exist in our jira
                && RepositoryLink.TYPE_JIRA.equals(repositoryLink.getHandler().getName())   // make sure this is the jira type link
                && baseUrl.equals(repositoryLink.getHandler().getUrl()))                    // make sure we are only removing links to OUR jira instance
            {
                linksToRemove.add(repositoryLink);
            }
        }
        return linksToRemove;
    }

    private List<String> calculateLinksToAdd(List<RepositoryLink> currentlyLinkedProjects, Set<String> projectsList)
    {
        // find which projects are not linked yet
        Set<String> projectsToAdd = Sets.newHashSet(projectsList);
        for (RepositoryLink repositoryLink : currentlyLinkedProjects)
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

    private List<RepositoryLink> getCurrentlyLinkedProjects(Repository repository)
    {
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(repository);
        try
        {
            String owner = repository.getOrgName();
            String slug = repository.getSlug();
            return repositoryLinksService.getRepositoryLinks(owner, slug);
        } catch (BitbucketClientException e)
        {
            log.error("Error retrieving Repository links from " + repository.getRepositoryUrl());
            return Collections.emptyList();
        }
    }
    
    private RepositoryLinksService getRepositoryLinksService(Repository repository)
    {
        // TODO is there a better way to get apiUrl?
		String apiUrl = repository.getOrgHostUrl() + "/!api/1.0";
        BitbucketClient bitbucketClient = new BitbucketClient(apiUrl);
		String unencryptedPassword = encryptor.decrypt(repository.getCredential()
		        .getAdminPassword(), repository.getOrgName(), repository.getOrgHostUrl());
        bitbucketClient.setAuthorisation(repository.getCredential().getAdminUsername(), unencryptedPassword);
		return new RepositoryLinksService(bitbucketClient);
    }
}
