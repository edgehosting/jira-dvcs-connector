package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientRemoteFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.RepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
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
    
    private final static Pattern PATTERN_PROJECTS_IN_LINK_REX = Pattern.compile("[A-Z|a-z]{2,}(|)+");

    public BitbucketLinkerImpl(BitbucketClientRemoteFactory bitbucketClientRemoteFactory,
            ApplicationProperties applicationProperties, ProjectManager projectManager)
    {
        this.bitbucketClientRemoteFactory = bitbucketClientRemoteFactory;
        this.projectManager = projectManager;
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
    public void linkRepository(Repository repository,  List<String> projectsInChangesets)
    {
        if (CollectionUtils.isEmpty(projectsInChangesets)) {
            return;
        }
        
        List<BitbucketRepositoryLink> currentLinks = getCurrentLinks(repository);

        List<BitbucketRepositoryLink> linksToRemove = calculateLinksToThisJira(currentLinks);
    
        if (log.isDebugEnabled())
        {
            log.debug("Configuring links for " + repository.getRepositoryUrl() + ". " + "existingLinks: "
                    + currentLinks + ", linksToRemove: " + linksToRemove);
        }

        removeLinks(repository, linksToRemove);

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
                    normalize(baseUrl) + "\\1",
                    constructProjectsRex(forProjects));

        } catch (BitbucketRequestException e)
        {
            log.error("Error adding Repository Link [" + baseUrl + ", " + repository.getName() + "] to " + repository.getRepositoryUrl()
                    + ": " + e.getMessage());
        }
    }

    private String constructProjectsRex(List<String> forProjects)
    {
        return "(?<!\\w)((" + joinBy(forProjects, "|") + ")-\\d+)(?!\\w)";
    }

    private String joinBy(List<String> collection, String separator)
    {
        return Joiner.on(separator).join(collection);
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

    @Override
    public void linkRepositoryIncremental(Repository repository, List<String> withProjectKeys)
    {
        if (CollectionUtils.isEmpty( withProjectKeys )) {
            return;
        }

        List<BitbucketRepositoryLink> currentLinks = getCurrentLinks(repository);
        List<BitbucketRepositoryLink> linksToThisJira = calculateLinksToThisJira(currentLinks);

        if (linksToThisJira.isEmpty()) {
            
            addLink(repository, withProjectKeys);
            
        } else {
            for (BitbucketRepositoryLink configuredLink : linksToThisJira)
            {
                String projectKeysInLink = getProjectKeysFromLinkOrNull(configuredLink.getHandler().getRawRegex());
                
                if (StringUtils.isNotBlank(projectKeysInLink)) {
                    
                    List<String> linkedProjectsAsList = linkedProjectsAsList(projectKeysInLink);
                    
                    if (isNewProjectLinksRequested(linkedProjectsAsList, withProjectKeys)) {
                        addLink(repository, uniqueUnion(linkedProjectsAsList, withProjectKeys));
                    } else {
                        return;
                    }
    
                    // do not search for other links
                    break;
                }
                
            }
            //
            removeLinks(repository, linksToThisJira);

        }
    }

    @SuppressWarnings("all")
    private List<String> uniqueUnion(List<String> linkedProjectsAsList, List<String> withProjectKeys)
    {
        Collection united = CollectionUtils.union(linkedProjectsAsList, withProjectKeys);
        return new ArrayList<String>(Sets.newHashSet(united));
    }

    private ArrayList<String> linkedProjectsAsList(String projectKeysInLink)
    {
        return Lists.newArrayList(Splitter.on("|").split(projectKeysInLink));
    }
    
    
    private boolean isNewProjectLinksRequested(List<String> projectsInLink, List<String> withProjectKeysCanContainNew)
    {
        
        for (String eventualNewProjectToLink : withProjectKeysCanContainNew)
        {
            if (!projectsInLink.contains(eventualNewProjectToLink)) {
                return true;
            }
        }
        
        return false;
    }

    private String getProjectKeysFromLinkOrNull(String rex) {
        
        try
        {
            Matcher matcher = PATTERN_PROJECTS_IN_LINK_REX.matcher(rex);
            matcher.find();
            return matcher.group(0);
            
        } catch (Exception e)
        {
            return null;
        }
        
    }

    private String normalize(String url)
    {
        if (url.endsWith("/")) {
            return url + "browse/";
        }

        return url + "/browse/";
    }

    private List<BitbucketRepositoryLink> calculateLinksToThisJira(List<BitbucketRepositoryLink> currentlyLinkedProjects)
    {
        List<BitbucketRepositoryLink> linksToRemove = Lists.newArrayList();
        for (BitbucketRepositoryLink repositoryLink : currentlyLinkedProjects)
        {
            // make sure that is of type jira or custom (new version of linking)
            String replacementUrl = repositoryLink.getHandler().getReplacementUrl().toLowerCase();
            if (isCustomOrJiraType(repositoryLink)
                    // remove links just to OUR jira instance
                    && replacementUrl.startsWith(baseUrl.toLowerCase()))
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
