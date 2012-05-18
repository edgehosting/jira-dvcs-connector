package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.BitbucketClient;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.BitbucketClientException;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.RepositoryLink;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.clientlibrary.RepositoryLinksService;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class BitbucketLinkerImpl implements BitbucketLinker
{
    private final Logger log = LoggerFactory.getLogger(BitbucketRepositoryManager.class);

    private final String baseUrl;

    private final RepositoryService repositoryService;

    public BitbucketLinkerImpl(ApplicationProperties applicationProperties, RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
        this.baseUrl = applicationProperties.getBaseUrl();
    }

    /* (non-Javadoc)
     * @see com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketLinker#setConfiguration(com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping, java.util.Set)
     */
    @Override
    public void setConfiguration(Organization organization, Set<String> projectsList)
    {
        List<Repository> repositories = getRepositories(organization);
        for (Repository repository : repositories)
        {
            List<RepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(organization, repository);
            
            List<RepositoryLink> linksToRemove = calculateLinksToRemove(currentlyLinkedProjects, projectsList);
            List<String> linksToAdd = calculateLinksToAdd(currentlyLinkedProjects, projectsList);
            
            removeLinks(organization, repository, linksToRemove);
            addLinks(organization, repository, linksToAdd);
        }
    }
    
    public void addLinks(Organization organization, Repository repository, List<String> linksToAdd)
    {
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(organization);
        for (String key : linksToAdd)
        {
            String owner = organization.getName();
            String slug = repository.getSlug();

            try
            {
                repositoryLinksService.addRepositoryLink(owner, slug, RepositoryLink.TYPE_JIRA, baseUrl, key);
            } catch (BitbucketClientException e)
            {
                log.error("Error adding Repository Link [" + baseUrl + ", " + key + "] to "
                    + getRepositoryUrl(organization, repository) + ": " + e.getMessage());
            }
        }
    }

    public void removeLinks(Organization organization, Repository repository, List<RepositoryLink> linksToRemove)
    {
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(organization);
        for (RepositoryLink repositoryLink : linksToRemove)
        {
            String owner = organization.getName();
            String slug = repository.getSlug();

            try
            {
                repositoryLinksService.removeRepositoryLink(owner, slug, repositoryLink.getId());
            } catch (BitbucketClientException e)
            {
                log.error("Error removing Repository Link [" + repositoryLink + "] from "
                    + getRepositoryUrl(organization, repository) + ": " + e.getMessage());
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

    private List<Repository> getRepositories(Organization organization)
    {
        List<Repository> repositories = repositoryService.getAllByOrganization(organization.getId(), false);
        
        Collections2.filter(repositories, new Predicate<Repository>()
        {
            @Override
            public boolean apply(Repository repository)
            {
                return repository.isLinked();
            }
        });
        return repositories;
    }

    private List<RepositoryLink> getCurrentlyLinkedProjects(Organization organization, Repository repository)
    {
        RepositoryLinksService repositoryLinksService = getRepositoryLinksService(organization);
        try
        {
            String owner = organization.getName();
            String slug = repository.getSlug();
            return repositoryLinksService.getRepositoryLinks(owner, slug);
        } catch (BitbucketClientException e)
        {
            log.error("Error retrieving Repository links from " + getRepositoryUrl(organization, repository));
            return Collections.emptyList();
        }
    }

    
    private RepositoryLinksService getRepositoryLinksService(Organization organization)
    {
        // TODO get apiUrl properly
        String apiUrl = organization.getHostUrl()+"/!api/1.0";
        return new RepositoryLinksService(new BitbucketClient(apiUrl));
    }

    /**
     * @param organization
     * @param repository
     * @return
     */
    @Deprecated
    private String getRepositoryUrl(Organization organization, Repository repository)
    {
        return organization.getHostUrl()+"/"+organization.getName()+"/"+repository.getSlug();
    }
}
