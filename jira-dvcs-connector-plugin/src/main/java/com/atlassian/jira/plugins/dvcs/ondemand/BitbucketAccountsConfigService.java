package com.atlassian.jira.plugins.dvcs.ondemand;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.BitbucketAccountInfo;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.Links;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.util.concurrent.ThreadFactories;

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
public class BitbucketAccountsConfigService implements AccountsConfigService
{

    private static final Logger log = LoggerFactory.getLogger(BitbucketAccountsConfigService.class);
    
    private static final String BITBUCKET_URL = "https://bitbucket.org";
    
    private final AccountsConfigProvider configProvider;
    private final OrganizationService organizationService;

    private ExecutorService executorService;
    
    public BitbucketAccountsConfigService(AccountsConfigProvider configProvider,
                                        OrganizationService organizationService)
    {
        super();
        this.configProvider = configProvider;
        this.organizationService = organizationService;
        this.executorService = Executors.newFixedThreadPool(1, ThreadFactories.namedThreadFactory("BitbucketAccountsConfigService"));
    }
    
    @Override
    public void reload()
    {
        
        executorService.submit(new Runnable() {

            @Override
            public void run()
            {
                try
                {
                    runInternal();
                } catch (Exception e)
                {
                    log.error("", e);
                }
            }
            
        });

    }

    private void runInternal()
    {
        //
        // supported only at ondemand instances
        //
        if (!supportsIntegratedAccounts()) {
            return;
        }
        
        //
        AccountsConfig configuration = configProvider.provideConfiguration();
        Organization integratedAccount = organizationService.findIntegratedAccount();
        //
        // new or update
        //

        if (integratedAccount == null) {
            
            if (configuration != null) {
                doNewAccount(configuration);
            } else {
                // probably not ondemand instance
                log.debug("No integrated account found and no configration is provided.");
            }
            
        } else {
            if (configuration != null) { // integrated account found
                doUpdateConfiguration(configuration, integratedAccount);
            } else {
                log.debug("Integrated account has been found and no configration is provided. Deleting integrated account.");
                removeAccount(integratedAccount);
            }
            
        }
        
    }

    private void doNewAccount(AccountsConfig configuration)
    {
        AccountInfo info = toInfoNewAccount(configuration);
        Organization userAddedAccount = getUserAddedAccount(info);
        
        Organization newOrganization = null;

        if (userAddedAccount == null) {

            // create brand new
            log.info("Creating new integrated account.");
            newOrganization = createNewOrganization(info);
      
        } else {
            
            log.info("Found the same user-added account.");
            removeAccount(userAddedAccount);
            // make integrated account from user-added account
            newOrganization = copyValues(info, userAddedAccount);
        }

        organizationService.save(newOrganization);
    }

    private Organization getUserAddedAccount(AccountInfo info)
    {
        Organization userAddedAccount = organizationService.getByHostAndName(BITBUCKET_URL, info.accountName);
        if (userAddedAccount != null && (StringUtils.isBlank(userAddedAccount.getCredential().getOauthKey())
                                     || StringUtils.isBlank(userAddedAccount.getCredential().getOauthSecret())) ) {
            return userAddedAccount;
        } else {
            return null;
        }
    }

    /**
     * BL comes from https://sdog.jira.com/wiki/pages/viewpage.action?pageId=47284285 
     */
    private void doUpdateConfiguration(AccountsConfig configuration, Organization integratedNotNullAccount)
    {
        AccountInfo info = toInfoExistingAccount(configuration);
        
        if (info != null) {

            // modify :?
            Organization userAddedAccount = getUserAddedAccount(info);
            
            if (userAddedAccount == null) {
                
                if (configHasChanged(integratedNotNullAccount, info)) {
                    
                    log.info("Detected credentials change.");
                    organizationService.updateCredentialsKeySecret(integratedNotNullAccount.getId(), info.oauthKey, info.oauthSecret);
                    
                } else if (accountNameHasChanged(integratedNotNullAccount, info)) {
                    
                    log.debug("Detected integrated account name change.");
                    removeAccount(integratedNotNullAccount);
                    organizationService.save(createNewOrganization(info));
                    
                } else {
                    // nothing has changed
                    log.info("No changes detect on integrated account");
                }
                
            } else {
                // should not happened
                // existing integrated account with the same name as user added
                log.warn("Detected existing integrated account with the same name as user added.");
                removeAccount(userAddedAccount);
            }
            
            
        } else {
            //
            // delete account
            //
            removeAccount(integratedNotNullAccount);
        }
        
    }

    private boolean configHasChanged(Organization integratedNotNullAccount, AccountInfo info)
    {
        return StringUtils.equals(info.accountName, integratedNotNullAccount.getName())
                && (    !StringUtils.equals(info.oauthKey, integratedNotNullAccount.getCredential().getOauthKey())
                    ||  !StringUtils.equals(info.oauthSecret, integratedNotNullAccount.getCredential().getOauthSecret()));
    }
    
    private boolean accountNameHasChanged(Organization integratedNotNullAccount, AccountInfo info)
    {
        return !StringUtils.equals(info.accountName, integratedNotNullAccount.getName());
    }

    private void removeAccount(Organization integratedNotNullAccount)
    {
        organizationService.remove(integratedNotNullAccount.getId());
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
            //
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
            //
            return null;
            //
        }
        
        BitbucketAccountInfo bitbucketAccountInfo = null;
        
        if (links != null) {
            try
            {
          
                bitbucketAccountInfo = links.getBitbucket().get(0);
           
            } catch (Exception e)
            {
           
                log.debug("Bitbucket accounts info not present. " + e + ": " + e.getMessage());
                //
                return null;
                //
            }
        }
        
        AccountInfo info = new AccountInfo();
        
        if (bitbucketAccountInfo != null) {
            
            info.accountName = bitbucketAccountInfo.getAccount();
            info.oauthKey = bitbucketAccountInfo.getKey();
            info.oauthSecret = bitbucketAccountInfo.getSecret();

        }
        //
        if (isBlank(info.accountName)) {
            log.debug("accountName is empty assuming deletion");
            //
            return null;
        }
        if (isBlank(info.oauthKey)) {
            log.debug("oauthKey is empty assuming deletion");
            //
            return null;
        }
        if (isBlank(info.oauthSecret)) {
            log.debug("oauthSecret is empty assuming deletion");
            //
            return null;
        }
        //
        //
        return info;

    }

    private static void assertNotBlank(String string, String msg)
    {
        if(isBlank(string)) {
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
        return newOrganization;
    }

    private Organization copyValues(AccountInfo info, Organization organization)
    {
        organization.setId(0);
        organization.setName(info.accountName);
        organization.setCredential(new Credential(null, null, null, info.oauthKey, info.oauthSecret));
        organization.setHostUrl(BITBUCKET_URL);
        organization.setDvcsType(BitbucketCommunicator.BITBUCKET);
        organization.setAutolinkNewRepos(true);
        organization.setSmartcommitsOnNewRepos(true);
        
        return organization;
    }

    private static class AccountInfo {
        String accountName;
        String oauthKey;
        String oauthSecret;
    }
    
}

