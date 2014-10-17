package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketConstants;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepositoryLinkHandler;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryLinkRemoteRestpoint;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of BitbucketLinker that configures repository links on bitbucket repositories
 * <p/>
 * https://confluence.atlassian.com/display/BITBUCKET/Repository+links
 */
@Component
public class BitbucketLinkerImpl implements BitbucketLinker
{
    private final Logger log = LoggerFactory.getLogger(BitbucketLinkerImpl.class);
    private final String baseUrl;
    private final BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    private final ProjectManager projectManager;

    private final static Pattern PATTERN_PROJECTS_IN_LINK_REX = Pattern.compile("[A-Z|a-z]{2,}(|)+");

    @Autowired
    public BitbucketLinkerImpl(BitbucketClientBuilderFactory bitbucketClientBuilderFactory,
            @ComponentImport ApplicationProperties applicationProperties, @ComponentImport ProjectManager projectManager)
    {
        this.bitbucketClientBuilderFactory = bitbucketClientBuilderFactory;
        this.projectManager = projectManager;
        this.baseUrl = normaliseBaseUrl(applicationProperties.getBaseUrl());
    }

    /**
     * Remove forward slash at the end of url.
     */
    private String normaliseBaseUrl(String url)
    {
        if (StringUtils.isNotBlank(url) && url.endsWith("/"))
        {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    @Override
    public void unlinkRepository(Repository repository)
    {

        List<BitbucketRepositoryLink> currentlyLinkedProjects = getCurrentLinks(repository);

        if (log.isDebugEnabled())
        {
            log.debug("Configuring links for " + repository.getRepositoryUrl() + ". " + "LinksToRemove: "
                    + currentlyLinkedProjects);
        }

        removeLinks(repository, currentlyLinkedProjects);
    }

    @Override
    public void linkRepository(Repository repository, Set<String> projectKeysToLink)
    {
        //
        // remove keys for nonexisting projects

        List<BitbucketRepositoryLink> currentLinks = getCurrentLinks(repository);
        // remove any existing ones
        removeLinks(repository, currentLinks);
        if (log.isDebugEnabled())
        {
            log.debug("Configuring links for " + repository.getRepositoryUrl() + ". Removing existing links: "
                    + currentLinks);
        }

        if (CollectionUtils.isNotEmpty(projectKeysToLink))
        {
            addLink(repository, projectKeysToLink);
        }

    }

    private void addLink(Repository repository, Set<String> forProjects)
    {
        try
        {
            // remove keys for nonexisting projects
            //
            Set<String> projectKeysInJira = getProjectKeysInJira();
            //
            log.debug("Requested links for projects {}.", forProjects);
            log.debug("Projects in JIRA {}.", projectKeysInJira);
            //
            // intersection
            //
            forProjects.retainAll(projectKeysInJira);
            //
            if (forProjects.isEmpty())
            {
                log.debug("No projects to link");
                return;
            }

            //
            // post the link to bitbucket
            //
            RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint = bitbucketClientBuilderFactory.forRepository(repository).closeIdleConnections().build().getRepositoryLinksRest();

            repositoryLinkRemoteRestpoint.addCustomRepositoryLink(repository.getOrgName(), repository.getSlug(),
                    baseUrl + "/browse/\\1", constructProjectsRex(forProjects));

        }
        catch (BitbucketRequestException e)
        {
            log.error("Error adding Repository Link [" + baseUrl + ", " + repository.getName() + "] to "
                    + repository.getRepositoryUrl() + ": " + e.getMessage() + " REX: " + constructProjectsRex(forProjects));
        }
    }

    private String constructProjectsRex(Collection<String> projectKeys)
    {
        return "(?<!\\w)((" + Joiner.on("|").join(projectKeys) + ")-\\d+)(?!\\w)";
    }

    private void removeLinks(Repository repository, List<BitbucketRepositoryLink> linksToRemove)
    {
        RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint = bitbucketClientBuilderFactory.forRepository(repository).closeIdleConnections().build().getRepositoryLinksRest();

        for (BitbucketRepositoryLink repositoryLink : linksToRemove)
        {
            String owner = repository.getOrgName();
            String slug = repository.getSlug();

            try
            {
                repositoryLinkRemoteRestpoint.removeRepositoryLink(owner, slug, repositoryLink.getId());
            }
            catch (BitbucketRequestException e)
            {
                log.error("Error removing Repository Link [" + repositoryLink + "] from "
                        + repository.getRepositoryUrl() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void linkRepositoryIncremental(Repository repository, Set<String> newProjectKeys)
    {

        //
        if (CollectionUtils.isEmpty(newProjectKeys))
        {
            return;
        }

        List<BitbucketRepositoryLink> currentLinks = getCurrentLinks(repository);
        if (currentLinks.isEmpty())
        {
            addLink(repository, newProjectKeys);
            return;
        }

        if (currentLinks.size() == 1)
        {
            Set<String> existingProjectKeys = getProjectKeysFromLinkOrNull(currentLinks.get(0));
            if (existingProjectKeys.containsAll(newProjectKeys))
            {
                // these projects are already linked, no change detected
                return;
            }
            existingProjectKeys.addAll(newProjectKeys);

            // todo add logging
            removeLinks(repository, currentLinks);
            addLink(repository, existingProjectKeys);
        }
        else
        {
            // todo add logging
            removeLinks(repository, currentLinks);
            addLink(repository, newProjectKeys);
        }

    }

    /**
     * TODO: add unit test
     */
    private HashSet<String> getProjectKeysFromLinkOrNull(BitbucketRepositoryLink bitbucketRepositoryLink)
    {
        String regexp = null;
        try
        {
            regexp = bitbucketRepositoryLink.getHandler().getRawRegex();
            Matcher matcher = PATTERN_PROJECTS_IN_LINK_REX.matcher(regexp);
            matcher.find();
            String pipedProjectKeys = matcher.group(0);
            return Sets.newHashSet(Splitter.on("|").split(pipedProjectKeys));
        }
        catch (Exception e)
        {
            log.debug("Failed to parse expression " + regexp + ", cause = " + e.getMessage());
            return null;
        }
    }

    private boolean isCustomOrJiraType(BitbucketRepositoryLink repositoryLink)
    {
        return repositoryLink.getHandler() != null &&
                (BitbucketConstants.REPOSITORY_LINK_TYPE_JIRA.equals(repositoryLink.getHandler().getName())
                        || BitbucketConstants.REPOSITORY_LINK_TYPE_CUSTOM.equals(repositoryLink.getHandler().getName()));
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

    /**
     * Returns BitbucketRepositoryLinks that point to this jira instance
     */
    private List<BitbucketRepositoryLink> getCurrentLinks(Repository repository)
    {
        RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint = bitbucketClientBuilderFactory.forRepository(repository).build().getRepositoryLinksRest();
        try
        {
            String owner = repository.getOrgName();
            String slug = repository.getSlug();
            List<BitbucketRepositoryLink> allRepositoryLinks = repositoryLinkRemoteRestpoint.getRepositoryLinks(owner,
                    slug);
            List<BitbucketRepositoryLink> linksToThisJira = filterLinksToThisJira(allRepositoryLinks);
            return linksToThisJira;

        }
        catch (BitbucketRequestException e)
        {
            log.error("Error retrieving Repository links from " + repository.getRepositoryUrl());
            return Collections.emptyList();
        }
    }

    /**
     * Returns BitbucketRepositoryLink that point to this jira instance
     */
    private List<BitbucketRepositoryLink> filterLinksToThisJira(List<BitbucketRepositoryLink> currentBitbucketLinks)
    {
        List<BitbucketRepositoryLink> linksToThisJira = Lists.newArrayList();
        for (BitbucketRepositoryLink repositoryLink : currentBitbucketLinks)
        {
            // make sure that is of type jira or custom (new version of linking)
            if (isCustomOrJiraType(repositoryLink))
            {
                BitbucketRepositoryLinkHandler handler = repositoryLink.getHandler();
                String displayTo = handler.getDisplayTo();
                if (displayTo != null && displayTo.toLowerCase().startsWith(baseUrl.toLowerCase()))
                {
                    // remove links just to OUR jira instance
                    linksToThisJira.add(repositoryLink);
                }
            }
        }
        return linksToThisJira;
    }

}
