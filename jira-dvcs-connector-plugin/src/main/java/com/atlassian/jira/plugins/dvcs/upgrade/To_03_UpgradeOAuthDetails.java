package com.atlassian.jira.plugins.dvcs.upgrade;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;

/**
 * FIXME: Test it!!!
 *
 */
public class To_03_UpgradeOAuthDetails implements PluginUpgradeTask
{
    private static final Logger log = LoggerFactory.getLogger(To_03_UpgradeOAuthDetails.class);

    private final PluginSettings settings;

    public To_03_UpgradeOAuthDetails(PluginSettingsFactory pluginSettingsFactory)
    {
        this.settings = pluginSettingsFactory.createGlobalSettings();
    }

    @Override
    public Collection<Message> doUpgrade() throws Exception
    {
        // Bitbucket
        String clientId = getProperty("bitbucketRepositoryClientID");
        String secret = getProperty("bitbucketRepositoryClientSecret");
        String url = "https://bitbucket.org";

        store("bitbucket", clientId, secret, "https://bitbucket.org");

        removeProperty("bitbucketRepositoryClientID");
        removeProperty("bitbucketRepositoryClientSecret");
        
        // Github
        clientId = getProperty("githubRepositoryClientID");
        secret = getProperty("githubRepositoryClientSecret");
        url = "https://github.com";
        
        store("github", clientId, secret, url);
        
        removeProperty("githubRepositoryClientID");
        removeProperty("githubRepositoryClientSecret");

        // Github Enterprise
        clientId = getProperty("ghEnterpriseRepositoryClientID");
        secret = getProperty("ghEnterpriseRepositoryClientSecret");
        url = getProperty("ghEnterpriseRepositoryHostUrl");
 
        store("githube", clientId, secret, url);
        
        removeProperty("ghEnterpriseRepositoryClientID");
        removeProperty("ghEnterpriseRepositoryClientSecret");
        removeProperty("ghEnterpriseRepositoryHostUrl");
        
        return null;
    }

    private void store(String hostId, String clientId, String secret, String url)
    {
        setProperty("dvcs.connector." + hostId + ".clientId", clientId);
        setProperty("dvcs.connector." + hostId + ".secret", secret);
        setProperty("dvcs.connector." + hostId + ".url", url);
    }

    private void setProperty(String name, String value)
    {
        settings.put(name, value);
        log.info("Storing property " + name + " = " + value);
    }
    
    private String getProperty(String name)
    {
        String value = (String) settings.get(name);
        log.info("Reading property " + name + " = " + value);
        return value;
    }
    
    private void removeProperty(String name)
    {
        settings.remove(name);
        log.info("Removing property " + name);
    }
    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    @Override
    public int getBuildNumber()
    {
        return 3;
    }

    @Override
    public String getShortDescription()
    {
        return "Migrate OAuth details to new location";
    }

    @Override
    public String getPluginKey()
    {
        return "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";
    }

}
