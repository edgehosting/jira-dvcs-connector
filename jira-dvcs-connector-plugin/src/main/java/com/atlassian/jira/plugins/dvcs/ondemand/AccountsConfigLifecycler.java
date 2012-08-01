package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.sal.api.lifecycle.LifecycleAware;

public class AccountsConfigLifecycler implements LifecycleAware
{
    private final AccountsConfigService configService;

    public AccountsConfigLifecycler(AccountsConfigService configService)
    {
        super();
        this.configService = configService;
    }

    @Override
    public void onStart()
    {

        configService.reload();

    }

}

