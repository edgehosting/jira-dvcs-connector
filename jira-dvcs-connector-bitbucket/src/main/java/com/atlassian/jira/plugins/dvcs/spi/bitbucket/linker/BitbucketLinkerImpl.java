package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientRemoteFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.RepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Joiner;
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
    private final BitbucketClientRemoteFactory bitbucketClientRemoteFactory;
    private final ProjectManager projectManager;
    private final ChangesetService changesetService;

    public BitbucketLinkerImpl(BitbucketClientRemoteFactory bitbucketClientRemoteFactory,
            ApplicationProperties applicationProperties, ProjectManager projectManager, ChangesetService changesetService)
    {
        this.bitbucketClientRemoteFactory = bitbucketClientRemoteFactory;
        this.projectManager = projectManager;
        this.changesetService = changesetService;
        this.baseUrl = applicationProperties.getBaseUrl();
    }

    @Override
    public void unlinkRepository(Repository repository)
    {
        List<BitbucketRepositoryLink> currentlyLinkedProjects = getCurrentLinks(repository);
        
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
        List<BitbucketRepositoryLink> currentLinks = getCurrentLinks(repository);

        List<BitbucketRepositoryLink> linksToRemove = calculateLinksToRemove(currentLinks);
    
        if (log.isDebugEnabled())
        {
            log.debug("Configuring links for " + repository.getRepositoryUrl() + ". " + "existingLinks: "
                    + currentLinks + ", linksToRemove: " + linksToRemove);
        }

        removeLinks(repository, linksToRemove);

        List<String> projectsInChangesets = changesetService.getOrderedProjectKeysByRepository(repository.getId());
        Set<String> projectKeysInJira = getProjectKeysInJira();
        
        // filter out just these JIRAs' projects
        projectsInChangesets.retainAll(projectKeysInJira);
        
        addLink(repository, projectsInChangesets);
    }

    private void addLink(Repository repository, List<String> forProjects)
    {
        try
        {
            RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint =
                    bitbucketClientRemoteFactory.getForRepository(repository).getRepositoryLinksRest();
            
            repositoryLinkRemoteRestpoint.addCustomRepositoryLink(
                    repository.getOrgName(), 
                    repository.getSlug(), 
                    normalize(repository.getOrgHostUrl()),
                    encode("(?<!\\w)(" + joinBy(forProjects, "|") + "-\\d+)(?!\\w)"));

        } catch (BitbucketRequestException e)
        {
            log.error("Error adding Repository Link [" + baseUrl + ", " + repository.getName() + "] to " + repository.getRepositoryUrl()
                    + ": " + e.getMessage());
        }
    }

    private String joinBy(List<String> collection, String separator)
    {
        return Joiner.on(separator).join(collection);
    }

    private String encode(String url)
    {
        try
        {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void removeLinks(Repository repository, List<BitbucketRepositoryLink> linksToRemove)
    {
        RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint =
                bitbucketClientRemoteFactory.getForRepository(repository).getRepositoryLinksRest();
        
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
    }

    private String normalize(String url)
    {
        if (url.endsWith("/")) {
            return url + "browse/";
        }

        return url + "/browse/";
    }

    private List<BitbucketRepositoryLink> calculateLinksToRemove(List<BitbucketRepositoryLink> currentlyLinkedProjects)
    {
        List<BitbucketRepositoryLink> linksToRemove = Lists.newArrayList();
        for (BitbucketRepositoryLink repositoryLink : currentlyLinkedProjects)
        {
            // make sure that is of type jira or custom (new version of linking)
            if (isCustomOrJiraType(repositoryLink)
                    // remove links just to OUR jira instance
                    && baseUrl.equals(repositoryLink.getHandler().getUrl()))
            {
                linksToRemove.add(repositoryLink);
            }
        }
        return linksToRemove;
    }

    private boolean isCustomOrJiraType(BitbucketRepositoryLink repositoryLink)
    {
        return RepositoryLink.TYPE_JIRA.equals(repositoryLink.getHandler().getName()) || 
              RepositoryLink.TYPE_CUSTOM.equals(repositoryLink.getHandler().getName());
    }

    private Set<String> getProjectKeysInJira()
    {
        // use gcache ?
        Set<String> projectKeys = Sets.newHashSet();
        List<Project> projectObjects = projectManager.getProjectObjects();
        for (Project project : projectObjects)
        {
            projectKeys.add(project.getKey());
        }
        return projectKeys; 
    }
 
    private List<BitbucketRepositoryLink> getCurrentLinks(Repository repository)
    {
        
        RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint =
                bitbucketClientRemoteFactory.getForRepository(repository).getRepositoryLinksRest();
        
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
    }
    
}
