package com.atlassian.jira.plugins.dvcs.ondemand;

import org.springframework.util.Assert;

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
            
            doNewAccount(configuration);
            
        } else {
            
            doUpdateConfiguration(configuration, integratedAccount);
            
        }
        
    }

    private void doNewAccount(AccountsConfig configuration)
    {
        AccountInfo info = toInfo(configuration);
        
        Organization newOrganization = new Organization();
        newOrganization.setName(info.accountName);
        newOrganization.setCredential(new Credential(null, null, null, info.oauthKey, info.oauthSecret));
        newOrganization.setHostUrl(BITBUCKET_URL);
        newOrganization.setDvcsType(BitbucketCommunicator.BITBUCKET);
        newOrganization.setAutolinkNewRepos(true);
        newOrganization.setSmartcommitsOnNewRepos(true);

        organizationService.save(newOrganization);
    }
    
    private void doUpdateConfiguration(AccountsConfig configuration, Organization integratedNotNullAccount)
    {
        AccountInfo info = toInfo(configuration);
        
        // TODO https://sdog.jira.com/wiki/pages/viewpage.action?pageId=47284285
        
    }

    private AccountInfo toInfo(AccountsConfig configuration)
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
            Assert.notNull(info.accountName, "accountName have to be provided");
            Assert.notNull(info.oauthKey, "oauthKey have to be provided");
            Assert.notNull(info.oauthSecret, "oauthSecret have to be provided");
            //
            return info;
            
        } catch (Exception e)
        {
            throw new IllegalStateException("Wrong configuration.", e);
        }
    }

    private boolean supportsIntegratedAccounts()
    {
        return configProvider.supportsIntegratedAccounts();
    }

    private static class AccountInfo {
        String accountName;
        String oauthKey;
        String oauthSecret;
    }
    
}

