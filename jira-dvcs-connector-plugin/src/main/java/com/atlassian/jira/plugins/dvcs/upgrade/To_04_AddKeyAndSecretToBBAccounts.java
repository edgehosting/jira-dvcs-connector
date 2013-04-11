package com.atlassian.jira.plugins.dvcs.upgrade;

import java.util.Collection;

import net.java.ao.Query;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;

/**
 *
 */
public class To_04_AddKeyAndSecretToBBAccounts implements PluginUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_04_AddKeyAndSecretToBBAccounts.class);

    private final PluginSettings settings;
    private final ActiveObjects activeObjects;

    public To_04_AddKeyAndSecretToBBAccounts(PluginSettingsFactory pluginSettingsFactory, ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
        this.settings = pluginSettingsFactory.createGlobalSettings();
    }

    /**
     * BBC-479 Add key/secret to the organization
     *
     * @param organization
     */
    @Override
    public Collection<Message> doUpgrade() throws Exception
    {

        final String key = String.valueOf(settings.get("dvcs.connector.bitbucket.clientId"));
        final String secret = String.valueOf(settings.get("dvcs.connector.bitbucket.secret"));
        log.info("Bitbucket key/secret = " + key + " / " + secret);

        if (StringUtils.isBlank(key) || StringUtils.isBlank(secret))
        {
            log.warn("Bitbucket key/secret cannot be blank, skipping upgrade task");
            return null;
        }

        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                Query query = Query.select().where(OrganizationMapping.DVCS_TYPE + " = ? ", "bitbucket");
                OrganizationMapping[] organizationMappings = activeObjects.find(OrganizationMapping.class, query);
                for (OrganizationMapping organizationMapping : organizationMappings)
                {
                    String accessToken = organizationMapping.getAccessToken();
                    String oauthKey = organizationMapping.getOauthKey();
                    String oauthSecret = organizationMapping.getOauthSecret();

                    if (StringUtils.isNotBlank(accessToken) && (StringUtils.isBlank(oauthKey) || StringUtils.isBlank(oauthSecret)))
                    {
                        organizationMapping.setOauthKey(key);
                        organizationMapping.setOauthSecret(secret);
                        organizationMapping.save();
                        log.info("Setting key secret for " + organizationMapping.getHostUrl() + " " + organizationMapping.getName());
                    }

                }
                return null;
            }
        });
        return null;

    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    @Override
    public int getBuildNumber()
    {
        return 4;
    }

    @Override
    public String getShortDescription()
    {
        return "Adding OAuth key/secret to each bitbucket organization.";
    }

    @Override
    public String getPluginKey()
    {
        return "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";
    }

}
