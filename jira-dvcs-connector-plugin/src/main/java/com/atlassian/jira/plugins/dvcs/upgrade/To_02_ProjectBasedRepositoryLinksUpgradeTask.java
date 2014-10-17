package com.atlassian.jira.plugins.dvcs.upgrade;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.plugins.dvcs.util.DvcsConstants.PLUGIN_KEY;

/**
 * For Bitbucket.
 */
@ExportAsService (PluginUpgradeTask.class)
@Component
public class To_02_ProjectBasedRepositoryLinksUpgradeTask implements PluginUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_02_ProjectBasedRepositoryLinksUpgradeTask.class);
    private final RepositoryService repositoryService;
    private final ChangesetService changesetService;
    private final DvcsCommunicator communicator;

    @Autowired
    public To_02_ProjectBasedRepositoryLinksUpgradeTask(@Qualifier ("bitbucketCommunicator") DvcsCommunicator communicator,
            RepositoryService repositoryService, ChangesetService changesetService)
    {
        this.communicator = communicator;
        this.repositoryService = repositoryService;
        this.changesetService = changesetService;
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    // Upgrade
    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    @Override
    public Collection<Message> doUpgrade() throws Exception
    {
        doUpgradeInternal();
        return Collections.emptyList();
    }

    private void doUpgradeInternal()
    {
        List<Repository> allRepositories = repositoryService.getAllRepositories();
        for (Repository repository : allRepositories)
        {
            try
            {
                if (repository.isLinked())
                {
                    log.debug("LINKING {} repository.", repository.getName());
                    communicator.linkRepository(repository,
                            changesetService.findReferencedProjects(repository.getId()));
                }
            }
            catch (Exception e)
            {
                log.warn("Failed to link repository {}. " + e.getMessage(), repository.getName());
            }
        }
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    @Override
    public int getBuildNumber()
    {
        return 2;
    }

    @Override
    public String getShortDescription()
    {
        return "Upgrades the repository links at Bitbucket with custom handlers (regexp) base on project keys present int this JIRA instance.";
    }

    @Override
    public String getPluginKey()
    {
        return PLUGIN_KEY;
    }
}
