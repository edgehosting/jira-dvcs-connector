package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;

public class DefaultAccountsConfigService implements AccountsConfigService
{
    private final AccountsConfigProvider configProvider;
    private final OrganizationService organizationService;

    public DefaultAccountsConfigService(AccountsConfigProvider configProvider,
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
            
            doUpdateConfiguratoin(configuration, integratedAccount);
            
        }
        
    }

    private void doNewAccount(AccountsConfig configuration)
    {
        // TODO Auto-generated method stub
        
    }
    
    private void doUpdateConfiguratoin(AccountsConfig configuration, Organization integratedAccount)
    {
        // TODO Auto-generated method stub
        
    }

    private boolean supportsIntegratedAccounts()
    {
        return configProvider.supportsIntegratedAccounts();
    }

    
}

