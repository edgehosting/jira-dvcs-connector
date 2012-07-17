package com.atlassian.jira.plugins.dvcs.upgrade;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.google.common.collect.Lists;

/**
 * For Bitbucket.
 */
public class RepositoryLinksUpgradeTask implements PluginUpgradeTask
{
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(RepositoryLinksUpgradeTask.class);
	
	private final BitbucketLinker linker;

	private final RepositoryService repositoryService;

	public RepositoryLinksUpgradeTask(@Qualifier("bitbucketLinker") BitbucketLinker linker,
			RepositoryService repositoryService)
	{
		super();
		this.linker = linker;
		this.repositoryService = repositoryService;
	}

	// -------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------
	// Upgrade
	// -------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------

	@Override
	public Collection<Message> doUpgrade() throws Exception
	{
		new Thread("Thread___" + RepositoryLinksUpgradeTask.class.getSimpleName())
		{

			@Override
			public void run()
			{
				doUpgradeInternal();
			}

		}.start();

		return Lists.newLinkedList();
	}

	private void doUpgradeInternal()
	{
		List<Repository> allRepositories = repositoryService.getAllRepositories();
		
		for (Repository repository : allRepositories)
		{
			// do not use defferedBitbucketLinker as this invocations needs to go in order
			
			log.debug("UNLINKING {} repository.", repository.getName());
			linker.unlinkRepository(repository);
			
			log.debug("LINKING {} repository.", repository.getName());
			linker.linkRepository(repository);
		}
	}

	// -------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------

	@Override
	public int getBuildNumber()
	{
		return 1;
	}

	@Override
	public String getShortDescription()
	{
		return "Upgrades the repository links at Bitbucket with custom handlers (regexp).";
	}

	@Override
	public String getPluginKey()
	{
		return "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";
	}

}
