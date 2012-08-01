package com.atlassian.jira.plugins.dvcs.ondemand;

import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;

import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.BitbucketAccountInfo;
import com.atlassian.jira.plugins.dvcs.ondemand.AccountsConfig.Links;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;

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
    
    private static final String BITBUCKET_URL = "https://bitbucket.org";
    
    private final AccountsConfigProvider configProvider;
    private final OrganizationService organizationService;

    public BitbucketAccountsConfigService(AccountsConfigProvider configProvider,
                                        OrganizationService organizationService)
    {
        super();
        this.configProvider = configProvider;
        this.organizationService = organizationService;
    }
    
    @Override
    public void reload()
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
                Log.debug("No integrated account found and no configration is provided.");
            }
            
        } else {
            if (configuration != null) { // integrated account found
                doUpdateConfiguration(configuration, integratedAccount);
            } else {
                Log.debug("Integrated account has been found and no configration is provided. Deleting integrated account.");
                removeAccount(integratedAccount);
            }
            
        }
        
    }

    private void doNewAccount(AccountsConfig configuration)
    {
        AccountInfo info = toInfoNewAccount(configuration);
        Organization userAddedAccount = organizationService.getByHostAndName(BITBUCKET_URL, info.accountName);
        
        Organization newOrganization = null;

        if (userAddedAccount == null) {
            // create brand new
            newOrganization = createNewOrganization(info);
      
        } else {
            // make integrated account from user-added account
            newOrganization = copyValues(info, userAddedAccount);
        }

        organizationService.save(newOrganization);
    }

    /**
     * BL comes from https://sdog.jira.com/wiki/pages/viewpage.action?pageId=47284285 
     */
    private void doUpdateConfiguration(AccountsConfig configuration, Organization integratedNotNullAccount)
    {
        AccountInfo info = toInfoExistingAccount(configuration);
        
        if (info != null) {

            // modify :?
            Organization userAddedAccount = organizationService.getByHostAndName(BITBUCKET_URL, info.accountName);
            
            if (userAddedAccount == null) {
                
                if (configHasChanged(integratedNotNullAccount, info)) {
                    
                    copyValues(info, integratedNotNullAccount);
                    organizationService.save(integratedNotNullAccount);
                    
                } else if (accountNameHasChanged(integratedNotNullAccount, info)) {
                    
                    removeAccount(userAddedAccount);
                    organizationService.save(createNewOrganization(info));
                    
                } else {
                    // nothing has changed
                    Log.debug("No changes detect on integrated account");
                }
                
            } else {
                // should not happened
                // existing integrated account with the same name as user added
                Log.warn("Detected existing integrated account with the same name as user added.");
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
        return StringUtils.equals(info.accountName, integratedNotNullAccount.getName());
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
            Log.debug("Bitbucket links not present. " + e + ": " + e.getMessage());
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
           
                Log.debug("Bitbucket accounts info not present. " + e + ": " + e.getMessage());
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
            Log.debug("accountName is empty assuming deletion");
            //
            return null;
        }
        if (isBlank(info.oauthKey)) {
            Log.debug("oauthKey is empty assuming deletion");
            //
            return null;
        }
        if (isBlank(info.oauthSecret)) {
            Log.debug("oauthSecret is empty assuming deletion");
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

