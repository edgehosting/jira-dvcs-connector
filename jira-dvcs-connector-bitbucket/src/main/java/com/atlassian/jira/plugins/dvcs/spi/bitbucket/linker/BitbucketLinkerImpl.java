package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
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
import com.atlassian.sal.api.UrlMode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>
 * Implementation of BitbucketLinker that configures repository links on bitbucket repositories
 * </p>
 * https://confluence.atlassian.com/display/BITBUCKET/Repository+links
 */
@Component ("bitbucketLinker")
public class BitbucketLinkerImpl implements BitbucketLinker
{
    private final Logger log = LoggerFactory.getLogger(BitbucketLinkerImpl.class);

    private final BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    private final ProjectManager projectManager;
    private final ApplicationProperties applicationProperties;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    public BitbucketLinkerImpl(BitbucketClientBuilderFactory bitbucketClientBuilderFactory,
            @ComponentImport ApplicationProperties applicationProperties, @ComponentImport ProjectManager projectManager)
    {
        this.bitbucketClientBuilderFactory = checkNotNull(bitbucketClientBuilderFactory);
        this.projectManager = checkNotNull(projectManager);
        this.applicationProperties = checkNotNull(applicationProperties);
    }

    @VisibleForTesting
    public BitbucketLinkerImpl(BitbucketClientBuilderFactory bitbucketClientBuilderFactory,
            @ComponentImport ApplicationProperties applicationProperties, @ComponentImport ProjectManager projectManager,
            final RepositoryService repositoryService)
    {
        this(bitbucketClientBuilderFactory, applicationProperties, projectManager);
        this.repositoryService = repositoryService;
    }

    /**
     * Remove forward slash at the end of url.
     *
     * @param url a url to be processed
     * @return the url supplied with no "/" at the end of it
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

    /**
     * Removes existing links to this jira, adds a link for the keys in {@code projectKeysToLink} that exist in this jira
     *
     * @param repository repository to replace links to
     * @param projectKeysToLink a set of project keys to be linked
     */
    @Override
    public void linkRepository(Repository repository, Set<String> projectKeysToLink)
    {
        Set<String> previouslyLinkedProjects = new HashSet<String>();
        previouslyLinkedProjects.addAll(repositoryService.getPreviouslyLinkedProjects(repository));

        Set<String> projectKeysInJira = getProjectKeysInJira();

        projectKeysToLink.retainAll(projectKeysInJira);

        if (previouslyLinkedProjects.equals(projectKeysToLink))
        {
            return;
        }

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

    /**
     * Creates a link in {@code repository} for the project keys in {@code forProjects}
     *
     * @param repository repository to install link into
     * @param forProjects project keys to be added
     */
    private void addLink(Repository repository, Set<String> forProjects)
    {
        try
        {
            if (forProjects.isEmpty())
            {
                log.debug("No projects to link");
                return;
            }
            RepositoryLinkRemoteRestpoint repositoryLinkRemoteRestpoint = bitbucketClientBuilderFactory.forRepository(repository).closeIdleConnections().build().getRepositoryLinksRest();
            repositoryLinkRemoteRestpoint.addCustomRepositoryLink(repository.getOrgName(), repository.getSlug(),
                   getRepositoryLinkUrl() , constructProjectsRex(forProjects));
            repositoryService.setPreviouslyLinkedProjects(repository, forProjects);
            repository.setUpdateLinkAuthorised(true);
            repositoryService.save(repository);
        }
        catch (BitbucketRequestException.Forbidden_403 e)
        {
            log.info("Bitbucket Account not authorised to install Repository Link on " + repository.getRepositoryUrl());
            repository.setUpdateLinkAuthorised(false);
            repositoryService.save(repository);
        }
        catch(BitbucketRequestException e){
            log.info("Error adding Repository Link [" + getBaseUrl() + ", " + repository.getName() + "] to "
                    + repository.getRepositoryUrl() + ": " + e.getMessage() + " REX: " + constructProjectsRex(forProjects), e);
        }
    }

    private String getRepositoryLinkUrl(){
        return getBaseUrl() + "/browse/\\1";
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
                repositoryService.setPreviouslyLinkedProjects(repository, new HashSet<String>());
            }
            catch (BitbucketRequestException e)
            {
                log.error("Error removing Repository Link [" + repositoryLink + "] from "
                        + repository.getRepositoryUrl() + ": " + e.getMessage());
            }
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
     *
     * @param repository repository to get links from
     * @return list of BitBucketRepositoryLinks that link to this jira instance from the {@code repository}
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
            return filterLinksToThisJira(allRepositoryLinks);

        }
        catch (BitbucketRequestException e)
        {
            log.error("Error retrieving Repository links from " + repository.getRepositoryUrl());
            return Collections.emptyList();
        }
    }

    /**
     * @param currentBitbucketLinks List of all bitbucket links in a given repository
     * @return BitbucketRepositoryLinks that point to this jira instance
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
                if (displayTo != null && displayTo.toLowerCase().startsWith(getBaseUrl().toLowerCase()))
                {
                    // remove links just to OUR jira instance
                    linksToThisJira.add(repositoryLink);
                }
            }
        }
        return linksToThisJira;
    }

    private String getBaseUrl()
    {
        return normaliseBaseUrl(applicationProperties.getBaseUrl(UrlMode.CANONICAL));
    }
}
