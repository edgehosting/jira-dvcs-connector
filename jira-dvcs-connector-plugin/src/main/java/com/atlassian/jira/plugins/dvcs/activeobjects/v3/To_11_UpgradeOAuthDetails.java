package com.atlassian.jira.plugins.dvcs.activeobjects.v3;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrate OAuth details to new location
 */
public class To_11_UpgradeOAuthDetails
{
    private static final Logger log = LoggerFactory.getLogger(To_11_UpgradeOAuthDetails.class);
    private final PluginSettings settings;

    public To_11_UpgradeOAuthDetails(PluginSettings settings)
    {
        this.settings = settings;
    }

    public void doMigrate()
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

}
