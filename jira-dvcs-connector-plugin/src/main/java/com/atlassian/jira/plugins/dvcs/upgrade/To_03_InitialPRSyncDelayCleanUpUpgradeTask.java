package com.atlassian.jira.plugins.dvcs.upgrade;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Cleaning up plugin property for initial pull request delay
 *
 */
public class To_03_InitialPRSyncDelayCleanUpUpgradeTask implements PluginUpgradeTask
{
    private static final Logger LOGGER = LoggerFactory.getLogger(To_03_InitialPRSyncDelayCleanUpUpgradeTask.class);
    private static final String POSTPONE_PR_SYNC_UNTIL = "plugin.dvcs.prsyncpostpone";

    private final PluginSettings pluginSettings;
    private final FeatureManager featureManager;

    public To_03_InitialPRSyncDelayCleanUpUpgradeTask(final PluginSettingsFactory pluginSettingsFactory, final FeatureManager featureManager)
    {
        this.featureManager = featureManager;
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    @Override
    public Collection<Message> doUpgrade() throws Exception
    {
        if (featureManager.isEnabled(CoreFeatures.ON_DEMAND))
        {
            LOGGER.debug("Removing '{}' property", POSTPONE_PR_SYNC_UNTIL);
            try
            {
                pluginSettings.remove(POSTPONE_PR_SYNC_UNTIL);
            }
            catch (Exception e)
            {
                LOGGER.info("'" + POSTPONE_PR_SYNC_UNTIL + "' property could not be removed.", e);
            }
        }

        return Collections.emptyList();
    }

    @Override
    public int getBuildNumber()
    {
        return 3;
    }

    @Override
    public String getShortDescription()
    {
        return "Cleans up 'plugin.dvcs.prsyncpostpone' plugin property";
    }

    @Override
    public String getPluginKey()
    {
        return DvcsConstants.PLUGIN_KEY;
    }
}
