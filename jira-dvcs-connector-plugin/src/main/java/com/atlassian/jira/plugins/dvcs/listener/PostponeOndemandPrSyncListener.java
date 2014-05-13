package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Date;
import java.util.Random;

public class PostponeOndemandPrSyncListener implements InitializingBean, DisposableBean
{

    private static final long POSTPONE_PR_SYNC_FRAME_MS = /* two days */ 2 * 24 * 60 * 60 * 1000;
    private static final String POSTPONE_PR_SYNC_UNTIL = "plugin.dvcs.prsyncpostpone";
    private static final String POSTPONE_GITHUB_PR_SYNC_UNTIL = "plugin.dvcs.prsyncpostpone.github";
    private static final Logger log = LoggerFactory.getLogger(PostponeOndemandPrSyncListener.class);

    private final EventPublisher eventPublisher;
    private final FeatureManager featureManager;
    private final PluginSettings pluginSettings;

    public PostponeOndemandPrSyncListener(EventPublisher eventPublisher, FeatureManager featureManager, PluginSettingsFactory settings)
    {
        super();
        this.eventPublisher = eventPublisher;
        this.featureManager = featureManager;
        this.pluginSettings = settings.createGlobalSettings();
    }

    @EventListener
    public void pluginInstalled(PluginEnabledEvent event)
    {
        if ("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin".equals(event.getPlugin().getKey())
                && featureManager.isEnabled(CoreFeatures.ON_DEMAND))
        {
            String savedSetting = (String) pluginSettings.get(POSTPONE_PR_SYNC_UNTIL);
            if (savedSetting == null)
            {
                long postpone = randomPostponeTimeWithinTimeWindow();
                long postponeUntil = postpone + System.currentTimeMillis();

                pluginSettings.put(POSTPONE_PR_SYNC_UNTIL, postponeUntil + "");

                log.info("Pull request synchronization will be postponed until " + new Date(postponeUntil));
            }
        }
    }

    public boolean isAfterPostponedTime()
    {
        if (!featureManager.isEnabled(CoreFeatures.ON_DEMAND))
        {
            return true;
        }

        try
        {
            String savedSetting = (String) pluginSettings.get(POSTPONE_PR_SYNC_UNTIL);
            long until = Long.parseLong(savedSetting);

            return System.currentTimeMillis() > until;

        } catch (NumberFormatException e)
        {
            log.warn("Failed to get expected setting property: " + POSTPONE_PR_SYNC_UNTIL + ". " + e.getMessage());
            return false;
        }

    }

    private static long randomPostponeTimeWithinTimeWindow()
    {
        return (long) (new Random().nextDouble() * POSTPONE_PR_SYNC_FRAME_MS);
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        eventPublisher.register(this);
    }

    @Override
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }
}
