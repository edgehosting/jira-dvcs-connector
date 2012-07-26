package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientRemoteFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.RepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.collect.Lists;

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

    public BitbucketLinkerImpl(BitbucketClientRemoteFactory bitbucketClientRemoteFactory,
            ApplicationProperties applicationProperties)
    {
        this.bitbucketClientRemoteFactory = bitbucketClientRemoteFactory;
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
        List<BitbucketRepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(repository);

        List<BitbucketRepositoryLink> linksToRemove = calculateLinksToRemove(currentlyLinkedProjects);
    
        if (log.isDebugEnabled())
        {
            log.debug("Configuring links for " + repository.getRepositoryUrl() + ". " + "existingLinks: "
                    + currentlyLinkedProjects + ", linksToRemove: " + linksToRemove);
        }

        removeLinks(repository, linksToRemove);
        addLink(repository);
    }
    

    private void addLink(Repository repository)
    {

        try
        {
            RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint =
                    bitbucketClientRemoteFactory.getForRepository(repository).getRepositoryLinksRest();
            
            repositoryLinkRemoteRestpoint.addCustomRepositoryLink(
                    repository.getOrgName(), 
                    repository.getSlug(), 
                    normalize(repository.getOrgHostUrl()),
                    encode("(?<!\\w)([A-Z|a-z]{2,}-\\d+)(?!\\w)"));

        } catch (BitbucketRequestException e)
        {
            log.error("Error adding Repository Link [" + baseUrl + ", " + repository.getName() + "] to " + repository.getRepositoryUrl()
                    + ": " + e.getMessage());
        }
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

 
    private List<BitbucketRepositoryLink> getCurrentlyLinkedProjects(Repository repository)
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
