package com.atlassian.jira.plugins.dvcs.ondemand;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.BitbucketAccountInfo;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.Links;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.web.descriptors.WebFragmentModuleDescriptor;
import com.atlassian.sal.api.scheduling.PluginScheduler;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * TODO implement sec. checks so int. account can not be i.e. deleted
 * 
 * BitbucketAccountsConfigService
 *
 * 
 * <br /><br />
 * Created on 1.8.2012, 13:41:20
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class BitbucketAccountsConfigService implements AccountsConfigService//TODO move to BB module
{

    private static final Logger log = LoggerFactory.getLogger(BitbucketAccountsConfigService.class);
    
    private static final String BITBUCKET_URL = "https://bitbucket.org";
    private static final String APP_SWITCHER_LINK_MODULE_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:app-switcher-nav-link";
    private static final String DEFAULT_INVITATION_GROUP = "developers";
        
    private final AccountsConfigProvider configProvider;
    private final OrganizationService organizationService;
    private final PluginScheduler pluginScheduler;
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;
    private final ExecutorService executorService;

    private volatile boolean firstAsyncReload = true;
    
    public BitbucketAccountsConfigService(AccountsConfigProvider configProvider,
                                          OrganizationService organizationService, PluginScheduler pluginScheduler,
                                          PluginController pluginController, PluginAccessor pluginAccessor)
    {
        this.configProvider = configProvider;
        this.organizationService = organizationService;
        this.pluginScheduler = pluginScheduler;
        this.pluginController = pluginController;
        this.pluginAccessor = pluginAccessor;
        this.executorService = Executors.newFixedThreadPool(1, ThreadFactories.namedThreadFactory("BitbucketAccountsConfigService"));
    }
    
    @Override
    public void reload(boolean runAsync)
    {
        //
        // supported only at ondemand instances
        //
        if (!supportsIntegratedAccounts())
        {
            return;
        }

        if (runAsync)
        {
            if (firstAsyncReload)
            {
                // we use the scheduler because AO is not available in LifecycleAware.onStart()
                Map<String, Object> data = Maps.newHashMap();
                data.put("bitbucketAccountsConfigService", this);
                data.put("pluginScheduler", pluginScheduler);
                pluginScheduler.scheduleJob(BitbucketAccountsReloadJob.JOB_NAME, BitbucketAccountsReloadJob.class,
                        data, new Date(), TimeUnit.HOURS.toMillis(1));
                firstAsyncReload = false;
            }
            else
            {
                executorService.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        reloadInternal();
                    }
                });
            }
        } else
        {
            reloadInternal();
        }
    }

    private void reloadInternal()
    {
        //
        AccountsConfig configuration = configProvider.provideConfiguration();
        Organization existingAccount = organizationService.findIntegratedAccount();
        //
        // new or update
        //

        if (existingAccount == null)
        {
            if (configuration != null)
            {
                doNewAccount(configuration);
            } else
            {
                // probably not ondemand instance
                log.debug("No integrated account found and no configration is provided.");
            }
        } else
        { // integrated account found
            if (configuration != null)
            {
                doUpdateConfiguration(configuration, existingAccount);
            } else
            {
                log.info("Integrated account has been found and no configuration is provided. Deleting integrated account.");
                removeAccount(existingAccount);
            }
        }
    }

    private void doNewAccount(AccountsConfig configuration)
    {
        AccountInfo info = toInfoNewAccount(configuration);
        enableAppSwitcherLink(info.accountName);

        Organization userAddedAccount = getUserAddedAccount(info);
        
        Organization newOrganization = null;

        if (userAddedAccount == null)
        {
            // create brand new
            log.info("Creating new integrated account.");
            newOrganization = createNewOrganization(info);
            organizationService.save(newOrganization);

        } else
        {
            log.info("Found the same user-added account.");
            markAsIntegratedAccount(userAddedAccount, info);
        }
    }

    private void enableAppSwitcherLink(String accountName)
    {
        log.info("Enabling app switcher plugin module");
        pluginController.enablePluginModule(APP_SWITCHER_LINK_MODULE_KEY);
        ModuleDescriptor descriptor = pluginAccessor.getEnabledPluginModule(APP_SWITCHER_LINK_MODULE_KEY);
        // if the descriptor isn't the right type, it's probably because we are on an older version of JIRA that
        // doesn't have the navigation-link plugin module type
        if (descriptor instanceof WebFragmentModuleDescriptor)
        {
            WebFragmentModuleDescriptor webFragmentModuleDescriptor = (WebFragmentModuleDescriptor) descriptor;

            Document document = DocumentHelper.createDocument();
            Element element = document.addElement("navigation-link");
            element.addAttribute("key", "app-switcher-nav-link");
            element.addAttribute("menu-key", "home");
            Element link = element.addElement("link");
            link.addText(BITBUCKET_URL + "/" + accountName);
            Element label = element.addElement("label");
            label.addAttribute("key", "Bitbucket - " + accountName);
            Element description = element.addElement("description");
            description.addAttribute("key", "Git and Mercurial code hosting");
            webFragmentModuleDescriptor.init(descriptor.getPlugin(), element);
        }
    }

    private void disableAppSwitcherLink()
    {
        log.info("Disabling app switcher plugin module");
        pluginController.disablePluginModule(APP_SWITCHER_LINK_MODULE_KEY);
    }

    private void markAsIntegratedAccount(Organization userAddedAccount, AccountInfo info)
    {
        organizationService.updateCredentialsKeySecret(userAddedAccount.getId(), info.oauthKey, info.oauthSecret);
    }

    private Organization getUserAddedAccount(AccountInfo info)
    {
        Organization userAddedAccount = organizationService.getByHostAndName(BITBUCKET_URL, info.accountName);
        if (userAddedAccount != null && (StringUtils.isBlank(userAddedAccount.getCredential().getOauthKey())
                                     || StringUtils.isBlank(userAddedAccount.getCredential().getOauthSecret())) ) 
        {
            return userAddedAccount;
        } else 
        {
            return null;
        }
    }

    /**
     * BL comes from https://sdog.jira.com/wiki/pages/viewpage.action?pageId=47284285 
     */
    private void doUpdateConfiguration(AccountsConfig configuration, Organization existingNotNullAccount)
    {
        AccountInfo providedConfig = toInfoExistingAccount(configuration);
        
        if (providedConfig != null)
        {
            // modify :?
            Organization userAddedAccount = getUserAddedAccount(providedConfig);
            
            // we have no user-added account with the same name
            if (userAddedAccount == null)
            {
                if (configHasChanged(existingNotNullAccount, providedConfig))
                {
                    log.info("Detected credentials change.");
                    organizationService.updateCredentialsKeySecret(existingNotNullAccount.getId(), providedConfig.oauthKey, providedConfig.oauthSecret);
                } else if (accountNameHasChanged(existingNotNullAccount, providedConfig))
                {
                    log.info("Detected integrated account name change.");
                    removeAccount(existingNotNullAccount);
                    organizationService.save(createNewOrganization(providedConfig));
                } else
                {
                    // nothing has changed
                    log.info("No changes detect on integrated account");
                }
                enableAppSwitcherLink(providedConfig.accountName);
            } else
            {
                // should not happen
                // existing integrated account with the same name as user added
                log.warn("Detected existing integrated account with the same name as user added. Removing both.");
                removeAccount(userAddedAccount);
                // as provided config is null, remove also integrated account
                removeAccount(existingNotNullAccount);
            }
        } else
        {
            //
            // delete account
            //
            removeAccount(existingNotNullAccount);
        }
        
    }

    private boolean configHasChanged(Organization existingNotNullAccount, AccountInfo info)
    {
        return StringUtils.equals(info.accountName, existingNotNullAccount.getName())
                && (    !StringUtils.equals(info.oauthKey, existingNotNullAccount.getCredential().getOauthKey())
                    ||  !StringUtils.equals(info.oauthSecret, existingNotNullAccount.getCredential().getOauthSecret()));
    }
    
    private boolean accountNameHasChanged(Organization existingNotNullAccount, AccountInfo providedConfig)
    {
        return !StringUtils.equals(providedConfig.accountName, existingNotNullAccount.getName());
    }

    private void removeAccount(Organization organizationAccount)
    {
        disableAppSwitcherLink();
        organizationService.remove(organizationAccount.getId());
    }

    private AccountInfo toInfoNewAccount(AccountsConfig configuration)
    {
        try
        {
            AccountInfo info = new AccountInfo();
            //
            // crawl to information
            //
            Links links = configuration.getSysadminApplicationLinks().get(0);
            BitbucketAccountInfo bitbucketAccountInfo = links.getBitbucket().get(0);
            //
            info.accountName = bitbucketAccountInfo.getAccount();
            info.oauthKey = bitbucketAccountInfo.getKey();
            info.oauthSecret = bitbucketAccountInfo.getSecret();
            //
            assertNotBlank(info.accountName, "accountName have to be provided for new account");
            assertNotBlank(info.oauthKey, "oauthKey have to be provided for new account");
            assertNotBlank(info.oauthSecret, "oauthSecret have to be provided for new account");
            //
            return info;
            
        } catch (Exception e)
        {
            throw new IllegalStateException("Wrong configuration.", e);
        }
    }
    
    private AccountInfo toInfoExistingAccount(AccountsConfig configuration)
    {
        //
        // try to find configuration, otherwise assuming it is deletion of the integrated account
        //
        Links links = null;
        try
        {
            links = configuration.getSysadminApplicationLinks().get(0);
        } catch (Exception e)
        {
            log.debug("Bitbucket links not present. " + e + ": " + e.getMessage());
            return null;
        }
        
        BitbucketAccountInfo bitbucketAccountInfo = null;
        
        if (links != null)
        {
            try
            {
                bitbucketAccountInfo = links.getBitbucket().get(0);
            } catch (Exception e)
            {
                log.debug("Bitbucket accounts info not present. " + e + ": " + e.getMessage());
                return null;
            }
        }

        AccountInfo info = new AccountInfo();
        
        if (bitbucketAccountInfo != null)
        {
            info.accountName = bitbucketAccountInfo.getAccount();
            info.oauthKey = bitbucketAccountInfo.getKey();
            info.oauthSecret = bitbucketAccountInfo.getSecret();
        }

        if (isBlank(info.accountName))
        {
            log.debug("accountName is empty assuming deletion");
            return null;
        }
        if (isBlank(info.oauthKey))
        {
            log.debug("oauthKey is empty assuming deletion");
            return null;
        }
        if (isBlank(info.oauthSecret))
        {
            log.debug("oauthSecret is empty assuming deletion");
            return null;
        }
        return info;
    }

    private static void assertNotBlank(String string, String msg)
    {
        if (isBlank(string))
        {
            throw new IllegalArgumentException(msg);
        }
    }

    private static boolean isBlank(String string)
    {
        return StringUtils.isBlank(string);
    }
    
    private boolean supportsIntegratedAccounts()
    {
        return configProvider.supportsIntegratedAccounts();
    }
    
    private Organization createNewOrganization(AccountInfo info)
    {
        Organization newOrganization = new Organization();
        copyValues(info, newOrganization);
        newOrganization.setDefaultGroups(Sets.newHashSet(new Group(DEFAULT_INVITATION_GROUP)));
        return newOrganization;
    }

    private Organization copyValues(AccountInfo info, Organization organization)
    {
        organization.setId(0);
        organization.setName(info.accountName);
        organization.setCredential(new Credential(null, null, null, info.oauthKey, info.oauthSecret));
        organization.setHostUrl(BITBUCKET_URL);
        organization.setDvcsType("bitbucket");
        organization.setAutolinkNewRepos(true);
        organization.setSmartcommitsOnNewRepos(true);
        
        return organization;
    }

    private static class AccountInfo
    {
        String accountName;
        String oauthKey;
        String oauthSecret;
    }
}

