package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.BitbucketClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.BitbucketClientException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.RepositoryLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.RepositoryLinksService;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.collect.Lists;

/**
 * Implementation of BitbucketLinker that configures repository links on
 * bitbucket repositories
 * 
 * https://confluence.atlassian.com/display/BITBUCKET/Repository+links
 */
public class BitbucketLinkerImpl implements BitbucketLinker
{
	private final Logger log = LoggerFactory.getLogger(BitbucketLinkerImpl.class);
	private final String baseUrl;
	private final Encryptor encryptor;

	public BitbucketLinkerImpl(ApplicationProperties applicationProperties,	Encryptor encryptor)
	{
		this.encryptor = encryptor;
		baseUrl = applicationProperties.getBaseUrl();
	}

	@Override
	public void unlinkRepository(Repository repository)
	{
		List<RepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(repository);

		if (log.isDebugEnabled())
		{
			log.debug("Configuring links for " + repository.getRepositoryUrl() + ". " + "LinksToRemove: "
					+ currentlyLinkedProjects);
		}

		removeLinks(repository, currentlyLinkedProjects);
	}

	@Override
	public void linkRepository(Repository repository)
	{
		List<RepositoryLink> currentlyLinkedProjects = getCurrentlyLinkedProjects(repository);

		List<RepositoryLink> linksToRemove = calculateLinksToRemove(currentlyLinkedProjects);
	
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
			RepositoryLinksService repositoryLinksService = getRepositoryLinksService(repository);
			repositoryLinksService.addCustomRepositoryLink(repository.getOrgName(), repository.getSlug(), baseUrl);

		} catch (BitbucketClientException e)
		{
			log.error("Error adding Repository Link [" + baseUrl + ", " + repository.getName() + "] to " + repository.getRepositoryUrl()
					+ ": " + e.getMessage());
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

	private List<RepositoryLink> calculateLinksToRemove(List<RepositoryLink> currentlyLinkedProjects)
	{
		List<RepositoryLink> linksToRemove = Lists.newArrayList();
		for (RepositoryLink repositoryLink : currentlyLinkedProjects)
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

	private boolean isCustomOrJiraType(RepositoryLink repositoryLink)
	{
		return RepositoryLink.TYPE_JIRA.equals(repositoryLink.getHandler().getName()) || 
			  RepositoryLink.TYPE_CUSTOM.equals(repositoryLink.getHandler().getName());
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
		String apiUrl = repository.getOrgHostUrl() + "/!api/1.0";

		BitbucketClient bitbucketClient = new BitbucketClient(apiUrl);
		String unencryptedPassword = encryptor.decrypt(repository.getCredential().getAdminPassword(),
				repository.getOrgName(), repository.getOrgHostUrl());
		bitbucketClient.setAuthorisation(repository.getCredential().getAdminUsername(), unencryptedPassword);
		
		return new RepositoryLinksService(bitbucketClient);
	}
}
