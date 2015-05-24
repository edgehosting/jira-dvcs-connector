package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class To_11_AddKeyAndSecretToBBAccounts implements ActiveObjectsUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_11_AddKeyAndSecretToBBAccounts.class);
    private final PluginSettings settings;

	public To_11_AddKeyAndSecretToBBAccounts(PluginSettingsFactory pluginSettingsFactory)
    {
        this.settings = pluginSettingsFactory.createGlobalSettings();
    }

    /**
     * BBC-479 Add key/secret to the organization
     * @param currentVersion
     * @param activeObjects
     */
	@Override
    public void upgrade(ModelVersion currentVersion, final ActiveObjects activeObjects)
    {
	    log.info("AO Upgrade task  [ " + getModelVersion() + " ] Adding OAuth key/secret to each bitbucket organization.");

	    // start with migrating OAuthData to new place in plugin settings
	    new To_11_UpgradeOAuthDetails(settings).doMigrate();

	    // now add key and secret to database for bitbucket accounts
        addKeyAndSecretToBBAccounts(activeObjects);
    }

    private void addKeyAndSecretToBBAccounts(final ActiveObjects activeObjects)
    {
        final String key = (String) settings.get("dvcs.connector.bitbucket.clientId");
        final String secret = (String) settings.get("dvcs.connector.bitbucket.secret");

        log.info("Bitbucket key / secret = " + key + " / " + secret);

        if (StringUtils.isBlank(key) || StringUtils.isBlank(secret))
        {
            log.warn("Bitbucket key/secret cannot be blank, nothing to upgrade");
            return;
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
                    } else if (StringUtils.isNotBlank(accessToken) && StringUtils.isNotBlank(oauthKey) && StringUtils.isNotBlank(oauthSecret))
                    {
                        organizationMapping.setAccessToken(null);
                        organizationMapping.save();
                    }
                }
                return null;
            }
        });
    }

    @Override
    public ModelVersion getModelVersion()
    {
        return ModelVersion.valueOf("11");
    }

}
