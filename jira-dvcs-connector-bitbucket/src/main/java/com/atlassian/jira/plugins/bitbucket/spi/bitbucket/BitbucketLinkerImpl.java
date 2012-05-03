package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.api.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.BitbucketClient;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.BitbucketClientException;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.RepositoryLink;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.RepositoryLinksService;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class BitbucketLinkerImpl implements BitbucketLinker
{
    private final Logger log = LoggerFactory.getLogger(BitbucketRepositoryManager.class);

    private final String baseUrl;

    public BitbucketLinkerImpl(ApplicationProperties applicationProperties)
    {
        this.baseUrl = applicationProperties.getBaseUrl();
    }

    /* (non-Javadoc)
     * @see com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketLinker#setConfiguration(com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping, java.util.Set)
     */
    @Override
    public void setConfiguration(OrganizationMapping om, Set<String> projectsList)
    {
        List<SourceControlRepository> repositories = getRepositories(om);
        for (SourceControlRepository sourceControlRepository : repositories)
        {
            List<RepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(sourceControlRepository);
            
            List<RepositoryLink> linksToRemove = calculateLinksToRemove(currentlyLinkedProjects, projectsList);
            List<String> linksToAdd = calculateLinksToAdd(currentlyLinkedProjects, projectsList);
            
            removeLinks(sourceControlRepository, linksToRemove);
            addLinks(sourceControlRepository, linksToAdd);
        }
    }
    
    public void addLinks(SourceControlRepository sourceControlRepository, List<String> linksToAdd)
    {
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(sourceControlRepository);
        for (String key : linksToAdd)
        {
            String owner = sourceControlRepository.getRepositoryUri().getOwner();
            String slug = sourceControlRepository.getRepositoryUri().getSlug();

            try
            {
                repositoryLinksService.addRepositoryLink(owner, slug, RepositoryLink.TYPE_JIRA, baseUrl, key);
            } catch (BitbucketClientException e)
            {
                log.error("Error adding Repository Link [" + baseUrl + ", " + key + "] to "
                    + sourceControlRepository.getRepositoryUri().getRepositoryUrl() + ": " + e.getMessage());
            }
        }
    }

    public void removeLinks(SourceControlRepository sourceControlRepository, List<RepositoryLink> linksToRemove)
    {
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(sourceControlRepository);
        for (RepositoryLink repositoryLink : linksToRemove)
        {
            String owner = sourceControlRepository.getRepositoryUri().getOwner();
            String slug = sourceControlRepository.getRepositoryUri().getSlug();

            try
            {
                repositoryLinksService.removeRepositoryLink(owner, slug, repositoryLink.getId());
            } catch (BitbucketClientException e)
            {
                log.error("Error removing Repository Link [" + repositoryLink + "] from "
                    + sourceControlRepository.getRepositoryUri().getRepositoryUrl() + ": " + e.getMessage());
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

    private List<SourceControlRepository> getRepositories(OrganizationMapping om)
    {
        // TODO Auto-generated method stub
        // from local DB get list of repositories from this organisation. Use only LINKED repositories
        return null;
    }

    private List<RepositoryLink> getCurrentlyLinkedProjects(SourceControlRepository sourceControlRepository)
    {
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(sourceControlRepository);
        try
        {
            String owner = sourceControlRepository.getRepositoryUri().getOwner();
            RepositoryUri slug = sourceControlRepository.getRepositoryUri();
            return repositoryLinksService.getRepositoryLinks(owner, slug.getSlug());
        } catch (BitbucketClientException e)
        {
            log.error("Error retrieving Repository links from " + sourceControlRepository.getRepositoryUri().getRepositoryUrl());
            return Collections.emptyList();
        }
    }

    private RepositoryLinksService getRepositoryLinksService(SourceControlRepository sourceControlRepository)
    {
        return new RepositoryLinksService(new BitbucketClient(sourceControlRepository.getRepositoryUri().getApiUrl()));
    }

}
